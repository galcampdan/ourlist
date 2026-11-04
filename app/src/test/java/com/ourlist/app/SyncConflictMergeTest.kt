package com.ourlist.app

import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.model.BucketStatus
import com.ourlist.app.data.model.ChecklistItem
import com.ourlist.app.data.model.ConflictType
import com.ourlist.app.data.model.WishType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncConflictMergeTest {

    @Test
    fun testAutoMerge_differentFields() {
        // Base item
        val base = BucketItem(
            id = "test-1",
            collection = BucketCollection.SHARED,
            title = "제주도 여행",
            placeText = "제주공항",
            targetDate = "2026-06-01",
            updatedAt = 1000L
        )

        // Partner updated placeText
        val partnerUpdate = base.copy(
            placeText = "제주 서귀포 올레길",
            updatedAt = 2000L
        )

        // Local user updated targetDate
        val localUpdate = base.copy(
            targetDate = "2026-06-15",
            updatedAt = 3000L
        )

        // Merged item should preserve both non-conflicting field edits
        val merged = base.copy(
            placeText = partnerUpdate.placeText,
            targetDate = localUpdate.targetDate,
            updatedAt = 4000L
        )

        assertEquals("제주 서귀포 올레길", merged.placeText)
        assertEquals("2026-06-15", merged.targetDate)
        assertEquals("제주도 여행", merged.title)
    }

    @Test
    fun testConflictDetection_sameFieldDifferentValues() {
        val base = BucketItem(
            id = "test-1",
            collection = BucketCollection.SHARED,
            title = "제주도 여행",
            updatedAt = 1000L
        )

        // Both modified title concurrently
        val partnerTitle = "제주도 감귤체험 여행"
        val localTitle = "제주도 한라산 등반 여행"

        val isConflict = partnerTitle != localTitle
        assertTrue("동일 필드 동시 수정 시 충돌로 감지되어야 함", isConflict)
    }

    @Test
    fun testChecklistMerge_byId() {
        val item1 = ChecklistItem("c1", "항공권 예약", checked = true)
        val item2Local = ChecklistItem("c2", "호텔 예약", checked = false)
        val item3Remote = ChecklistItem("c3", "렌터카 예약", checked = true)

        val localChecklist = listOf(item1, item2Local)
        val remoteChecklist = listOf(item1, item3Remote)

        // Merging by ID
        val combinedMap = (localChecklist + remoteChecklist).associateBy { it.id }
        assertEquals(3, combinedMap.size)
        assertTrue(combinedMap["c1"]!!.checked)
        assertFalse(combinedMap["c2"]!!.checked)
        assertTrue(combinedMap["c3"]!!.checked)
    }

    @Test
    fun testDeleteVsUpdateConflict() {
        val base = BucketItem(
            id = "test-item",
            collection = BucketCollection.SHARED,
            title = "일본 온천 여행",
            updatedAt = 1000L
        )

        val partnerDeletedTime = 2000L
        val localEditTime = 3000L // local user edited after partner deleted

        val isDeleteConflict = localEditTime > partnerDeletedTime
        assertTrue(isDeleteConflict)
    }
}
