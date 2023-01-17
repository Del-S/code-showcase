package cz.csob.smartbanking.addon.about.presentation.model

import androidx.annotation.StringRes

/**
 * View object holding title and info for about item.
 *
 * @author eMan a.s.
 */
data class AboutLineVo(
    val type: Type,
    val titleRes: Int,
    @StringRes val infoTitleRes: Int = 0,
    @StringRes val infoTextRes: Int = 0
) {

    enum class Type {
        /**
         * Type for GDPR personal data processing information.
         */
        PERSONAL_DATA_PROCESSING,

        /**
         * Type for application terms and conditions.
         */
        TERMS_AND_CONDITIONS,

        /**
         * Type for cookies and terms of use.
         */
        COOKIES,

        /**
         * Type which displays information for users.
         */
        INFO_FOR_USERS,

        /**
         * Type for used onboarding starting the process.
         */
        ONBOARDING,

        /**
         * Type which resets the app to default settings.
         */
        RESTORE_APP,

        /**
         * Type which allows users to send application logs.
         */
        SEND_LOGS
    }
}


