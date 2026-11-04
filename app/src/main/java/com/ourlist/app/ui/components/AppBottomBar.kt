package com.ourlist.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface as MaterialSurface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ourlist.app.ui.navigation.BottomNavTab
import com.ourlist.app.ui.rememberHaptics
import com.ourlist.app.ui.theme.HeartRed
import com.ourlist.app.ui.theme.IconInactive
import com.ourlist.app.ui.theme.Ink
import com.ourlist.app.ui.theme.OurListDimens
import com.ourlist.app.ui.theme.OurListMotion
import com.ourlist.app.ui.theme.Primary
import com.ourlist.app.ui.theme.SurfaceSelected
import com.ourlist.app.ui.theme.SurfaceSunken

/**
 * Floating sunken bottom dock matching PayTrace's signature navigation bar.
 * A sliding pill indicator travels smoothly across the sunken dock on tab switches.
 */
@Composable
fun AppBottomBar(
    navController: NavController,
    unreadNotificationCount: Int,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route
    val tabs = BottomNavTab.items
    val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    val position by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = OurListMotion.snappy(),
        label = "dockPillPosition",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Ink)
            .navigationBarsPadding()
            .padding(start = 14.dp, top = 6.dp, end = 14.dp, bottom = 10.dp),
    ) {
        MaterialSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(36.dp),
            color = SurfaceSunken,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
            shadowElevation = 6.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(OurListDimens.BottomDockHeight)
                    .padding(5.dp),
            ) {
                // Sliding pill indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f / tabs.size)
                        .fillMaxHeight()
                        .graphicsLayer { translationX = position * size.width }
                        .clip(RoundedCornerShape(31.dp))
                        .background(SurfaceSelected),
                )

                // Background inactive layer (handles clicks)
                DockTabsRow(
                    tabs = tabs,
                    tint = IconInactive,
                    selectedIndex = selectedIndex,
                    unreadCount = unreadNotificationCount,
                    onTabSelected = { tab ->
                        if (currentRoute != tab.route) {
                            navController.navigate(tab.route) {
                                popUpTo(BottomNavTab.Shared.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )

                // Active foreground layer (revealed only where the sliding pill is)
                DockTabsRow(
                    tabs = tabs,
                    tint = Primary,
                    selectedIndex = selectedIndex,
                    unreadCount = unreadNotificationCount,
                    onTabSelected = null,
                    modifier = Modifier
                        .clearAndSetSemantics { }
                        .drawWithContent {
                            val slot = size.width / tabs.size
                            val left = position * slot
                            clipRect(left = left, right = left + slot) {
                                this@drawWithContent.drawContent()
                            }
                        },
                )
            }
        }
    }
}

@Composable
private fun DockTabsRow(
    tabs: List<BottomNavTab>,
    tint: Color,
    selectedIndex: Int,
    unreadCount: Int,
    onTabSelected: ((BottomNavTab) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onTabSelected == null) Modifier else Modifier.selectableGroup()),
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            val itemTint = if (tab == BottomNavTab.Heart && isSelected) HeartRed else tint

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (onTabSelected == null) {
                            Modifier
                        } else {
                            Modifier
                                .clip(RoundedCornerShape(31.dp))
                                .selectable(
                                    selected = isSelected,
                                    interactionSource = null,
                                    indication = null,
                                    role = Role.Tab,
                                    onClick = {
                                        haptics.tick()
                                        onTabSelected(tab)
                                    },
                                )
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = null,
                            modifier = Modifier.size(23.dp),
                            tint = itemTint,
                        )
                        Text(
                            text = tab.title,
                            modifier = Modifier.padding(top = 3.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = itemTint,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    // Notification unread badge pill
                    if (tab == BottomNavTab.Notifications && unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 10.dp, y = (-4).dp)
                                .clip(CircleShape)
                                .background(HeartRed)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = if (unreadCount > 99) "99+" else "$unreadCount",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
