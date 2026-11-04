package com.ourlist.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface as MaterialSurface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ourlist.app.data.model.WishType
import com.ourlist.app.data.sync.SyncState
import com.ourlist.app.ui.rememberHaptics
import com.ourlist.app.ui.theme.Divider
import com.ourlist.app.ui.theme.Due
import com.ourlist.app.ui.theme.HeartRed
import com.ourlist.app.ui.theme.Income
import com.ourlist.app.ui.theme.Ink
import com.ourlist.app.ui.theme.OurListDimens
import com.ourlist.app.ui.theme.OurListMotion
import com.ourlist.app.ui.theme.OverviewGradientEnd
import com.ourlist.app.ui.theme.OverviewGradientStart
import com.ourlist.app.ui.theme.Primary
import com.ourlist.app.ui.theme.PrimarySoft
import com.ourlist.app.ui.theme.Review
import com.ourlist.app.ui.theme.Surface
import com.ourlist.app.ui.theme.SurfaceRaised
import com.ourlist.app.ui.theme.SurfaceSelected
import com.ourlist.app.ui.theme.SurfaceSunken
import com.ourlist.app.ui.theme.SwitchKnob
import com.ourlist.app.ui.theme.SwitchTrackOff
import com.ourlist.app.ui.theme.TextPrimary
import com.ourlist.app.ui.theme.TextSecondary
import com.ourlist.app.ui.theme.WishDateColor
import com.ourlist.app.ui.theme.WishMustColor
import com.ourlist.app.ui.theme.WishSomedayColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SEGMENT_HEIGHT = 44.dp
private val TrackWidth = 52.dp
private val TrackHeight = 31.dp
private val KnobSize = 25.dp
private val KnobInset = 3.dp

@Composable
fun OurListCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Surface,
    border: BorderStroke? = BorderStroke(1.dp, Divider),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(OurListDimens.CardCorner)
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    if (onClick == null) {
        Card(modifier = modifier, shape = shape, colors = colors, border = border) { content() }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            border = border,
            onClick = onClick,
        ) { content() }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        when {
            action == null -> Unit
            onAction != null -> TextButton(
                onClick = onAction,
                modifier = Modifier.heightIn(min = OurListDimens.MinTouchTarget),
            ) {
                Text(text = action, style = MaterialTheme.typography.labelLarge, color = Primary)
            }
            else -> Text(
                text = action,
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
            )
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    MaterialSurface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun WishTypeBadge(
    wishType: WishType,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    val (color, label) = when (wishType) {
        WishType.MUST_DO -> WishMustColor to "꼭 하고 싶다"
        WishType.SOMEDAY -> WishSomedayColor to "언젠간"
        WishType.SPECIFIC_DATE -> WishDateColor to "구체적 날짜"
    }
    StatusPill(text = label, color = color, modifier = modifier)
}

@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)
    val position by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = OurListMotion.snappy(),
        label = "segment",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SEGMENT_HEIGHT)
            .clip(RoundedCornerShape(15.dp))
            .background(SurfaceSunken)
            .padding(4.dp)
            .selectableGroup(),
    ) {
        if (options.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(1f / options.size)
                    .fillMaxHeight()
                    .graphicsLayer { translationX = position * size.width }
                    .clip(RoundedCornerShape(11.dp))
                    .background(SurfaceSelected),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                val color by animateColorAsState(
                    targetValue = if (isSelected) Primary else TextSecondary,
                    animationSpec = OurListMotion.snappy(),
                    label = "segmentLabel",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(11.dp))
                        .selectable(
                            selected = isSelected,
                            interactionSource = null,
                            indication = null,
                            role = Role.RadioButton,
                            onClick = {
                                if (!isSelected) {
                                    haptics.tick()
                                    onSelect(option)
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(option),
                        style = MaterialTheme.typography.labelLarge,
                        color = color,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
fun <T> MultiChoiceChips(
    options: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    onToggle: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option in selected
            val background by animateColorAsState(
                targetValue = if (isSelected) SurfaceSelected else SurfaceSunken,
                animationSpec = OurListMotion.snappy(),
                label = "chipBackground",
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) Primary else TextSecondary,
                animationSpec = OurListMotion.snappy(),
                label = "chipLabel",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = OurListDimens.MinTouchTarget)
                    .clip(RoundedCornerShape(13.dp))
                    .background(background)
                    .toggleable(
                        value = isSelected,
                        interactionSource = null,
                        indication = null,
                        role = Role.Checkbox,
                        onValueChange = {
                            haptics.tick()
                            onToggle(option)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun InlineAddButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Primary,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp),
        shape = RoundedCornerShape(OurListDimens.CardCorner),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = accentColor,
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = accentColor,
        )
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    OurListCard(modifier = modifier.fillMaxWidth(), containerColor = Surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = TextPrimary,
            )
            if (description != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            if (action != null) {
                Spacer(Modifier.height(16.dp))
                action()
            }
        }
    }
}

@Composable
fun HomeHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accentColor: Color = Primary,
    actions: (@Composable () -> Unit)? = null,
) {
    val dateText = subtitle ?: LocalDate.now().format(
        DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN),
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        }
        if (actions != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                actions()
            }
        }
    }
}

@Composable
fun CoupleOverviewCard(
    completedCount: Int,
    totalCount: Int,
    syncState: SyncState,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(OverviewGradientStart, OverviewGradientEnd),
    title: String = "우리가 함께 이룬 꿈",
    accentColor: Color = Primary,
    onClick: (() -> Unit)? = null,
) {
    val ratio = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val percent = (ratio * 100).toInt()

    val (syncLabel, syncIcon, syncColor) = when (syncState) {
        is SyncState.Syncing -> Triple("동기화 중…", Icons.Default.CloudSync, Due)
        is SyncState.Success -> Triple("동기화 완료", Icons.Default.CloudDone, Income)
        is SyncState.Error -> Triple("동기화 확인 필요", Icons.Default.SyncProblem, Primary)
        else -> Triple("동기화 대기", Icons.Default.CloudDone, TextSecondary)
    }

    OurListCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(colors = gradientColors),
                    RoundedCornerShape(OurListDimens.CardCorner),
                )
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimarySoft,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Ink.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = syncIcon,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = syncColor,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = syncLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = syncColor,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$completedCount",
                        style = MaterialTheme.typography.displaySmall,
                        color = TextPrimary,
                    )
                    Text(
                        text = " / $totalCount 개 달성",
                        modifier = Modifier.padding(bottom = 6.dp, start = 6.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary,
                    )
                }
                Text(
                    text = "$percent%",
                    modifier = Modifier.padding(bottom = 6.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { ratio.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accentColor,
                trackColor = Ink.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
fun OurListSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = Primary,
) {
    val haptics = rememberHaptics()
    val trackColor by animateColorAsState(
        targetValue = if (checked) activeColor else SwitchTrackOff,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "switchTrack",
    )
    val knobOffset by animateDpAsState(
        targetValue = if (checked) TrackWidth - KnobSize - KnobInset else KnobInset,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "switchKnob",
    )

    Box(
        modifier = modifier
            .sizeIn(minWidth = OurListDimens.MinTouchTarget, minHeight = OurListDimens.MinTouchTarget)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = null,
                indication = null,
                onValueChange = {
                    haptics.toggle(it)
                    onCheckedChange(it)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = TrackWidth, height = TrackHeight)
                .alpha(if (enabled) 1f else 0.4f)
                .background(trackColor, CircleShape)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), CircleShape),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset(x = knobOffset)
                    .size(KnobSize)
                    .background(SwitchKnob, CircleShape)
                    .border(BorderStroke(2.dp, trackColor), CircleShape),
            )
        }
    }
}
