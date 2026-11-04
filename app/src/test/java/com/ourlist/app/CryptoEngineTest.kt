package com.ourlist.app

import com.ourlist.app.data.backup.CryptoEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CryptoEngineTest {

    @Test
    fun testEncryptionAndDecryption_success() {
        val originalText = "두 사람의 소중한 비밀 버킷리스트 10가지"
        val password = "MySecureCouplePassword123!"

        val encrypted = CryptoEngine.encrypt(originalText, password)
        assertNotNull(encrypted)
        assertNotEquals(originalText, encrypted)

        val decrypted = CryptoEngine.decrypt(encrypted, password)
        assertEquals(originalText, decrypted)
    }

    @Test(expected = Exception::class)
    fun testDecryption_wrongPassword_fails() {
        val originalText = "비밀 메모 내용"
        val correctPassword = "CorrectPassword123"
        val wrongPassword = "WrongPassword999"

        val encrypted = CryptoEngine.encrypt(originalText, correctPassword)
        CryptoEngine.decrypt(encrypted, wrongPassword)
    }

    @Test
    fun testRecoveryCodeFormat() {
        val code = CryptoEngine.generateRecoveryCode()
        assertNotNull(code)
        // Format: XXXX-XXXX-XXXX-XXXX (19 chars with hyphens)
        assertEquals(19, code.length)
        val parts = code.split("-")
        assertEquals(4, parts.size)
        for (part in parts) {
            assertEquals(4, part.length)
        }
    }

    @Test
    fun testEncryptionWithRecoveryCode() {
        val original = "복구 코드로 암호화된 백업 파일"
        val recoveryCode = CryptoEngine.generateRecoveryCode()

        val encrypted = CryptoEngine.encrypt(original, recoveryCode)
        val decrypted = CryptoEngine.decrypt(encrypted, recoveryCode)
        assertEquals(original, decrypted)
    }
}
