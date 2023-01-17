package cz.csob.smartbanking.addon.about.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cz.csob.smartbanking.addon.about.presentation.BuildConfig
import cz.csob.smartbanking.addon.about.presentation.R
import cz.csob.smartbanking.codebase.domain.addon.usecase.CallActionUseCase
import cz.csob.smartbanking.codebase.domain.features.addon.action.model.AddonAction
import cz.csob.smartbanking.codebase.domain.features.globalsettings.model.GlobalSettingsKey
import cz.csob.smartbanking.codebase.domain.features.globalsettings.usecase.GetGlobalSettingsCurrentValueUseCase
import cz.csob.smartbanking.codebase.presentation.util.launch
import cz.csob.smartbanking.codebase.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

/**
 *
 * @author eMan s.r.o.
 * @see[BaseViewModel]
 */
class AboutViewModel(
    private val callAction: CallActionUseCase,
    private val getGlobalSettingsCurrentValue: GetGlobalSettingsCurrentValueUseCase

) : BaseViewModel() {

    var mepiSdkVersion = BuildConfig.MEPI_SDK_VERSION

    val isOnboardingActive = MutableLiveData<Boolean>()

    fun getVersion(context: Context): String? =
        context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES).versionName

    fun onPersonalDataClick(v: View) {
        startBrowser(
            v.context,
            Uri.parse(
                v.context.getString(R.string.other_about_aplication_link_personal_data_protection)
            )
        )
    }

    fun onBusinessConditionClick(v: View) {
        startBrowser(
            v.context,
            Uri.parse(
                v.context.getString(R.string.other_about_aplication_link_terms_and_conditions)
            )
        )
    }

    fun onCookiesConditionsClick(v: View) {
        startBrowser(
            v.context,
            Uri.parse(v.context.getString(R.string.other_about_aplication_link_terms_of_use))
        )
    }

    fun onOnboardingClick() {
        viewModelScope.launch {
            callAction(
                CallActionUseCase.Params(AddonAction.ShowOnboarding)
            )
        }
    }

    fun startBrowser(context: Context, uri: Uri) {
        val browserIntent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(browserIntent)
    }

    fun checkOnboarding() {
        launch {
            val bidonEnabled = getGlobalSettingsCurrentValue(
                GetGlobalSettingsCurrentValueUseCase.Params(
                    GlobalSettingsKey.SBOB_BIDON_ENABLED.key
                )
            ) ?: "0"

            val sbobEnabled = getGlobalSettingsCurrentValue(
                GetGlobalSettingsCurrentValueUseCase.Params(
                    GlobalSettingsKey.SBOB_ENABLED.key
                )
            ) ?: "0"
            isOnboardingActive.value = !(bidonEnabled == "0" && sbobEnabled == "0")
        }
    }
}

