package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EcoGreenPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrashChips(
    trashTypes: List<String>,
    modifier: Modifier = Modifier
) {
    if (trashTypes.isEmpty()) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trash_chips_container"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = "Тип отходов",
                    tint = EcoGreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Тип обнаруженного мусора",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                trashTypes.forEach { type ->
                    val icon = getIconForTrashType(type)
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = type.replaceFirstChar { it.uppercase() },
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = EcoGreenPrimary
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            borderWidth = 1.dp,
                            enabled = true
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("trash_chip_${type.lowercase().replace(" ", "_")}")
                    )
                }
            }
        }
    }
}

private fun getIconForTrashType(type: String): ImageVector {
    val lower = type.lowercase()
    return when {
        lower.contains("пластик") || lower.contains("полиэтилен") || lower.contains("бутылк") -> Icons.Default.LocalDrink
        lower.contains("стекло") -> Icons.Default.LocalDrink
        lower.contains("бумаг") || lower.contains("картон") -> Icons.Default.Description
        lower.contains("опасн") || lower.contains("батарей") || lower.contains("химия") -> Icons.Default.Warning
        lower.contains("строй") || lower.contains("бетон") || lower.contains("кирпич") -> Icons.Default.Build
        lower.contains("органик") || lower.contains("пищев") -> Icons.Default.Eco
        else -> Icons.Default.Delete
    }
}
