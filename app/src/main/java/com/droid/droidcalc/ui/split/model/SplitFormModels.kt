package com.droid.droidcalc.ui.split.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.droid.droidcalc.R // Ensure these string resources are created

/**
 * Represents the data collected from the Add Friend sheet for a new participant.
 * This data is then used to construct a [com.droid.droidcalc.domain.model.Participant] object.
 */
data class NewParticipantData(
    val name: String,
    val emailOrPhone: String? = null,
    val defaultShareMethod: DefaultShareMethod
    // val avatarOption: AvatarOption? = null // TODO: Define AvatarOption for avatar selection
)

/**
 * Enum for selecting a default share setup when adding a new participant via the AddFriendSheet.
 * This helps in pre-configuring how the new participant might be handled initially,
 * though the main [com.droid.droidcalc.domain.usecase.SplitAlgorithm] will ultimately govern the split.
 */
enum class DefaultShareMethod {
    EQUAL_SHARE, // Participant will be considered for equal sharing by default.
    // SPECIFIC_AMOUNT, // Future: Allow pre-filling a specific initial amount or percentage.
    NO_DEFAULT; // Participant added, share determined purely by main algorithm or subsequent manual edit.

    /**
     * Provides a displayable name for the share method, intended for use in UI elements.
     * Requires string resources to be defined (e.g., R.string.split_add_friend_share_equal).
     */
    val displayName: String
        @Composable
        get() = when (this) {
            EQUAL_SHARE -> stringResource(R.string.split_add_friend_share_equal)
            //SPECIFIC_AMOUNT -> stringResource(R.string.split_add_friend_share_specific)
            NO_DEFAULT -> stringResource(R.string.split_add_friend_share_no_default)
        }
}

// TODO: Define AvatarOption sealed class or enum if avatar selection is implemented in AddFriendSheet.
// Example:
// sealed class AvatarOption {
//     data class Image(val uri: String): AvatarOption()
//     data class Emoji(val char: String): AvatarOption()
//     object NoAvatar: AvatarOption()
// }
