package com.ourlist.app

import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.model.WishType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CreatorAndDatePickerTest {

    @Test
    fun `when item createdBy is myDeviceId, display creator is '나'`() {
        val item = BucketItem(
            id = "item1",
            collection = BucketCollection.SHARED,
            title = "여행 가기",
            createdBy = "my-device-1234",
            createdByName = "나"
        )

        val result = item.getDisplayCreatorName(myDeviceId = "my-device-1234", partnerName = "여보")
        assertEquals("나", result)
    }

    @Test
    fun `when item createdBy is myUserSub, display creator is '나'`() {
        val item = BucketItem(
            id = "item2",
            collection = BucketCollection.SHARED,
            title = "영화 보기",
            createdBy = "google-sub-9999",
            createdByName = "나"
        )

        val result = item.getDisplayCreatorName(
            myDeviceId = "my-device-1234",
            myUserSub = "google-sub-9999",
            partnerName = "여보"
        )
        assertEquals("나", result)
    }

    @Test
    fun `when item is in PERSONAL or SECRET collection, display creator is always '나'`() {
        val personalItem = BucketItem(
            id = "item3",
            collection = BucketCollection.PERSONAL,
            title = "자격증 취득",
            createdBy = "partner-device-5678",
            createdByName = "상대방"
        )
        assertEquals("나", personalItem.getDisplayCreatorName(myDeviceId = "my-device-1234"))

        val secretItem = BucketItem(
            id = "item4",
            collection = BucketCollection.SECRET_LOCAL,
            title = "비밀 계획",
            createdBy = "partner-device-5678",
            createdByName = "상대방"
        )
        assertEquals("나", secretItem.getDisplayCreatorName(myDeviceId = "my-device-1234"))
    }

    @Test
    fun `when partner created item and partnerName is set, displays partnerName and never '나'`() {
        // Partner created item on partner's phone where partner's local username was "나"
        val partnerItem = BucketItem(
            id = "item5",
            collection = BucketCollection.SHARED,
            title = "웨딩홀 투어",
            createdBy = "partner-device-5678",
            createdByName = "나" // partner's phone set this to "나"
        )

        // On my phone, partnerName is "지은이 💕"
        val resultWithCustomPartner = partnerItem.getDisplayCreatorName(
            myDeviceId = "my-device-1234",
            partnerName = "지은이 💕"
        )
        assertEquals("지은이 💕", resultWithCustomPartner)

        // On my phone, if partnerName is default "상대방", and partner's createdByName was "나",
        // it must NEVER return "나" - it must return "상대방"!
        val resultWithDefaultPartner = partnerItem.getDisplayCreatorName(
            myDeviceId = "my-device-1234",
            partnerName = "상대방"
        )
        assertEquals("상대방", resultWithDefaultPartner)
    }

    @Test
    fun `when partner created item with explicit nickname, displays that nickname if partnerName is not overridden`() {
        val partnerItem = BucketItem(
            id = "item6",
            collection = BucketCollection.SHARED,
            title = "반지 맞추기",
            createdBy = "partner-device-5678",
            createdByName = "공주님"
        )

        val result = partnerItem.getDisplayCreatorName(
            myDeviceId = "my-device-1234",
            partnerName = "상대방"
        )
        assertEquals("공주님", result)
    }

    @Test
    fun `date format YYYY-MM-DD parses correctly for date picker and D-Day`() {
        val dateString = "2026-12-31"
        val parsed = LocalDate.parse(dateString)
        assertEquals(2026, parsed.year)
        assertEquals(12, parsed.monthValue)
        assertEquals(31, parsed.dayOfMonth)
    }
}
