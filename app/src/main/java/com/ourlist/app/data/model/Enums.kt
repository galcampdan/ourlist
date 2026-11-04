package com.ourlist.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class WishType(val label: String) {
    MUST_DO("꼭 하고 싶다"),
    SOMEDAY("언젠간 하고 싶다"),
    SPECIFIC_DATE("구체적인 날에 하고 싶다")
}

@Serializable
enum class BucketCollection(val displayName: String) {
    SHARED("일반 공동"),
    HEART("❤️ 하트 공동"),
    PERSONAL("개인"),
    SECRET_LOCAL("개인 비밀")
}

@Serializable
enum class BucketStatus {
    TODO,
    DONE
}

enum class SortOption(val label: String) {
    TARGET_DATE("목표 날짜순"),
    CREATED_AT("등록일순"),
    WISH_TYPE("희망 유형별"),
    COST("예상 비용순"),
    CREATED_BY("등록자별"),
    MANUAL("직접 순서")
}

enum class ViewMode {
    LIST,
    CARD
}
