// NOTE: this code is not mine. It is just for reference before the refactor.

package cz.csob.smartbanking.feature.settings.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import cz.csob.smartbanking.codebase.analytics.Analytics
import cz.csob.smartbanking.codebase.api.ApiConstants
import cz.csob.smartbanking.codebase.data.settings.NotificationsSettingsConst
import cz.csob.smartbanking.codebase.domain.addon.usecase.CallActionUseCase
import cz.csob.smartbanking.codebase.domain.analytics.AnalyticsEvent
import cz.csob.smartbanking.codebase.domain.features.addon.action.model.AddonAction
import cz.csob.smartbanking.codebase.domain.features.adobe.usecase.GetAdobeUseCase
import cz.csob.smartbanking.codebase.domain.features.adobe.usecase.SetAdobeUseCase
import cz.csob.smartbanking.codebase.domain.features.language.model.Language
import cz.csob.smartbanking.codebase.domain.features.language.model.LanguageInfo
import cz.csob.smartbanking.codebase.domain.features.language.usecase.GetLanguageInfoUseCase
import cz.csob.smartbanking.codebase.domain.features.language.usecase.SetLanguageUseCase
import cz.csob.smartbanking.codebase.domain.features.products.usecase.GetProductsOrderedUseCase
import cz.csob.smartbanking.codebase.domain.features.products.usecase.SetProductsOrderUseCase
import cz.csob.smartbanking.codebase.domain.features.stage.model.Stage
import cz.csob.smartbanking.codebase.domain.features.stage.usecase.GetActiveStageUseCase
import cz.csob.smartbanking.codebase.domain.logout.usecase.LogoutUseCase
import cz.csob.smartbanking.codebase.domain.mepi.login.model.UserScope
import cz.csob.smartbanking.codebase.domain.mepi.login.usecase.DeactivateMepiSdkUseCase
import cz.csob.smartbanking.codebase.domain.mepi.login.usecase.GetBiometricLoginPreferenceUseCase
import cz.csob.smartbanking.codebase.domain.mepi.login.usecase.GetMepiStatusUseCase
import cz.csob.smartbanking.codebase.domain.mepi.login.usecase.GetSmartKeyLoginPreferenceUseCase
import cz.csob.smartbanking.codebase.domain.mepi.login.usecase.GetUserScopeUseCase
import cz.csob.smartbanking.codebase.domain.mepi.login.usecase.SetBiometricLoginPreferenceUseCase
import cz.csob.smartbanking.codebase.domain.mepi.login.usecase.SetSkipBiometricActivationFlagUseCase
import cz.csob.smartbanking.codebase.domain.mepi.login.usecase.SetSmartKeyLoginPreferenceUseCase
import cz.csob.smartbanking.codebase.domain.pushapp.usecase.CanRegisterPushAppUseCase
import cz.csob.smartbanking.codebase.domain.pushapp.usecase.RegisterPushAppUseCase
import cz.csob.smartbanking.codebase.domain.pushapp.usecase.SetCanRegisterPushAppUseCase
import cz.csob.smartbanking.codebase.domain.pushapp.usecase.UnregisterPushAppUseCase
import cz.csob.smartbanking.codebase.domain.settingowner.usecase.GetOwnerPreferenceUseCase
import cz.csob.smartbanking.codebase.domain.settingowner.usecase.SetOwnerPreferenceUseCase
import cz.csob.smartbanking.codebase.domain.theme.model.ThemeMode
import cz.csob.smartbanking.codebase.domain.theme.usecase.ChangeThemeModeUseCase
import cz.csob.smartbanking.codebase.domain.theme.usecase.GetActiveThemeModeUseCase
import cz.csob.smartbanking.codebase.infrastructure.language.LocaleManager
import cz.csob.smartbanking.codebase.presentation.util.KLiveData
import cz.csob.smartbanking.codebase.presentation.util.KMutableLiveData
import cz.csob.smartbanking.codebase.presentation.util.launch
import cz.csob.smartbanking.codebase.presentation.view.dialog.GenericDialog
import cz.csob.smartbanking.codebase.presentation.viewmodel.BaseViewModel
import cz.csob.smartbanking.codebase.presentation.widget.reorderRecycler.ReorderableRecyclerView
import cz.csob.smartbanking.feature.settings.domain.model.NotificationMaReq
import cz.csob.smartbanking.feature.settings.domain.usecase.GetNotificationsSettingsUseCase
import cz.csob.smartbanking.feature.settings.domain.usecase.SetNotificationsSettingsUseCase
import cz.csob.smartbanking.feature.settings.presentation.BuildConfig
import cz.csob.smartbanking.feature.settings.presentation.R
import cz.csob.smartbanking.feature.settings.presentation.adapter.ReorderProductsAdapter
import cz.csob.smartbanking.feature.settings.presentation.mapper.ReorderProductMapper
import cz.csob.smartbanking.feature.settings.presentation.state.PreferenceViewState
import cz.eman.kaal.domain.result.Result
import cz.eman.kaal.presentation.lifecycle.SingleLiveEvent
import cz.eman.logger.logDebug
import cz.eman.logger.logVerbose
import kotlinx.coroutines.runBlocking

class PreferenceViewModel(
    private val changeThemeModeUc: ChangeThemeModeUseCase,
    private val getThemeModeUc: GetActiveThemeModeUseCase,
    private val setLanguageUc: SetLanguageUseCase,
    private val getLanguageInfoUc: GetLanguageInfoUseCase,
    private val canRegisterPushAppUseCase: CanRegisterPushAppUseCase,
    private val setCanRegisterPushAppUseCase: SetCanRegisterPushAppUseCase,
    private val registerPushApp: RegisterPushAppUseCase,
    private val unregisterPushApp: UnregisterPushAppUseCase,
    private val setAdobeUc: SetAdobeUseCase,
    private val getAdobeUc: GetAdobeUseCase,
    private val setSmartKeyLoginPreference: SetSmartKeyLoginPreferenceUseCase,
    private val setBiometricLoginPreference: SetBiometricLoginPreferenceUseCase,
    private val getSmartKeyLoginPreference: GetSmartKeyLoginPreferenceUseCase,
    private val getBiometricLoginPreference: GetBiometricLoginPreferenceUseCase,
    private val getMepiStatus: GetMepiStatusUseCase,
    private val setProductsOrder: SetProductsOrderUseCase,
    private val getProductsOrdered: GetProductsOrderedUseCase,
    private val logout: LogoutUseCase,
    private val deactivateMepiSdk: DeactivateMepiSdkUseCase,
    private val reorderProductsMapper: ReorderProductMapper,
    private val setSkipBiometricActivationFlag: SetSkipBiometricActivationFlagUseCase,
    private val callAction: CallActionUseCase,
    private val getUserScope: GetUserScopeUseCase,
    private val getActiveStage: GetActiveStageUseCase,
    private val getNotificationsSettings: GetNotificationsSettingsUseCase,
    private val setNotificationsSettings: SetNotificationsSettingsUseCase,
    private val getOwnerPreference: GetOwnerPreferenceUseCase,
    private val setOwnerPreference: SetOwnerPreferenceUseCase
) : BaseViewModel() {

    private val _viewState = SingleLiveEvent<PreferenceViewState>()
    private val _mamRegisterRunning = KMutableLiveData(false)

    val viewState: LiveData<PreferenceViewState> = _viewState
    val mamRegisterRunning: KLiveData<Boolean> = _mamRegisterRunning

    fun getThemeMode(): ThemeMode = runBlocking { getThemeModeUc() }

    fun setThemeMode(modeName: String) = runBlocking {
        val settingsSkin = AnalyticsEvent.SETTINGS_SKIN
        settingsSkin.action = modeName
        Analytics.trackEvent(settingsSkin)
        changeThemeModeUc(ChangeThemeModeUseCase.Params(ThemeMode.createByName(modeName)))
    }

    fun getLanguage(): LanguageInfo = runBlocking { getLanguageInfoUc() }

    /**
     * Shows dialog informing user about restarting the app due to language change. User can confirm
     * it resulting with app restart or cancel it which does nothing.
     *
     * @param context used to modify language
     * @param newLanguage to be set
     */
    fun showLanguageChangeDialog(context: Context, newLanguage: Any) {
        logVerbose { "showLanguageChangeDialog(newLanguage = $newLanguage)" }
        val config = GenericDialog.Config(
            criticalDialog = true,
            titleRes = R.string.settings_popup_title_language_change,
            messageRes = R.string.settings_popup_text_language_change,
            positiveRes = R.string.other_text_yes,
            positiveCallback = { changeLanguage(context, newLanguage) },
            negativeRes = R.string.other_text_no,
            negativeCallback = { _viewState.value = PreferenceViewState.LanguageKept }
        )
        showDialog(config)
    }

    fun getOwnerAvailablePreference() = runBlocking { getOwnerPreference() }

    fun setOwnerAvailable(enabled: Boolean) = runBlocking {
        setOwnerPreference(
            SetOwnerPreferenceUseCase.Params(enabled)
        )
    }

    fun getNotificationMamPreference() = runBlocking { canRegisterPushAppUseCase() }

    fun changeMamRegistration(register: Boolean) {
        logVerbose { "changeMamRegistration(register = $register)" }
        launch {
            _mamRegisterRunning.value = true
            setCanRegisterPushAppUseCase(register)
            if (register) {
                registerPushApp()
            } else {
                unregisterPushApp()
            }
            _mamRegisterRunning.value = false
        }
    }

    fun getAdobe(): Boolean = runBlocking { getAdobeUc() }

    fun setAdobe(enabled: Boolean) = runBlocking {
        setAdobeUc(enabled)

        val settingsAdobe = AnalyticsEvent.SETTINGS_ADOBE
        if (enabled) {
            Analytics.setEnabled(enabled)
            settingsAdobe.action = "on"
            Analytics.trackEvent(settingsAdobe)
        } else {
            settingsAdobe.action = "off"
            Analytics.trackEvent(settingsAdobe)
            Analytics.setEnabled(enabled)
        }
    }

    fun getBiometricLoginUserPreference() = runBlocking { getBiometricLoginPreference() }

    fun getSmartKeyLoginUserPreference() = runBlocking { getSmartKeyLoginPreference() }

    fun setSmartKeyLoginUserPreference(enabled: Boolean) = runBlocking {
        setSmartKeyLoginPreference(SetSmartKeyLoginPreferenceUseCase.Params(enabled))
    }

    fun biometricLoginAvailable(): Boolean = runBlocking {
        getMepiStatus().getOrNull()?.biometricsAvailableOnDevice ?: false
    }

    fun smartkeyLoginAvailable(): Boolean = runBlocking {
        getMepiStatus().getOrNull()?.caseMobileAvailableOnDevice ?: false
    }

    fun isFullScopeUser(): Boolean = runBlocking {
        getUserScope() == UserScope.FULL
    }

    fun setProductsOrderPrefs(order: List<String>) = launch {
        setProductsOrder(SetProductsOrderUseCase.Params(order))
    }

    fun loadAndOrder(recycler: ReorderableRecyclerView) {
        launch {
            when (val result = getProductsOrdered()) {
                is Result.Success -> recycler.setReorderAdapter(
                    swapAdapter = ReorderProductsAdapter(
                        ArrayList(result.data.map { reorderProductsMapper.mapToVo(it) }),
                        R.layout.layout_product_item,
                        this@PreferenceViewModel
                    ),
                    swipeDirs = 0
                )
                is Result.Error -> Unit // TODO: error
            }
        }
    }

    suspend fun onUserWantsToEnableBiometricLogin() {
        setBiometricLoginPreference(SetBiometricLoginPreferenceUseCase.Params(true))
        setSkipBiometricActivationFlag(SetSkipBiometricActivationFlagUseCase.Params(false))
        deactivateMepiSdk()
        logout()
    }

    suspend fun onUserWantsToDisableBiometricLogin() {
        setBiometricLoginPreference(SetBiometricLoginPreferenceUseCase.Params(false))
        setSkipBiometricActivationFlag(SetSkipBiometricActivationFlagUseCase.Params(true))
        logout()
    }

    fun runAddonByAction(action: AddonAction) {
        launch {
            callAction(CallActionUseCase.Params(action = action))
        }
    }

    /**
     * Checks if the app is in Demo stage. If true it displays the dialog.
     *
     * @return true if app in demo else false
     */
    fun isDemoStage(): Boolean = runBlocking {
        val isDemo = getActiveStage() == Stage.Demo
        if (isDemo) {
            displayErrorDialog(R.string.common_error_popup_demo_disabled)
        }
        isDemo
    }

    private fun changeLanguage(context: Context, language: Any) {
        logDebug { "changeLanguage(language = $language)" }
        setLanguage(language.toString())
        val selectedLanguage = getLanguage().language.value
        LocaleManager.setNewLocale(context, selectedLanguage)

        launch {
            val settingsLanguage = AnalyticsEvent.SETTINGS_LANGUAGE
            settingsLanguage.action = selectedLanguage
            analytics.trackEvent(settingsLanguage)
            setNotificationsLanguage(selectedLanguage)
            logout()
            _viewState.value = PreferenceViewState.LanguageChanged
        }
    }

    private fun setLanguage(language: String) = runBlocking {
        logDebug { "setLanguage(newLanguage = $language)" }
        val newLanguage = if (language != Language.SYSTEM_LANGUAGE) {
            Language.createFromValue(language)
        } else {
            null
        }

        setLanguageUc(SetLanguageUseCase.Params(newLanguage))
    }

    suspend fun setNotificationsLanguage(selectedLanguage: String) {
        if (BuildConfig.FEATURE_BUILD) {
            // errors are ignored
            getNotificationsSettings().getOrNull()?.let {
                if (it.notificationStatusFlag) {
                    setNotificationsSettings(
                        SetNotificationsSettingsUseCase.Params(
                            NotificationMaReq(
                                brandCid = ApiConstants.BRAND_CID,
                                businessFunctionCid =
                                ApiConstants.NOTIFICATION_BUSSINESS_FUNCTION_SET,
                                notificationStatusFlag = true,
                                languageCid = if (selectedLanguage == Language.CZECH.value) {
                                    NotificationsSettingsConst.CID_LAN_CS
                                } else {
                                    NotificationsSettingsConst.CID_LAN_OTHER
                                },
                                profileList = null
                            )
                        )
                    ).getOrNull()
                }
            }
        }
    }
}

