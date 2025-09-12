package com.droid.droidcalc.ui.split.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.droid.droidcalc.R
import com.droid.droidcalc.ui.theme.CalcTheme
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/**
 * Dimensions and styling constants for the [SplitHeaderCard] composable.
 * These values help maintain consistency and ease of modification for the card's appearance.
 */
private object SplitHeaderCardDimens {
    val CardHorizontalPadding = 16.dp
    val CardVerticalPadding = 16.dp
    val ContentPadding = 16.dp
    val IconSize = 48.dp // Standard size for the merchant icon
    val IllustrationIconSize = 24.dp // Size for smaller illustrative icons like windmills
    val SpacingSmall = 4.dp
    val SpacingMedium = 8.dp
    val SpacingLarge = 16.dp
    val IconBackgroundAlpha = 0.1f
}

/**
 * A composable that displays the header information for the split bill screen, redesigned to match the new UI.
 * It shows merchant details, total bill amount, amount left to split, and an optional donation message.
 * This component is stateless and its appearance is determined by the provided parameters.
 *
 * @param merchantName The name of the merchant (e.g., "Burger Gembel").
 * @param transactionDate The date of the transaction (e.g., "22 Jun 2023").
 * @param totalAmount The total bill amount as a [BigDecimal].
 * @param amountLeftToSplit The amount left to be split as a [BigDecimal].
 * @param currencyFormatter A [NumberFormat] instance for formatting monetary values.
 * @param donationMessage An optional message regarding donations (e.g., "Burger Gembel sends 2.99 USD for nature conservation").
 * @param merchantIconPainter Optional [Painter] for the merchant icon. A placeholder is used if null.
 * @param illustrationIcons Optional list of [Painter]s for illustrative icons (e.g., windmills). Currently supports up to two.
 * @param modifier Modifier for this composable, allowing for custom styling and layout adjustments from the caller.
 */
@Composable
fun SplitHeaderCard(
    merchantName: String,
    transactionDate: String,
    totalAmount: BigDecimal,
    amountLeftToSplit: BigDecimal,
    currencyFormatter: NumberFormat,
    donationMessage: String?,
    merchantIconPainter: Painter? = null,
    illustrationIcons: List<Painter>? = null,
    modifier: Modifier = Modifier
) {
    // Helper to format currency consistently, appending " USD" as per UI mockups.
    val formatCurrency = remember(currencyFormatter) {
        { amount: BigDecimal ->
            val formatted = currencyFormatter.format(amount)
            // Ensure "USD" suffix, remove if formatter already adds it or if locale is different.
            if (formatted.contains("USD")) formatted else "$formatted USD"
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth(),
        // Padding is applied outside if this card is part of a larger padded structure (e.g. LazyColumn padding)
        // .padding(horizontal = SplitScreenDimens.CardHorizontalPadding, vertical = SplitScreenDimens.CardVerticalPadding),
        shape = MaterialTheme.shapes.large, // Using Material 3 shapes
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // Semi-transparent surface variant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(SplitHeaderCardDimens.ContentPadding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SplitHeaderCardDimens.SpacingLarge)
        ) {
            // Main row for merchant icon, name/date, and total/left amounts
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Merchant Icon section
                Box(
                    modifier = Modifier
                        .size(SplitHeaderCardDimens.IconSize)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = SplitHeaderCardDimens.IconBackgroundAlpha)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (merchantIconPainter != null) {
                        Image(
                            painter = merchantIconPainter,
                            contentDescription = stringResource(
                                R.string.split_merchant_icon_desc,
                                merchantName
                            ),
                            contentScale = ContentScale.Crop, // Or ContentScale.Fit as appropriate
                            modifier = Modifier.size(SplitHeaderCardDimens.IconSize * 0.7f) // Icon slightly smaller than background
                        )
                    } else {
                        // Placeholder Text Icon if no painter provided
                        Text(
                            text = merchantName.firstOrNull()?.uppercase() ?: "M",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(SplitHeaderCardDimens.SpacingMedium))

                // Merchant Name and Date column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = merchantName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = transactionDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(SplitHeaderCardDimens.SpacingMedium))

                // Total Amount and Amount Left column
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.height(IntrinsicSize.Min) // Ensure consistent height with merchant name/date column
                ) {
                    Text(
                        text = formatCurrency(totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = stringResource(
                            R.string.split_amount_left_display,
                            formatCurrency(amountLeftToSplit)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorResource(R.color.grey_unpaid), // Using theme color for unpaid/neutral status
                        textAlign = TextAlign.End
                    )
                }
            }

            // Optional Donation Message and Illustrations row
            if (!donationMessage.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = donationMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorResource(R.color.green_paid), // Using theme color for positive/donation message
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    illustrationIcons?.take(2)
                        ?.forEach { painter -> // Show up to two illustration icons
                            Spacer(modifier = Modifier.width(SplitHeaderCardDimens.SpacingSmall))
                            Image(
                                painter = painter,
                                contentDescription = stringResource(R.string.split_donation_illustration_desc),
                                modifier = Modifier.size(SplitHeaderCardDimens.IllustrationIconSize),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                    colorResource(R.color.green_paid)
                                ) // Tint to match text
                            )
                        }
                }
            }
        }
    }
}

// --- Previews for SplitHeaderCard ---

@Preview(showBackground = true, name = "SplitHeaderCard - Light Theme")
@Composable
private fun SplitHeaderCardPreviewLight() {
    CalcTheme(darkTheme = false) {
        val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }
        SplitHeaderCard(
            merchantName = "Burger Gembel",
            transactionDate = "22 Jun 2023",
            totalAmount = BigDecimal("40.80"),
            amountLeftToSplit = BigDecimal("0.00"),
            currencyFormatter = currencyFormatter,
            donationMessage = "Burger Gembel sends 2.99 USD for nature conservation",
            merchantIconPainter = painterResource(id = R.drawable.ic_split), // Example icon
            illustrationIcons = listOf(painterResource(id = R.drawable.ic_split)), // Example icon
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "SplitHeaderCard - Dark Theme, No Donation")
@Composable
private fun SplitHeaderCardPreviewDark() {
    CalcTheme(darkTheme = true) {
        val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.UK) }
        SplitHeaderCard(
            merchantName = "The Corner Cafe",
            transactionDate = "15 Nov 2023",
            totalAmount = BigDecimal("25.50"),
            amountLeftToSplit = BigDecimal("5.50"),
            currencyFormatter = currencyFormatter,
            donationMessage = null, // No donation message
            merchantIconPainter = null, // Test placeholder icon
            illustrationIcons = null,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "SplitHeaderCard - Long Texts")
@Composable
private fun SplitHeaderCardPreviewLongTexts() {
    CalcTheme(darkTheme = false) {
        val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.GERMANY) }
        SplitHeaderCard(
            merchantName = "International House of Pancakes, Waffles, and Extremely Long Merchant Names Ltd.",
            transactionDate = "December 31st, New Year's Eve Grand Feast",
            totalAmount = BigDecimal("12345.67"),
            amountLeftToSplit = BigDecimal("987.65"),
            currencyFormatter = currencyFormatter,
            donationMessage = "A significant portion of the proceeds from tonight's extravagant banquet and festivities will be generously donated to the Royal Society for the Promotion of Universal Kindness and the Preservation of Fluffy Kittens.",
            merchantIconPainter = painterResource(id = R.drawable.ic_split),
            illustrationIcons = listOf(
                painterResource(id = R.drawable.ic_split),
                painterResource(id = R.drawable.ic_split)
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
