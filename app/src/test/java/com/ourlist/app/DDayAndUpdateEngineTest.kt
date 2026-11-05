package com.ourlist.app

import com.ourlist.app.data.model.DDayCalculator
import com.ourlist.app.data.update.AppUpdateInfo
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DDayAndUpdateEngineTest {

    @Test
    fun testDDayCalculator_todayIsDay1() {
        val today = LocalDate.now().toString()
        val days = DDayCalculator.calculateDays(today)
        assertEquals("오늘 사귀기 시작하면 1일째여야 함", 1L, days)

        val text = DDayCalculator.formatDDayText(today)
        assertEquals("오늘부터 1일 💕", text)
    }

    @Test
    fun testDDayCalculator_pastDate() {
        val pastDate = LocalDate.now().minusDays(99).toString()
        val days = DDayCalculator.calculateDays(pastDate)
        assertEquals("99일 전 시작했으면 오늘은 100일째여야 함", 100L, days)

        val text = DDayCalculator.formatDDayText(pastDate)
        assertEquals("함께한 지 100일째 💕", text)
    }

    @Test
    fun testDDayCalculator_blankOrInvalidReturnsNull() {
        assertNull("빈 문자열은 null 반환", DDayCalculator.calculateDays(""))
        assertNull("공백 문자열은 null 반환", DDayCalculator.calculateDays("   "))
        assertNull("잘못된 날짜 형식은 null 반환", DDayCalculator.calculateDays("invalid-date"))
        assertNull("잘못된 날짜 텍스트는 null 반환", DDayCalculator.formatDDayText(""))
    }

    @Test
    fun testAppUpdateInfo_jsonSerializationAndComparison() {
        val jsonStr = """
            {
                "latestVersionCode": 7,
                "latestVersionName": "1.0.6",
                "apkUrl": "https://example.com/download/OurList-v1.0.6-release.apk",
                "releaseNotes": "1. 알림 배지 잘림 수정\n2. 사귄 날짜 D-Day 지원\n3. 인앱 업데이트 지원",
                "minSupportedVersion": 1,
                "forceUpdate": false,
                "publishedAt": 1730800000000
            }
        """.trimIndent()

        val json = Json { ignoreUnknownKeys = true }
        val info = json.decodeFromString<AppUpdateInfo>(jsonStr)

        assertEquals(7, info.latestVersionCode)
        assertEquals("1.0.6", info.latestVersionName)
        assertFalse(info.forceUpdate)
        assertNotNull(info.apkUrl)

        val currentVersionCode = 6
        assertTrue("최신 버전 코드가 현재 버전 코드보다 크면 업데이트 대상이어야 함", info.latestVersionCode > currentVersionCode)

        val futureCurrentVersion = 7
        assertFalse("버전 코드가 같으면 업데이트 대상이 아니어야 함", info.latestVersionCode > futureCurrentVersion)
    }
}
