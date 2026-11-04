package com.ourlist.app

import com.ourlist.app.data.backup.ExportManager
import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.model.BucketStatus
import com.ourlist.app.data.model.ChecklistItem
import com.ourlist.app.data.model.WishType
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportManagerTest {

    private val sampleItems = listOf(
        BucketItem(
            id = "item-1",
            collection = BucketCollection.SHARED,
            title = "파리 에펠탑 야경 보기",
            wishType = WishType.MUST_DO,
            targetDate = "2027-05-20",
            estimatedCostKRW = 3500000L,
            placeText = "프랑스 파리",
            checklist = listOf(
                ChecklistItem("c1", "여권 갱신", true),
                ChecklistItem("c2", "항공권 예매", false)
            ),
            memo = "밤에 반짝이는 화이트 에펠 꼭 보기",
            status = BucketStatus.TODO
        ),
        BucketItem(
            id = "item-2",
            collection = BucketCollection.HEART,
            title = "커플링 맞추기",
            wishType = WishType.SPECIFIC_DATE,
            targetDate = "2026-12-25",
            estimatedCostKRW = 800000L,
            placeText = "종로 귀금속거리",
            status = BucketStatus.DONE,
            completedAt = 1766620800000L
        )
    )

    @Test
    fun testExportToMarkdown() {
        val md = ExportManager.exportToMarkdown("테스트 목록", sampleItems)
        assertTrue(md.contains("# Our List 버킷리스트 — 테스트 목록"))
        assertTrue(md.contains("파리 에펠탑 야경 보기"))
        assertTrue(md.contains("3,500,000원"))
        assertTrue(md.contains("[x] 여권 갱신"))
        assertTrue(md.contains("[ ] 항공권 예매"))
        assertTrue(md.contains("커플링 맞추기"))
    }

    @Test
    fun testExportToJson() {
        val json = ExportManager.exportToJson(sampleItems)
        assertTrue(json.contains("\"title\": \"파리 에펠탑 야경 보기\""))
        assertTrue(json.contains("\"title\": \"커플링 맞추기\""))
        assertTrue(json.contains("\"collection\": \"SHARED\""))
    }

    @Test
    fun testExportToCsv() {
        val csv = ExportManager.exportToCsv(sampleItems)
        assertTrue(csv.contains("ID,영역,제목,희망유형,목표날짜,예상비용(KRW),장소,메모,상태,완료일시,체크리스트"))
        assertTrue(csv.contains("파리 에펠탑 야경 보기"))
        assertTrue(csv.contains("일반 공동"))
        assertTrue(csv.contains("3500000"))
    }
}
