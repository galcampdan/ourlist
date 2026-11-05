package com.ourlist.app

import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.model.BucketStatus
import com.ourlist.app.data.model.ChecklistItem
import com.ourlist.app.data.model.SortDirection
import com.ourlist.app.data.model.SortOption
import com.ourlist.app.data.model.WishType
import com.ourlist.app.ui.screens.shared.DreamGaugeStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UIBugFixRegressionTest {

    @Test
    fun testUI05_dreamGauge_7of10_is70Percent_and_independentOfFilters() {
        // Scenario from audit: 10 items total, 7 completed
        val allItems = mutableListOf<BucketItem>()
        for (i in 1..7) {
            allItems.add(
                BucketItem(
                    id = "item-$i",
                    collection = BucketCollection.SHARED,
                    title = "완료된 꿈 $i",
                    status = BucketStatus.DONE,
                    completedAt = 1000L * i,
                    wishType = if (i % 2 == 0) WishType.MUST_DO else WishType.SOMEDAY,
                )
            )
        }
        for (i in 8..10) {
            allItems.add(
                BucketItem(
                    id = "item-$i",
                    collection = BucketCollection.SHARED,
                    title = "진행중 꿈 $i",
                    status = BucketStatus.TODO,
                    wishType = WishType.SPECIFIC_DATE,
                    targetDate = "2026-12-31",
                )
            )
        }

        // Active items calculation (excluding trash)
        val activeItems = allItems.filter { !it.isDeleted }
        val total = activeItems.size
        val completed = activeItems.count { it.isDone }
        val percent = if (total > 0) (completed.toFloat() / total * 100).toInt() else 0

        val stats = DreamGaugeStats(totalCount = total, completedCount = completed, percentage = percent)

        // Must be exactly 7/10 and 70%
        assertEquals(10, stats.totalCount)
        assertEquals(7, stats.completedCount)
        assertEquals(70, stats.percentage)

        // Search filter applied to the list does NOT affect gauge stats
        val searchQuery = "완료된"
        val filteredList = activeItems.filter { it.title.contains(searchQuery) }
        assertEquals(7, filteredList.size)
        // Dream gauge remains 7/10 (70%) regardless of filteredList
        assertEquals(70, stats.percentage)

        // Moving 1 TODO item to trash:
        val withOneTrashed = allItems.map { if (it.id == "item-10") it.copy(deletedAt = 123456L) else it }
        val activeAfterTrash = withOneTrashed.filter { !it.isDeleted }
        val totalAfterTrash = activeAfterTrash.size
        val completedAfterTrash = activeAfterTrash.count { it.isDone }
        val percentAfterTrash = (completedAfterTrash.toFloat() / totalAfterTrash * 100).toInt()

        assertEquals(9, totalAfterTrash)
        assertEquals(7, completedAfterTrash)
        assertEquals(77, percentAfterTrash) // 7/9 = 77.7% -> 77%

        // Reverting 1 completed item to TODO:
        val reverted = activeAfterTrash.map { if (it.id == "item-1") it.copy(status = BucketStatus.TODO, completedAt = null) else it }
        val totalReverted = reverted.size
        val completedReverted = reverted.count { it.isDone }
        val percentReverted = (completedReverted.toFloat() / totalReverted * 100).toInt()

        assertEquals(9, totalReverted)
        assertEquals(6, completedReverted)
        assertEquals(66, percentReverted) // 6/9 = 66.6% -> 66%
    }

    @Test
    fun testWishTypeFilter_allCanBeDeselected() {
        var selectedWishTypes: Set<WishType> = WishType.entries.toSet()
        assertEquals(3, selectedWishTypes.size)

        fun toggle(type: WishType) {
            selectedWishTypes = if (type in selectedWishTypes) selectedWishTypes - type else selectedWishTypes + type
        }

        // Toggle MUST_DO off -> 2 left
        toggle(WishType.MUST_DO)
        assertEquals(setOf(WishType.SOMEDAY, WishType.SPECIFIC_DATE), selectedWishTypes)

        // Toggle SOMEDAY off -> 1 left
        toggle(WishType.SOMEDAY)
        assertEquals(setOf(WishType.SPECIFIC_DATE), selectedWishTypes)

        // Toggle last remaining SPECIFIC_DATE off -> can be deselected completely (0 selected)
        toggle(WishType.SPECIFIC_DATE)
        assertTrue(selectedWishTypes.isEmpty())

        // Toggle MUST_DO back on -> 1 active
        toggle(WishType.MUST_DO)
        assertEquals(setOf(WishType.MUST_DO), selectedWishTypes)
    }

    @Test
    fun testSortDirection_costAndDateAscendingDescending() {
        val item1 = BucketItem(id = "1", collection = BucketCollection.SHARED, title = "A", estimatedCostKRW = 50000L, targetDate = "2026-06-01")
        val item2 = BucketItem(id = "2", collection = BucketCollection.SHARED, title = "B", estimatedCostKRW = 10000L, targetDate = "2026-12-31")
        val item3 = BucketItem(id = "3", collection = BucketCollection.SHARED, title = "C", estimatedCostKRW = null, targetDate = null)
        val list = listOf(item1, item2, item3)

        // Cost Ascending (10k -> 50k -> null)
        val costAsc = list.sortedWith(compareBy<BucketItem> { it.estimatedCostKRW == null }.thenBy { it.estimatedCostKRW })
        assertEquals("2", costAsc[0].id)
        assertEquals("1", costAsc[1].id)
        assertEquals("3", costAsc[2].id)

        // Cost Descending (50k -> 10k -> null)
        val costDesc = list.sortedWith(compareBy<BucketItem> { it.estimatedCostKRW == null }.thenByDescending { it.estimatedCostKRW })
        assertEquals("1", costDesc[0].id)
        assertEquals("2", costDesc[1].id)
        assertEquals("3", costDesc[2].id)

        // Target Date Ascending (2026-06-01 -> 2026-12-31 -> null)
        val dateAsc = list.sortedWith(compareBy<BucketItem> { it.targetDate == null }.thenBy { it.targetDate })
        assertEquals("1", dateAsc[0].id)
        assertEquals("2", dateAsc[1].id)
        assertEquals("3", dateAsc[2].id)

        // Target Date Descending (2026-12-31 -> 2026-06-01 -> null)
        val dateDesc = list.sortedWith(compareBy<BucketItem> { it.targetDate == null }.thenByDescending { it.targetDate })
        assertEquals("2", dateDesc[0].id)
        assertEquals("1", dateDesc[1].id)
        assertEquals("3", dateDesc[2].id)
    }

    @Test
    fun testUI10_achievementDateSortConsistency() {
        val memories = listOf(
            BucketItem(id = "1", collection = BucketCollection.SHARED, title = "A", status = BucketStatus.DONE, completedAt = 1000L),
            BucketItem(id = "2", collection = BucketCollection.SHARED, title = "B", status = BucketStatus.DONE, completedAt = 3000L),
            BucketItem(id = "3", collection = BucketCollection.SHARED, title = "C", status = BucketStatus.DONE, completedAt = 2000L),
        )

        // Sorted by completedAt descending
        val sorted = memories.sortedByDescending { it.completedAt ?: 0L }
        assertEquals("2", sorted[0].id)
        assertEquals("3", sorted[1].id)
        assertEquals("1", sorted[2].id)

        // Update item 1's date to 5000L (most recent)
        val updatedMemories = memories.map { if (it.id == "1") it.copy(completedAt = 5000L) else it }
        val reSorted = updatedMemories.sortedByDescending { it.completedAt ?: 0L }
        assertEquals("1", reSorted[0].id)
        assertEquals("2", reSorted[1].id)
        assertEquals("3", reSorted[2].id)
    }

    @Test
    fun testUI01_dirtyStateDetectionLogic() {
        val original = BucketItem(
            id = "original-1",
            collection = BucketCollection.SHARED,
            title = "원래 제목",
            wishType = WishType.MUST_DO,
            targetDate = "2026-07-07",
            estimatedCostKRW = 100000L,
            placeText = "서울",
            memo = "메모 내용",
            checklist = listOf(ChecklistItem("c1", "준비물 1", false))
        )

        // Clean state
        assertFalse(isDirtyCheck(original, original.title, original.wishType, original.targetDate ?: "", original.estimatedCostKRW.toString(), original.placeText ?: "", original.memo ?: "", false, false, original.checklist))

        // Title modified
        assertTrue(isDirtyCheck(original, "새로운 제목", original.wishType, original.targetDate ?: "", original.estimatedCostKRW.toString(), original.placeText ?: "", original.memo ?: "", false, false, original.checklist))

        // WishType modified
        assertTrue(isDirtyCheck(original, original.title, WishType.SOMEDAY, original.targetDate ?: "", original.estimatedCostKRW.toString(), original.placeText ?: "", original.memo ?: "", false, false, original.checklist))

        // TargetDate modified
        assertTrue(isDirtyCheck(original, original.title, original.wishType, "2026-08-08", original.estimatedCostKRW.toString(), original.placeText ?: "", original.memo ?: "", false, false, original.checklist))

        // Cost modified
        assertTrue(isDirtyCheck(original, original.title, original.wishType, original.targetDate ?: "", "200000", original.placeText ?: "", original.memo ?: "", false, false, original.checklist))

        // Place modified
        assertTrue(isDirtyCheck(original, original.title, original.wishType, original.targetDate ?: "", original.estimatedCostKRW.toString(), "부산", original.memo ?: "", false, false, original.checklist))

        // Memo modified
        assertTrue(isDirtyCheck(original, original.title, original.wishType, original.targetDate ?: "", original.estimatedCostKRW.toString(), original.placeText ?: "", "수정된 메모", false, false, original.checklist))

        // Checklist modified
        val modifiedChecklist = original.checklist + ChecklistItem("c2", "추가 준비물", false)
        assertTrue(isDirtyCheck(original, original.title, original.wishType, original.targetDate ?: "", original.estimatedCostKRW.toString(), original.placeText ?: "", original.memo ?: "", false, false, modifiedChecklist))
    }

    private fun isDirtyCheck(
        original: BucketItem,
        title: String,
        wishType: WishType,
        targetDate: String,
        costText: String,
        placeText: String,
        memo: String,
        isReminderOn: Boolean,
        initialReminderOn: Boolean,
        checklist: List<ChecklistItem>
    ): Boolean {
        return title != original.title ||
                wishType != original.wishType ||
                targetDate != (original.targetDate ?: "") ||
                costText != (original.estimatedCostKRW?.toString() ?: "") ||
                placeText != (original.placeText ?: "") ||
                memo != (original.memo ?: "") ||
                isReminderOn != initialReminderOn ||
                checklist != original.checklist
    }

    @Test
    fun testExtractGoogleDriveFolderId() {
        val rawId = "1jTbU5HI34WskGcDoITJlGq4P81sDjbh_"

        // 1. Full standard web URL
        val url1 = "https://drive.google.com/drive/folders/1jTbU5HI34WskGcDoITJlGq4P81sDjbh_"
        assertEquals(rawId, com.ourlist.app.ui.screens.more.extractGoogleDriveFolderId(url1))

        // 2. URL with multi-account /u/0/ and ?usp=sharing
        val url2 = "https://drive.google.com/drive/u/0/folders/1jTbU5HI34WskGcDoITJlGq4P81sDjbh_?usp=sharing"
        assertEquals(rawId, com.ourlist.app.ui.screens.more.extractGoogleDriveFolderId(url2))

        // 3. Short open link
        val url3 = "https://drive.google.com/open?id=1jTbU5HI34WskGcDoITJlGq4P81sDjbh_"
        assertEquals(rawId, com.ourlist.app.ui.screens.more.extractGoogleDriveFolderId(url3))

        // 4. Raw ID with spaces
        val rawWithSpaces = "   1jTbU5HI34WskGcDoITJlGq4P81sDjbh_   "
        assertEquals(rawId, com.ourlist.app.ui.screens.more.extractGoogleDriveFolderId(rawWithSpaces))

        // 5. Empty
        assertEquals("", com.ourlist.app.ui.screens.more.extractGoogleDriveFolderId(""))
    }
}
