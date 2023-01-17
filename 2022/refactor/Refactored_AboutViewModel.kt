package cz.csob.smartbanking.addon.about.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.findNavController
import cz.csob.smartbanking.addon.about.presentation.BR
import cz.csob.smartbanking.addon.about.presentation.BuildConfig
import cz.csob.smartbanking.addon.about.presentation.R
import cz.csob.smartbanking.addon.about.presentation.model.AboutLineVo
import cz.csob.smartbanking.addon.about.presentation.view.AboutFragmentDirections
import cz.csob.smartbanking.codebase.domain.action.CsobAddonAction
import cz.csob.smartbanking.codebase.domain.feature.onboarding.usecase.IsBidonEnabledUseCase
import cz.csob.smartbanking.codebase.domain.features.appstarted.model.AppStartImportantInformation
import cz.csob.smartbanking.codebase.domain.features.appstarted.usecase.GetShowAppStartImportantInfoUseCase
import cz.csob.smartbanking.codebase.domain.features.logs.usecase.ShareLogsUseCase
import cz.csob.smartbanking.codebase.domain.mepi.login.usecase.FetchTokenUseCase
import cz.csob.smartbanking.codebase.domain.redirectwebview.CimPortalUrlDestination
import cz.csob.smartbanking.codebase.presentation.avoidDoubleClick
import cz.csob.smartbanking.codebase.presentation.util.launch
import cz.csob.smartbanking.codebase.presentation.view.bottom.BottomInfoSheet
import cz.csob.smartbanking.codebase.presentation.view.dialog.GenericDialog
import cz.csob.smartbanking.codebase.presentation.viewmodel.BaseViewModel
import cz.csob.smartbanking.engine.domain.addon.action.usecase.CallActionUseCase
import cz.csob.smartbanking.globalsettings.domain.model.GlobalSettingsKey
import cz.csob.smartbanking.globalsettings.domain.usecase.GetGlobalSettingsCurrentValueUseCase
import cz.csob.smartbanking.globalsettings.domain.usecase.GetWebPortalUrlResultUseCase
import cz.eman.kaal.presentation.adapter.binder.ItemBinderImpl
import cz.eman.kaal.presentation.adapter.binder.VariableBinder
import cz.eman.kaal.presentation.adapter.binder.VariableBinderImpl
import cz.eman.kaal.presentation.lifecycle.SingleLiveEvent
import cz.eman.logger.logInfo
import cz.eman.logger.logVerbose
import cz.eman.logger.logWarn

/**
 *
 * @author eMan a.s.
 * @see[BaseViewModel]
 */
class AboutViewModel(
    private val callAction: CallActionUseCase,
    private val getGlobalSettingsCurrentValue: GetGlobalSettingsCurrentValueUseCase,
    private val fetchTokenUseCase: FetchTokenUseCase,
    private val getShowAppStartImportantInfoUseCase: GetShowAppStartImportantInfoUseCase,
    private val getWebPortalUrl: GetWebPortalUrlResultUseCase,
    private val isBidonEnabled: IsBidonEnabledUseCase,
    private val shareLogs: ShareLogsUseCase,
) : BaseViewModel() {

    var mepiSdkVersion = BuildConfig.MEPI_SDK_VERSION

    private var isOnboardingActive: Boolean = false
    private var showAppStartImportantInfo: Boolean = false
    private val _itemsAbout = MutableLiveData<List<AboutLineVo>>()
    private val _resetApp = SingleLiveEvent<Boolean>()

    val itemsAbout: LiveData<List<AboutLineVo>> = _itemsAbout
    val itemsAboutBinder = ItemBinderImpl<AboutLineVo>(BR.item, R.layout.item_about)
    val itemsAboutVariableBinders: Array<VariableBinder<AboutLineVo>> =
        arrayOf(VariableBinderImpl(BR.viewModel, this))
    val itemsAboutOnClick: Function2<View, AboutLineVo, Unit> =
        { view, item -> triggerAboutAction(view, item.type) }
    val resetApp: LiveData<Boolean> = _resetApp

    init {
        checkOnboarding()
        checkShowAppStartImportantInfo()
        buildItems()
    }

    fun getVersion(context: Context): String? =
        context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES).versionName

    /**
     * Displays InfoBottomSheet with more information for the user. Each [AboutLineVo] item can have
     * it's own info text which is displayed.
     *
     * @param view used to find nav controller
     * @param item used to load info text
     */
    fun showBottomInfo(view: View, item: AboutLineVo) {
        logInfo { "showBottomInfo(itemType = ${item.type})" }
        if (item.infoTextRes == 0) {
            logWarn { "Item info text is empty -> not showing bottom info." }
            return
        }

        view.findNavController().navigate(
            AboutFragmentDirections.actionToBottomInfo(
                BottomInfoSheet.Config(
                    title = item.infoTitleRes,
                    body = item.infoTextRes
                )
            )
        )
    }

    private fun checkOnboarding() {
        logVerbose { "checkOnboarding()" }
        launch {
            val bidonEnabled = isBidonEnabled()
            val sbobEnabled = getGlobalSettingsCurrentValue(
                GetGlobalSettingsCurrentValueUseCase.Params(
                    GlobalSettingsKey.SBOB_ENABLED.key
                )
            ) ?: "0" == "0"
            val userIsNotLoggedIn = fetchTokenUseCase() == null
            isOnboardingActive = userIsNotLoggedIn && (bidonEnabled || !sbobEnabled)
        }
    }

    private fun checkShowAppStartImportantInfo() {
        logVerbose { "checkShowAppStartImportantInfo()" }
        launch {
            showAppStartImportantInfo =
                getShowAppStartImportantInfoUseCase() != AppStartImportantInformation.DISABLED &&
                    fetchTokenUseCase() == null
        }
    }

    private fun buildItems() {
        logVerbose { "buildItems()" }
        _itemsAbout.value = listOfNotNull(
            AboutLineVo(
                AboutLineVo.Type.PERSONAL_DATA_PROCESSING,
                R.string.other_about_aplication_button_personal_information
            ),
            AboutLineVo(
                AboutLineVo.Type.TERMS_AND_CONDITIONS,
                R.string.other_about_aplication_button_terms_of_trade
            ),
            AboutLineVo(AboutLineVo.Type.COOKIES, R.string.other_about_aplication_button_cookies),
            AboutLineVo(
                AboutLineVo.Type.INFO_FOR_USERS,
                R.string.other_about_aplication_button_ippid_client_info
            ).takeIf { showAppStartImportantInfo },
            AboutLineVo(
                AboutLineVo.Type.ONBOARDING,
                R.string.other_about_application_text_onboarding_link
            ).takeIf { isOnboardingActive },
            AboutLineVo(
                AboutLineVo.Type.RESTORE_APP,
                R.string.other_button_set_default_app_settings
            ),
            AboutLineVo(
                AboutLineVo.Type.SEND_LOGS,
                R.string.other_about_aplication_button_send_logs,
                R.string.other_about_aplication_button_send_logs,
                R.string.other_about_aplication_button_send_logs_info_popup
            )
        )
    }

    private fun triggerAboutAction(view: View, type: AboutLineVo.Type) {
        logInfo { "triggerAboutAction(type = $type)" }
        view.avoidDoubleClick {
            when (type) {
                AboutLineVo.Type.PERSONAL_DATA_PROCESSING -> onPersonalDataClick(view)
                AboutLineVo.Type.TERMS_AND_CONDITIONS -> onTermsAndConditionsClick(view)
                AboutLineVo.Type.COOKIES -> onCookiesClick(view)
                AboutLineVo.Type.INFO_FOR_USERS -> onInfoForUsersClick(view)
                AboutLineVo.Type.ONBOARDING -> onOnboardingClick()
                AboutLineVo.Type.RESTORE_APP -> onRestoreAppClick()
                AboutLineVo.Type.SEND_LOGS -> onSendLogsClick()
            }
        }
    }

    private fun onPersonalDataClick(view: View) {
        logInfo { "onPersonalDataClick()" }
        startBrowser(
            view.context,
            Uri.parse(
                view.context.getString(
                    R.string.other_about_aplication_link_personal_data_protection
                )
            )
        )
    }

    private fun onTermsAndConditionsClick(view: View) {
        logInfo { "onTermsAndConditionsClick()" }
        startBrowser(
            view.context,
            Uri.parse(
                view.context.getString(
                    R.string.other_about_aplication_link_terms_and_conditions
                )
            )
        )
    }

    private fun onCookiesClick(view: View) {
        logInfo { "onCookiesClick()" }
        startBrowser(
            view.context,
            Uri.parse(view.context.getString(R.string.other_about_aplication_link_terms_of_use))
        )
    }

    private fun onInfoForUsersClick(view: View) {
        logInfo { "onInfoForUsersClick()" }
        view.findNavController().navigate(
            AboutFragmentDirections.actionToImportantInformationFragment()
        )
    }

    private fun onOnboardingClick() {
        logInfo { "onOnboardingClick()" }
        launch {
            val redirectSettingsResult = getWebPortalUrl(
                GetWebPortalUrlResultUseCase.Params(
                    key = GlobalSettingsKey.REDIRECT,
                    property = CimPortalUrlDestination.LINK_URL_CAO_ONBOARDING.urlDestination
                )
            )
            if (redirectSettingsResult.getOrNull()?.enable == true) {
                callAction(
                    CallActionUseCase.Params(
                        CsobAddonAction.RedirectWebView(
                            destinationUrl = CimPortalUrlDestination.LINK_URL_CAO_ONBOARDING
                                .urlDestination
                        )
                    )
                )
            } else {
                callAction(
                    CallActionUseCase.Params(action = CsobAddonAction.ShowOnboarding)
                )
            }
        }
    }

    private fun onRestoreAppClick() {
        logInfo { "onRestoreAppClick()" }
        val config = GenericDialog.Config(
            criticalDialog = true,
            titleRes = R.string.other_popup_set_default_app_settings_header,
            messageRes = R.string.other_popup_set_default_app_settings_text,
            positiveRes = R.string.other_popup_set_default_app_settings_button_restore,
            positiveCallback = { _resetApp.value = true },
            negativeRes = R.string.common_button_cancel
        )
        showDialog(config)
    }

    private fun onSendLogsClick() {
        logInfo { "onSendLogsClick()" }
        launch {
            shareLogs()
        }
    }

    private fun startBrowser(context: Context, uri: Uri) {
        logVerbose { "startBrowser(uri = $uri)" }
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

