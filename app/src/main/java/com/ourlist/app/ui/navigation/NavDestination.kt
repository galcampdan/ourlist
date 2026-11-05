package com.ourlist.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Shared : BottomNavTab(
        route = "shared",
        title = "공동",
        selectedIcon = Icons.Filled.FormatListBulleted,
        unselectedIcon = Icons.Outlined.FormatListBulleted
    )

    object Heart : BottomNavTab(
        route = "heart",
        title = "하트",
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder
    )

    object Memories : BottomNavTab(
        route = "memories",
        title = "추억",
        selectedIcon = Icons.Filled.PhotoAlbum,
        unselectedIcon = Icons.Outlined.PhotoAlbum
    )

    object Notifications : BottomNavTab(
        route = "notifications",
        title = "알림",
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications
    )

    object More : BottomNavTab(
        route = "more",
        title = "더보기",
        selectedIcon = Icons.Filled.MoreHoriz,
        unselectedIcon = Icons.Outlined.MoreHoriz
    )

    companion object {
        val items = listOf(Shared, Heart, Memories, More)
    }
}

object AppRoutes {
    const val SHARED = "shared"
    const val HEART = "heart"
    const val MEMORIES = "memories"
    const val NOTIFICATIONS = "notifications"
    const val MORE = "more"

    const val DETAIL = "detail/{itemId}"
    fun createDetailRoute(itemId: String) = "detail/$itemId"

    const val ADD = "add?collection={collection}"
    fun createAddRoute(collection: String) = "add?collection=$collection"

    const val PERSONAL = "personal"
    const val SECRET_PERSONAL = "secret_personal"
    const val TRASH = "trash"
    const val BACKUP = "backup"
    const val DRIVE_SETUP = "drive_setup"
    const val COUPLE_CONNECT = "couple_connect"
    const val EXPORT = "export"
}
