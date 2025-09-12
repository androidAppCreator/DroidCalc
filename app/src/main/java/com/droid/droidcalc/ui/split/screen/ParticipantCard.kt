package com.droid.droidcalc.ui.split.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droid.droidcalc.R
import com.droid.droidcalc.domain.model.Participant
import com.droid.droidcalc.ui.split.SplitContract
import com.droid.droidcalc.ui.theme.CalcTheme

/**
 * Dimensions and styling constants for the [ParticipantCard].
 * Adheres to Material 3 guidelines and new UI design.
 */
private object ParticipantCardDimens {
    val CardHorizontalPadding = 16.dp
    val CardVerticalPadding = 12.dp
    val AvatarSize = 40.dp
    val SpacingSmall = 4.dp
    val SpacingMedium = 8.dp
    val SpacingLarge = 16.dp
    val Elevation = 2.dp // Subtle elevation
}

/**
 * Displays a card for a single participant in the bill split, reflecting the new UI design.
 * The card shows the participant's avatar (placeholder), name, paid/unpaid status,
 * their monetary share, and their percentage share. The paid status is clickable to toggle.
 * This composable is stateless and driven by the provided parameters, adhering to MVI principles.
 *
 * @param participant The [Participant] data object containing name, paid status, and avatar seed.
 * @param formattedShare A string representing the participant's calculated monetary share (e.g., "8,00 USD").
 * @param percentageShareText A string representing the participant's share as a percentage (e.g., "20%").
 * @param cardColor The background color for this specific participant card.
 * @param onIntent Lambda to dispatch [SplitContract.SplitIntent] to the ViewModel,
 *                 specifically for toggling the paid status.
 * @param modifier Modifier for this composable.
 */
@Composable
fun ParticipantCard(
    participant: Participant,
    formattedShare: String,
    percentageShareText: String,
    cardColor: Color,
    onIntent: (SplitContract.SplitIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large, // Rounded corners as per new UI
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = ParticipantCardDimens.Elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ParticipantCardDimens.CardHorizontalPadding,
                    vertical = ParticipantCardDimens.CardVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Avatar Placeholder and Name/Paid Status Column
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f) // Takes available space, pushing amounts to the end
            ) {
                AvatarPlaceholder(
                    name = participant.name,
                    isYou = participant.isYou,
                    modifier = Modifier.size(ParticipantCardDimens.AvatarSize)
                )
                Spacer(modifier = Modifier.width(ParticipantCardDimens.SpacingLarge))
                Column(
                    verticalArrangement = Arrangement.spacedBy(ParticipantCardDimens.SpacingSmall)
                ) {
                    Text(
                        text = participant.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface // Adapts to cardColor
                    )
                    Text(
                        text = if (participant.isPaid) stringResource(R.string.participant_card_paid_status)
                               else stringResource(R.string.participant_card_unpaid_status),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (participant.isPaid) colorResource(R.color.green_paid) else colorResource(R.color.grey_unpaid), // Dynamic color for paid status
                        modifier = Modifier.clickable {
                            onIntent(
                                SplitContract.SplitIntent.UpdateParticipantField(participant.id) { p ->
                                    p.copy(isPaid = !p.isPaid)
                                }
                            )
                        }
                    )
                }
            }

            // Share Amount and Percentage Column (Aligned to the end)
            Spacer(modifier = Modifier.width(ParticipantCardDimens.SpacingMedium)) // Ensure spacing before amounts
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(ParticipantCardDimens.SpacingSmall)
            ) {
                Text(
                    text = formattedShare,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurface // Adapts to cardColor
                )
                Text(
                    text = percentageShareText,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // Slightly less emphasis
                )
            }
        }
    }
}

/**
 * A placeholder composable for displaying a circular avatar with initials or a default icon.
 *
 * @param name The name of the participant, used to derive initials.
 * @param isYou Flag to potentially style the "You" avatar differently (e.g. different color).
 * @param modifier Modifier for this composable.
 */
@Composable
private fun AvatarPlaceholder(
    name: String,
    isYou: Boolean, // Can be used for different styling if needed
    modifier: Modifier = Modifier
) {
    val initials = name
        .split(' ')
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .take(2)

    val backgroundColor = remember(name, isYou) { // Basic color generation based on name hash
        val hash = name.hashCode()
        // Use isYou for a specific color or a more varied color otherwise
        if (isYou) Color(0xFFE0E0E0) else Color(
            red = (hash and 0xFF0000 shr 16),
            green = (hash and 0x00FF00 shr 8),
            blue = (hash and 0x0000FF)
        ).copy(alpha = 0.3f) // Softer background
    }
    
    val textColor = if (isYou) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)


    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.ifEmpty { "PG" }, // Default if no initials
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
            color = textColor
        )
    }
}

// --- Previews for the new ParticipantCard design ---

@Preview(showBackground = true, name = "ParticipantCard - 'You', Paid")
@Composable
fun ParticipantCardPreviewYouPaid() {
    CalcTheme {
        ParticipantCard(
            participant = Participant(id = "1", name = "You", isYou = true, isPaid = true, avatarSeed = "You"),
            formattedShare = "8,00 USD",
            percentageShareText = "20%",
            cardColor = MaterialTheme.colorScheme.surfaceVariant, // Example color
            onIntent = {},
            modifier = Modifier.padding(ParticipantCardDimens.SpacingMedium)
        )
    }
}

@Preview(showBackground = true, name = "ParticipantCard - Other, Unpaid")
@Composable
fun ParticipantCardPreviewOtherUnpaid() {
    CalcTheme(darkTheme = false) {
        ParticipantCard(
            participant = Participant(id = "2", name = "Samantha W.", isPaid = false, avatarSeed = "Samantha W."),
            formattedShare = "20,00 USD",
            percentageShareText = "50%",
            cardColor = Color(0xFFFCE9E8), // Example reddish tint from UI
            onIntent = {},
            modifier = Modifier.padding(ParticipantCardDimens.SpacingMedium)
        )
    }
}

@Preview(showBackground = true, name = "ParticipantCard - Other, Paid, Dark Theme")
@Composable
fun ParticipantCardPreviewOtherPaidDark() {
    CalcTheme(darkTheme = true) {
        ParticipantCard(
            participant = Participant(id = "3", name = "Jonathan D.", isPaid = true, avatarSeed = "Jonathan D."),
            formattedShare = "12,80 USD",
            percentageShareText = "30%",
            cardColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), // Example color for dark
            onIntent = {},
            modifier = Modifier.padding(ParticipantCardDimens.SpacingMedium)
        )
    }
}

@Preview(showBackground = true, name = "ParticipantCard - Generic PG, Unpaid")
@Composable
fun ParticipantCardPreviewPGUnpaid() {
    CalcTheme {
        ParticipantCard(
            participant = Participant(id = "4", name = "Pandi Gembel", isPaid = false, avatarSeed = "Pandi Gembel"),
            formattedShare = "0,00 USD",
            percentageShareText = "0%",
            cardColor = Color(0xFFE8F0FE), // Example bluish tint from UI
            onIntent = {},
            modifier = Modifier.padding(ParticipantCardDimens.SpacingMedium)
        )
    }
}

/*
String Resources Checklist (ensure these are in strings.xml for ParticipantCard):

<string name="participant_card_paid_status">Paid</string>
<string name="participant_card_unpaid_status">Unpaid</string>

// Colors for paid/unpaid status (example, define in your colors.xml or Theme.kt)
// <color name="green_paid">#4CAF50</color>
// <color name="grey_unpaid">#757575</color>
// Or use MaterialTheme.colorScheme properties if they fit.
// For the preview, I've used GreenPaid and GreyUnpaid which would be defined in the theme.
*/
