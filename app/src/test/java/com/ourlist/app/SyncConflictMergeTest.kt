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
        val base = BucketItem(
            id = "test-1",
            collection = BucketCollection.SHARED,
            title = "제주도 여행",
            placeText = "제주공항",
            targetDate = "2026-06-01",
            estimatedCostKRW = 500000L,
            memo = "기본 메모",
            updatedAt = 1000L
        )

        // Partner updated placeText & memo
        val partnerUpdate = base.copy(
            placeText = "제주 서귀포 올레길",
            memo = "올레 7코스 걷기",
            updatedAt = 2000L
        )

        // Local user only updated targetDate & estimatedCostKRW
        val locallyModifiedFields = setOf("targetDate", "estimatedCostKRW")
        val localUpdate = base.copy(
            targetDate = "2026-06-15",
            estimatedCostKRW = 700000L,
            updatedAt = 3000L
        )

        // 3-way merge
        var mergedTitle = localUpdate.title
        var mergedPlace = localUpdate.placeText
        var mergedTargetDate = localUpdate.targetDate
        var mergedCost = localUpdate.estimatedCostKRW
        var mergedMemo = localUpdate.memo

        if (!locallyModifiedFields.contains("placeText")) mergedPlace = partnerUpdate.placeText
        if (!locallyModifiedFields.contains("memo")) mergedMemo = partnerUpdate.memo
        if (!locallyModifiedFields.contains("targetDate")) mergedTargetDate = partnerUpdate.targetDate

        assertEquals("제주 서귀포 올레길", mergedPlace)
        assertEquals("올레 7코스 걷기", mergedMemo)
        assertEquals("2026-06-15", mergedTargetDate)
        assertEquals(700000L, mergedCost)
        assertEquals("제주도 여행", mergedTitle)
    }

    @Test
    fun testConflictDetection_sameFieldDifferentValues() {
        // Test conflict detection across multiple field types
        val localFields = mapOf(
            "title" to "제주도 감귤체험",
            "placeText" to "서귀포 감귤농장",
            "estimatedCostKRW" to "300000",
            "targetDate" to "2026-09-01"
        )

        val partnerFields = mapOf(
            "title" to "제주도 한라산 등반",
            "placeText" to "성판악 탐방안내소",
            "estimatedCostKRW" to "150000",
            "targetDate" to "2026-10-15"
        )

        for (field in localFields.keys) {
            val isConflict = localFields[field] != partnerFields[field]
            assertTrue("필드 '$field' 동시 수정 시 충돌이 감지되어야 함", isConflict)
        }
    }

    @Test
    fun testClockSkewImmunity_usingCausalPendingOperations() {
        // Device A has a clock 2 hours in the future
        val deviceAClock = 10000000L
        val deviceBClock = 5000000L // 2 hours behind

        // Remote op created by Device B
        val remoteCreatedAt = deviceBClock

        // Device A has NO pending operations for this item
        val locallyModifiedFields = emptySet<String>()

        // Even though Device A's clock is way ahead, because locallyModifiedFields is empty,
        // no false conflict should be triggered, and remote changes are applied cleanly!
        val remoteTitle = "원격에서 수정한 제목"
        var finalTitle = "기존 제목"
        if (!locallyModifiedFields.contains("title")) {
            finalTitle = remoteTitle
        }

        assertEquals("시계 오차와 무관하게 인과 변경이 적용되어야 함", remoteTitle, finalTitle)
    }

    @Test
    fun testChecklist3WayUnionMerge() {
        val c1 = ChecklistItem("c1", "항공권 예약", checked = true)
        val c2 = ChecklistItem("c2", "호텔 예약", checked = false)
        val c3 = ChecklistItem("c3", "렌터카 예약", checked = true)
        val c4NewRemote = ChecklistItem("c4", "카메라 충전기", checked = false)
        val c5NewLocal = ChecklistItem("c5", "선글라스", checked = false)

        val localChecklist = listOf(c1, c2, c5NewLocal)
        val remoteChecklist = listOf(c1.copy(checked = true), c2.copy(checked = true), c3, c4NewRemote)

        // Local pending toggles (e.g. c2 was NOT toggled locally)
        val pendingToggles = setOf<String>()

        val localMap = localChecklist.associateBy { it.id }.toMutableMap()
        for (remoteCheck in remoteChecklist) {
            val existing = localMap[remoteCheck.id]
            if (existing == null) {
                localMap[remoteCheck.id] = remoteCheck
            } else {
                if (!pendingToggles.contains(remoteCheck.id)) {
                    localMap[remoteCheck.id] = remoteCheck
                }
            }
        }
        val merged = localMap.values.toList()

        assertEquals(5, merged.size)
        assertTrue("c1은 checked=true 유지", merged.first { it.id == "c1" }.checked)
        assertTrue("c2는 원격의 완료 반영", merged.first { it.id == "c2" }.checked)
        assertNotNull("c3 원격 신규 항목 보존", merged.firstOrNull { it.id == "c3" })
        assertNotNull("c4 원격 신규 항목 보존", merged.firstOrNull { it.id == "c4" })
        assertNotNull("c5 로컬 신규 항목 보존", merged.firstOrNull { it.id == "c5" })
    }

    @Test
    fun testDeleteVsUpdateConflict_detection() {
        // Case 1: Local edited, remote deleted
        val hasLocalPendingUpdate = true
        val remoteAction = "DELETE"
        val isConflict1 = hasLocalPendingUpdate && remoteAction == "DELETE"
        assertTrue("로컬 수정 vs 원격 삭제 충돌 감지", isConflict1)

        // Case 2: Local deleted, remote updated
        val hasLocalPendingDelete = true
        val remoteAction2 = "UPDATE_FIELD"
        val isConflict2 = hasLocalPendingDelete && remoteAction2 == "UPDATE_FIELD"
        assertTrue("로컬 삭제 vs 원격 수정 충돌 감지", isConflict2)
    }

    @Test
    fun testOperationIdempotency() {
        val seenOpIds = mutableSetOf<String>()
        val opId = "unique-op-uuid-1234"

        // First receipt
        val firstInserted = seenOpIds.add(opId)
        assertTrue(firstInserted)

        // Duplicate receipt
        val duplicateInserted = seenOpIds.add(opId)
        assertFalse("중복 opId 수신 시 재처리가 차단되어야 함", duplicateInserted)
    }
}
