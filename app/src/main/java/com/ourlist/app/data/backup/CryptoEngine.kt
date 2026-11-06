package com.ourlist.app.data.backup

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATION_COUNT = 65536
    private const val KEY_LENGTH = 256
    private const val TAG_LENGTH_BIT = 128
    private const val SALT_LENGTH_BYTE = 16
    private const val IV_LENGTH_BYTE = 12

    fun encryptBackup(plainText: String, password: String, recoveryCode: String): String {
        require(password.isNotBlank() && recoveryCode.isNotBlank())
        val dataKey = Base64.getEncoder().encodeToString(ByteArray(32).apply { SecureRandom().nextBytes(this) })
        return Json.encodeToString(mapOf(
            "format" to "ourlist-backup-2",
            "payload" to encrypt(plainText, dataKey),
            "passwordKey" to encrypt(dataKey, password),
            "recoveryKey" to encrypt(dataKey, recoveryCode)
        ))
    }

    fun decryptBackup(backup: String, passwordOrCode: String): String {
        val text = backup.trim()
        if (!text.startsWith("{")) return decrypt(text, passwordOrCode)
        val envelope = Json.decodeFromString<Map<String, String>>(text)
        require(envelope["format"] == "ourlist-backup-2") { "지원하지 않는 백업 형식입니다." }
        val key = listOf("passwordKey", "recoveryKey").firstNotNullOfOrNull { name ->
            envelope[name]?.let { runCatching { decrypt(it, passwordOrCode) }.getOrNull() }
        } ?: throw javax.crypto.AEADBadTagException("비밀번호 또는 백업 당시의 복구 코드를 확인해주세요.")
        return decrypt(requireNotNull(envelope["payload"]), key)
    }

    fun generateRecoveryCode(): String {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ" // exclude confusing 0, 1, I, O
        val random = SecureRandom()
        val sb = StringBuilder()
        for (i in 0 until 16) {
            if (i > 0 && i % 4 == 0) sb.append("-")
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }

    fun encrypt(plainText: String, secretKeyPass: String): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTE).apply { random.nextBytes(this) }
        val iv = ByteArray(IV_LENGTH_BYTE).apply { random.nextBytes(this) }

        val keySpec = PBEKeySpec(secretKeyPass.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val keyBytes = factory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BIT, iv))
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Format: version (1 byte) + salt (16 bytes) + iv (12 bytes) + cipherBytes
        val output = ByteArray(1 + salt.size + iv.size + cipherBytes.size)
        output[0] = 1 // version 1
        System.arraycopy(salt, 0, output, 1, salt.size)
        System.arraycopy(iv, 0, output, 1 + salt.size, iv.size)
        System.arraycopy(cipherBytes, 0, output, 1 + salt.size + iv.size, cipherBytes.size)

        return Base64.getEncoder().encodeToString(output)
    }

    fun decrypt(encryptedBase64: String, secretKeyPass: String): String {
        val raw = Base64.getDecoder().decode(encryptedBase64)
        if (raw.size < 1 + SALT_LENGTH_BYTE + IV_LENGTH_BYTE + 16) {
            throw IllegalArgumentException("Invalid encrypted payload length")
        }

        val version = raw[0].toInt()
        if (version != 1) {
            throw IllegalArgumentException("Unsupported backup version: $version")
        }

        val salt = ByteArray(SALT_LENGTH_BYTE)
        val iv = ByteArray(IV_LENGTH_BYTE)
        System.arraycopy(raw, 1, salt, 0, SALT_LENGTH_BYTE)
        System.arraycopy(raw, 1 + SALT_LENGTH_BYTE, iv, 0, IV_LENGTH_BYTE)

        val cipherOffset = 1 + SALT_LENGTH_BYTE + IV_LENGTH_BYTE
        val cipherLength = raw.size - cipherOffset
        val cipherBytes = ByteArray(cipherLength)
        System.arraycopy(raw, cipherOffset, cipherBytes, 0, cipherLength)

        val keySpec = PBEKeySpec(secretKeyPass.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val keyBytes = factory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BIT, iv))
        val plainBytes = cipher.doFinal(cipherBytes)

        return String(plainBytes, Charsets.UTF_8)
    }
}
