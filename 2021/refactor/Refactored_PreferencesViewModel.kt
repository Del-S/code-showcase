package cz.csob.smartbanking.feature.settings.presentation.viewmodel

import android.content.Context
import android.view.View
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.navigation.findNavController
import cz.csob.smartbanking.codebase.analytics.Analytics
import cz.csob.smartbanking.codebase.domain.KATE_SETTINGS_INIT
import cz.csob.smartbanking.codebase.domain.addon.usecase.CallActionUseCase
import cz.csob.smartbanking.codebase.domain.analytics.AnalyticsEvent
import cz.csob.smartbanking.codebase.domain.common.model.BusinessFunction
import cz.csob.smartbanking.codebase.domain.features.addon.action.model.ActionTriggerType
import cz.csob.smartbanking.codebase.domain.features.addon.action.model.AddonAction
import cz.csob.smartbanking.codebase.domain.features.adobe.usecase.GetAdobeUseCase
import cz.csob.smartbanking.codebase.domain.features.adobe.usecase.SetAdobeUseCase
import cz.csob.smartbanking.codebase.domain.features.language.model.Language
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
import cz.csob.smartbanking.codebase.domain.settingowner.usecase.GetOwnerPreferenceUseCase
import cz.csob.smartbanking.codebase.domain.settingowner.usecase.SetOwnerPreferenceUseCase
import cz.csob.smartbanking.codebase.domain.theme.model.ThemeMode
import cz.csob.smartbanking.codebase.domain.theme.usecase.ChangeThemeModeUseCase
import cz.csob.smartbanking.codebase.domain.theme.usecase.GetActiveThemeModeUseCase
import cz.csob.smartbanking.codebase.infrastructure.language.LocaleManager
import cz.csob.smartbanking.codebase.presentation.model.AllowTimer
import cz.csob.smartbanking.codebase.presentation.util.KLiveData
import cz.csob.smartbanking.codebase.presentation.util.KMutableLiveData
import cz.csob.smartbanking.codebase.presentation.util.launch
import cz.csob.smartbanking.codebase.presentation.view.dialog.GenericDialog
import cz.csob.smartbanking.codebase.presentation.viewmodel.BaseViewModel
import cz.csob.smartbanking.codebase.presentation.widget.reorderRecycler.ReorderableRecyclerView
import cz.csob.smartbanking.feature.settings.domain.model.NotificationMaReq
import cz.csob.smartbanking.feature.settings.domain.usecase.GetNotificationsSettingsUseCase
import cz.csob.smartbanking.feature.settings.domain.usecase.SetNotificationsSettingsUseCase
import cz.csob.smartbanking.feature.settings.presentation.BR
import cz.csob.smartbanking.feature.settings.presentation.BuildConfig
import cz.csob.smartbanking.feature.settings.presentation.R
import cz.csob.smartbanking.feature.settings.presentation.adapter.ReorderProductsAdapter
import cz.csob.smartbanking.feature.settings.presentation.mapper.ReorderProductMapper
import cz.csob.smartbanking.feature.settings.presentation.model.SettingsDialogLineType
import cz.csob.smartbanking.feature.settings.presentation.model.SettingsDialogLineVo
import cz.csob.smartbanking.feature.settings.presentation.model.SettingsDialogLineVo.Companion.buildLanguageDialogLines
import cz.csob.smartbanking.feature.settings.presentation.model.SettingsDialogType
import cz.csob.smartbanking.feature.settings.presentation.model.SettingsDialogViewState
import cz.csob.smartbanking.feature.settings.presentation.model.SettingsHeaderVo
import cz.csob.smartbanking.feature.settings.presentation.model.SettingsItemVo
import cz.csob.smartbanking.feature.settings.presentation.model.SettingsLineType
import cz.csob.smartbanking.feature.settings.presentation.model.SettingsLineVo
import cz.csob.smartbanking.feature.settings.presentation.state.PreferenceViewState
import cz.csob.smartbanking.feature.settings.presentation.view.SettingsFragmentDirections
import cz.eman.kaal.domain.result.Result
import cz.eman.kaal.presentation.adapter.binder.CompositeItemBinder
import cz.eman.kaal.presentation.adapter.binder.ConditionalDataBinder
import cz.eman.kaal.presentation.adapter.binder.ItemBinder
import cz.eman.kaal.presentation.adapter.binder.ItemBinderImpl
import cz.eman.kaal.presentation.lifecycle.SingleLiveEvent
import cz.eman.logger.logDebug
import cz.eman.logger.logVerbose
import cz.eman.logger.logWarn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class PreferenceViewModel(
    private val changeThemeModeUc: ChangeThemeModeUseCase,
    private val getThemeModeUc: GetActiveThemeModeUseCase,
    private val setLanguageUc: SetLanguageUseCase,
    private val getLanguageInfoUc: GetLanguageInfoUseCase,
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
    private val _settingItems = KMutableLiveData<List<SettingsLineVo>>(emptyList())
    private val _settingDialogItems = KMutableLiveData<List<SettingsDialogLineVo>>(emptyList())
    private val _settingDialogViewState = KMutableLiveData(SettingsDialogViewState.SHOWN)

    val viewState: LiveData<PreferenceViewState> = _viewState
    val settingItemBinder: ItemBinder<SettingsLineVo> = CompositeItemBinder(
        object : ConditionalDataBinder<SettingsLineVo>(BR.header, R.layout.item_settings_header) {
            override fun canHandle(itemModel: SettingsLineVo) = itemModel is SettingsHeaderVo
        },
        object : ConditionalDataBinder<SettingsLineVo>(BR.item, R.layout.item_settings) {
            override fun canHandle(itemModel: SettingsLineVo) = itemModel is SettingsItemVo
        }
    )
    val settingItems: KLiveData<List<SettingsLineVo>> = _settingItems
    val settingClick: Function2<View, SettingsLineVo, Unit> =
        { view, item -> handleSettingsAction(view, item) }

    val settingDialogItemBinder =
        ItemBinderImpl<SettingsDialogLineVo>(BR.item, R.layout.item_dialog_settings)
    val settingDialogItems: KLiveData<List<SettingsDialogLineVo>> = _settingDialogItems
    val settingDialogClick: Function2<View, SettingsDialogLineVo, Unit> =
        { view, item -> handleSettingsDialogAction(view, item) }

    val settingDialogViewState: KLiveData<SettingsDialogViewState> = _settingDialogViewState

    init {
        loadSettings()
    }

    /**
     * Loads dialog items which are displayed to the user and can be picked from. This option is
     * available for [SettingsDialogType.THEME] and [SettingsDialogType.LANGUAGE].
     *
     * @param type used to decide which items should be loaded
     * @see SettingsDialogLineVo.buildThemeDialogLines
     * @see SettingsDialogLineVo.buildLanguageDialogLines
     */
    fun loadSettingsDialogItems(type: SettingsDialogType) {
        logVerbose { "loadSettingsDialogItems(type = $type)" }
        launch(Dispatchers.Default) {
            _settingDialogItems.postValue(
                when (type) {
                    SettingsDialogType.THEME ->
                        SettingsDialogLineVo.buildThemeDialogLines(getThemeModeUc())
                    SettingsDialogType.LANGUAGE -> buildLanguageDialogLines(
                        getLanguageInfoUc().takeIf { !it.fromSystem }?.language
                    )
                }
            )
        }
    }

    /**
     * Hides setting dialog by navigating back using navigation.
     *
     * @param view used to avoid double click and load nav controller
     */
    fun hideSettingsDialog(view: View) {
        logVerbose { "hideSettingsDialog()" }
        avoidDoubleClick(view) {
            _settingDialogViewState.postValue(SettingsDialogViewState.HIDE)
        }
    }

    /**
     * Loads and orders products to be displayed in [ReorderableRecyclerView]. Allowing user to
     * reorder products.
     *
     * @param recycler which will display products
     */
    fun loadAndOrder(recycler: ReorderableRecyclerView) {
        logVerbose { "loadAndOrder()" }
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

    /**
     * Sets product order into preferences.
     *
     * @param order of the products
     */
    fun setProductsOrderPrefs(order: List<String>) {
        logDebug { "setProductsOrderPrefs(order = $order)" }
        launch {
            setProductsOrder(SetProductsOrderUseCase.Params(order))
        }
    }

    /**
     * Loads application settings. Combines values from multiple functions and builds a single list
     * with [SettingsLineVo] which is then set to [_settingItems]. Settings are split into four
     * sections at the moment:
     * - View (application appearance): [buildSettingItemsView].
     * - Notification: [buildSettingItemsNotification].
     * - Kate: [buildSettingItemsKate].
     * - Security: [buildSettingItemsSecurity].
     *
     * Note: Group scope user does not have Notification and Kate settings.
     */
    private fun loadSettings() {
        launch(Dispatchers.Default) {
            _settingItems.postValue(
                buildSettingItemsView() +
                    if (getUserScope() == UserScope.FULL) {
                        buildSettingItemsNotification() + buildSettingItemsKate()
                    } else {
                        emptyList()
                    } +
                    buildSettingItemsSecurity()
            )
        }
    }

    /**
     * Build setting items for View section. Contains:
     * - Header which describes the section (can have description).
     * - Theme: where user can chose application theme.
     * - Language: where user can chose application language.
     * - Product order: where user can change order of it's products.
     * - Show account owner: allows user to show account owner in dashboard.
     *
     * @return List of [SettingsLineVo]
     */
    private suspend fun buildSettingItemsView() = listOf(
        SettingsHeaderVo(titleRes = R.string.settings_text_view),
        SettingsItemVo(
            type = SettingsLineType.View.THEME,
            titleRes = R.string.settings_switch_dark_mode,
            descriptionRes = getThemeValue()
        ),
        SettingsItemVo(
            type = SettingsLineType.View.LANGUAGE,
            titleRes = R.string.settings_button_language,
            descriptionRes = getLanguageTitle()
        ),
        SettingsItemVo(
            type = SettingsLineType.View.PRODUCT_ORDER,
            titleRes = R.string.settings_button_product_order
        ),
        SettingsItemVo(
            type = SettingsLineType.View.ACCOUNT_OWNER,
            titleRes = R.string.settings_switch_show_account_owner,
            hasSwitch = true,
            hasDivider = true,
            isChecked = getOwnerPreference(),
        )
    )

    /**
     * Gets theme value which is a String resource displayed to the used based on which
     * application theme is currently displayed. Can be:
     * - Dark mode: user has set application to be in dark mode
     * - Light mode: user has set application to be in light mode
     * - System mode: system has decided the application theme (based on system theme)
     *
     * @return Int with String resource
     */
    private suspend fun getThemeValue(): Int = when (getThemeModeUc()) {
        ThemeMode.Dark -> R.string.settings_switch_dark_mode_on
        ThemeMode.Light -> R.string.settings_switch_dark_mode_off
        ThemeMode.System -> R.string.settings_switch_dark_mode_use_system_setting
    }

    /**
     * Gets language value which is a String resource displayed to the user based on which language
     * is selected in the application. Can be:
     * - System: when system chose the language (or when language is unknown - should not happen)
     * - Czech: user has set language to be Czech
     * - English: user has set language to be English
     *
     * @return Int with String resource
     */
    private suspend fun getLanguageTitle(): Int {
        val lang = getLanguageInfoUc()
        return when {
            lang.fromSystem -> R.string.settings_switch_language_system_setting
            lang.language == Language.CZECH -> R.string.settings_switch_language_czech
            lang.language == Language.ENGLISH -> R.string.settings_switch_language_english
            else -> R.string.settings_switch_language_system_setting
        }
    }

    /**
     * Build setting items for Notification section. Contains:
     * - Header which describes the section (can have description).
     * - Notifications: user will be redirected to notification settings.
     *
     * @return List of [SettingsLineVo]
     */
    private fun buildSettingItemsNotification() = listOf(
        SettingsHeaderVo(titleRes = R.string.settings_text_notifications),
        SettingsItemVo(
            type = SettingsLineType.Notification,
            titleRes = R.string.notifications_text_send_notifications,
            hasDivider = true
        )
    )

    /**
     * Build setting items for Kate section. Contains:
     * - Header which describes the section (can have description).
     * - Kate version: allows user to change Kate version.
     *
     * @return List of [SettingsLineVo]
     */
    private fun buildSettingItemsKate() = listOf(
        SettingsHeaderVo(titleRes = R.string.settings_text_kate),
        SettingsItemVo(
            type = SettingsLineType.Kate,
            titleRes = R.string.settings_button_change_kate_version,
            descriptionRes = R.string.settings_change_kate_version_description,
            hasDivider = true
        )
    )

    /**
     * Build setting items for Security section. Contains:
     * - Header which describes the section with antivirus description.
     * - Biometry: allows user to change biometric authorization.
     * - SmartKey: allows user to change smart key authorization.
     * - Adobe: allows user to enable or disable adobe analytics.
     * - Security check: allows user to run a security check of the app.
     *
     * @return List of [SettingsLineVo]
     */
    private suspend fun buildSettingItemsSecurity() = listOfNotNull(
        SettingsHeaderVo(
            titleRes = R.string.settings_text_security,
            descriptionRes = R.string.settings_antivirus_label
        ),
        if (isBiometricAvailable()) {
            SettingsItemVo(
                type = SettingsLineType.Security.BIOMETRY,
                titleRes = R.string.settings_switch_biometry,
                descriptionRes = R.string.settings_text_biometry_info,
                hasSwitch = true,
                isChecked = getBiometricLoginPreference()
            )
        } else {
            null
        },
        if (isSmartKeyLoginAvailable()) {
            SettingsItemVo(
                type = SettingsLineType.Security.SMART_KEY,
                titleRes = R.string.settings_switch_smart_key,
                descriptionRes = R.string.settings_text_smartkey_info,
                hasSwitch = true,
                isChecked = getSmartKeyLoginPreference()
            )
        } else {
            null
        },
        SettingsItemVo(
            type = SettingsLineType.Security.ADOBE,
            titleRes = R.string.settings_switch_adobe_analytics,
            descriptionRes = R.string.settings_text_adobe_analytics,
            hasSwitch = true,
            isChecked = getAdobeUc()
        ),
        SettingsItemVo(
            type = SettingsLineType.Security.SECURITY_CHECK,
            titleRes = R.string.settings_button_security_check
        )
    )

    /**
     * Function that returns information if the biometric is available in the device. Check done
     * using MEPI SDK ([getMepiStatus]).
     *
     * @return true if biometric available else false (returned also when check failed)
     */
    private suspend fun isBiometricAvailable(): Boolean =
        getMepiStatus().getOrNull()?.biometricsAvailableOnDevice ?: false

    /**
     * Function that returns information if the Smart Key is available in the device. Check done
     * using MEPI SDK ([getMepiStatus]).
     *
     * @return true if Smart Key is available else false (returned also when check failed)
     */
    private suspend fun isSmartKeyLoginAvailable(): Boolean =
        getMepiStatus().getOrNull()?.caseMobileAvailableOnDevice ?: false

    /**
     * Handles settings action. Only when clicked item is [SettingsItemVo]. Triggers an action based
     * on which type of the item was clicked.
     * - [SettingsLineType.View] is handled by [handleSettingsViewAction].
     * - [SettingsLineType.Kate] is handled by [handleSettingsKateAction].
     * - [SettingsLineType.Notification] is handled by [handleSettingsNotificationAction].
     * - [SettingsLineType.Security] is handled by [handleSettingsSecurityAction].
     *
     * @param view used to get context, nav controller nad other
     * @param item used to decide which action to trigger based on type and additional information
     */
    private fun handleSettingsAction(view: View, item: SettingsLineVo) {
        logVerbose { "handleSettingsAction(item = $item)" }
        if (item is SettingsItemVo) {
            avoidDoubleClick(view, AllowTimer.SHORTEST) {
                when (item.type) {
                    is SettingsLineType.View -> handleSettingsViewAction(view, item)
                    SettingsLineType.Notification -> handleSettingsNotificationAction(view)
                    SettingsLineType.Kate -> handleSettingsKateAction(view)
                    is SettingsLineType.Security -> handleSettingsSecurityAction(view, item)
                }
            }
        }
    }

    /**
     * Handles settings for view actions [SettingsLineType.View].
     * - Theme shows settings dialog using [navigateToSettingDialog].
     * - Language shows settings dialog using [navigateToSettingDialog].
     * - Product order navigates to reorder fragment.
     * - Account owner sets show information using [setShowAccountOwner].
     *
     * @param view used to load NavController
     * @param item used to load type and additional information
     */
    private fun handleSettingsViewAction(view: View, item: SettingsItemVo) {
        logVerbose { "handleSettingsViewAction(item = $item)" }
        when (item.type) {
            SettingsLineType.View.THEME -> navigateToSettingDialog(view, SettingsDialogType.THEME)
            SettingsLineType.View.LANGUAGE ->
                navigateToSettingDialog(view, SettingsDialogType.LANGUAGE)
            SettingsLineType.View.PRODUCT_ORDER -> view.findNavController().navigate(
                SettingsFragmentDirections.actionToFragmentReorderProducts()
            )
            SettingsLineType.View.ACCOUNT_OWNER -> negateSettings(item, ::setShowAccountOwner)
            else -> Unit // Handles only view types
        }
    }

    /**
     * Negates settings that has a switch (if it does not then it logs a warning). Runs a block
     * which will be called with new boolean (negated) boolean value which is used to save the
     * settings.
     *
     * @param item used to check switch and check value
     * @param block used to save settings boolean value
     */
    private fun negateSettings(item: SettingsItemVo, block: (Boolean) -> Unit) {
        logVerbose { "negateSettings(item = $item)" }
        if (item.hasSwitch) {
            val show = !item.isChecked.value
            block(show)
            item.postIsChecked(show)
        } else {
            logWarn { "negateSettings() - item does not have switch -> nothing will happen" }
        }
    }

    /**
     * Navigates to setting dialog which allows user to pick from multiple options. Allowed dialogs
     * are identified by [SettingsDialogType].
     *
     * @param view used to find NavController
     * @param type decides which dialog type should be displayed
     */
    private fun navigateToSettingDialog(view: View, type: SettingsDialogType) {
        logVerbose { "navigateToSettingDialog(type = $type)" }
        view.findNavController().navigate(
            SettingsFragmentDirections.actionToDialogSettingsList(type)
        )
        _settingDialogViewState.postValue(SettingsDialogViewState.SHOWN)
    }

    /**
     * Sets information if the account owner should be displayed using [setOwnerPreference].
     *
     * @param show true if owner should be displayed else false
     */
    private fun setShowAccountOwner(show: Boolean) {
        logVerbose { "setShowAccountOwner(show = $show)" }
        launch(Dispatchers.Default) {
            setOwnerPreference(
                SetOwnerPreferenceUseCase.Params(show)
            )
        }
    }

    /**
     * Handles notification action by navigating to notification settings. There are no other
     * options at the moment.
     *
     * @param view used to find NavController
     */
    private fun handleSettingsNotificationAction(view: View) {
        logVerbose { "handleSettingsNotificationAction()" }
        avoidDoubleClick(view) {
            view.findNavController().navigate(
                SettingsFragmentDirections.actionToFragmentNotificationSettings()
            )
        }
    }

    /**
     * Handles kate action by running kate addon action. There are no other options at the moment.
     *
     * @param view used to avoid double click
     */
    private fun handleSettingsKateAction(view: View) {
        logVerbose { "handleSettingsKateAction()" }
        avoidDoubleClick(view) {
            launch(Dispatchers.Default) {
                callAction(
                    CallActionUseCase.Params(
                        action = AddonAction.Kate(
                            action = KATE_SETTINGS_INIT,
                            triggerType = ActionTriggerType.SETTINGS
                        )
                    )
                )
            }
        }
    }

    /**
     * Handles settings for view actions [SettingsLineType.Security].
     * - Biometry shows biometry enable/disable dialog using [showBiometricDialog].
     * - Smart key tries to enable/disable SK using [setSmartKeyAutoStartEnabled].
     * - Adobe enables or disables analytics using [negateSettings].
     * - Security check navigates to security section.
     *
     * @param view used to load NavController
     * @param item used to load type and additional information
     */
    private fun handleSettingsSecurityAction(view: View, item: SettingsItemVo) {
        logVerbose { "handleSettingsSecurityAction(item = $item)" }
        when (item.type) {
            SettingsLineType.Security.BIOMETRY -> if (!isDemoStage()) {
                showBiometricDialog(item, ::setBiometricLoginEnabled)
            }
            SettingsLineType.Security.SMART_KEY -> setSmartKeyAutoStartEnabled(item)
            SettingsLineType.Security.ADOBE -> negateSettings(item, ::setAdobeEnabled)
            SettingsLineType.Security.SECURITY_CHECK -> view.findNavController().navigate(
                SettingsFragmentDirections.actionToFragmentSecurity(startedFromSettings = true)
            )
            else -> Unit // Handles only security types
        }
    }

    /**
     * Checks if the app is in Demo stage. If true it displays the dialog.
     *
     * @return true if app in demo else false
     */
    private fun isDemoStage(): Boolean = runBlocking {
        (getActiveStage() == Stage.Demo).also {
            displayErrorDialog(R.string.common_error_popup_demo_disabled)
        }
    }

    /**
     * Shows biometric dialog informing the user that the app needs to be restarted in order for the
     * biometric to be enabled/disabled. If user clicks positive button it will negate the settings
     * and trigger [block] function.
     *
     * @param item used to check and negate checked state
     * @param block used to save settings boolean value
     */
    @MainThread
    private fun showBiometricDialog(item: SettingsItemVo, block: (Boolean) -> Unit) {
        logVerbose { "showBiometricDialog(item = $item)" }
        val config = GenericDialog.Config(
            titleRes = R.string.settings_switch_biometry,
            messageRes = if (item.isChecked.value) {
                R.string.settings_popup_text_biometry_deactivation
            } else {
                R.string.settings_popup_text_biometry_activation
            },
            positiveRes = R.string.settings_popup_button_biometry_change_logout,
            positiveCallback = { negateSettings(item, block) },
            negativeRes = R.string.settings_popup_button_biometry_change_do_not_logout
        )
        showDialog(config)
    }

    /**
     * Enables or disables Biometric login. Triggers [enableBiometricLogin] to enable
     * and [disableBiometricLogin] to disable. After the state is changed it will post
     * [PreferenceViewState.Restart] to the [_viewState] to notify the app that it should restart.
     *
     * @param enable true if it should enable else false
     */
    private fun setBiometricLoginEnabled(enable: Boolean) {
        logVerbose { "enableDisableBiometricLogin(enable = $enable)" }
        launch(Dispatchers.Default) {
            if (enable) {
                enableBiometricLogin()
            } else {
                disableBiometricLogin()
            }
            _viewState.postValue(PreferenceViewState.Restart)
        }
    }

    /**
     * Enables biometric login by setting multiple flags, deactivating SDK and logs user out of the
     * app.
     *
     * Note: After this function user should be logged out (this function does not trigger it).
     */
    private suspend fun enableBiometricLogin() {
        setBiometricLoginPreference(SetBiometricLoginPreferenceUseCase.Params(true))
        setSkipBiometricActivationFlag(SetSkipBiometricActivationFlagUseCase.Params(false))
        deactivateMepiSdk()
        logout()
    }

    /**
     * Disables biometric login by setting multiple flags, deactivating SDK and logs user out of the
     * app.
     *
     * Note: After this function user should be logged out (this function does not trigger it).
     */
    private suspend fun disableBiometricLogin() {
        setBiometricLoginPreference(SetBiometricLoginPreferenceUseCase.Params(false))
        setSkipBiometricActivationFlag(SetSkipBiometricActivationFlagUseCase.Params(true))
        logout()
    }

    /**
     * Sets information if SK should auto start if possible. Since this option is not possible when
     * biometric login is enabled it will try to disable it before this can be enabled. In that case
     * it must start Biometric disable dialog using [showBiometricDialog]. If the user agrees it will
     * enable SK and disable Biometric using [setSmartKeyAutoStartEnabledAfterDialog].
     *
     * If biometric is not enabled it just enables or disables SK auto start.
     *
     * @param item used to check and modify checked value
     */
    private fun setSmartKeyAutoStartEnabled(item: SettingsItemVo) {
        logVerbose { "setSmartKeyEnabled(item = $item)" }
        launch(Dispatchers.Default) {
            if (!item.isChecked.value && isBiometricAvailable() && getBiometricLoginPreference()) {
                withContext(Dispatchers.Main) {
                    showBiometricDialog(item, ::setSmartKeyAutoStartEnabledAfterDialog)
                }
            } else {
                negateSettings(item, ::setSmartKeyAutoStartPreference)
            }
        }
    }

    /**
     * Sets information if SK should auto start after user agrees with Biometric disable. Enables SK
     * autostart and disables Biometric and vice-versa even thought it should never happen.
     *
     * @param enable true if enable SK autostart and disable Biometric else false
     */
    private fun setSmartKeyAutoStartEnabledAfterDialog(enable: Boolean) {
        logVerbose { "setSmartKeyAutoStartEnabledAfterDialog(enable = $enable)" }
        setSmartKeyAutoStartPreference(enable)
        setBiometricLoginEnabled(!enable)
        (_settingItems.value.firstOrNull {
            it is SettingsItemVo && it.type == SettingsLineType.Security.BIOMETRY
        } as SettingsItemVo).postIsChecked(!enable)
    }

    /**
     * Sets the actual value for Smart key auto start into preferences.
     *
     * @param enabled if SK auto start enabled else false
     */
    private fun setSmartKeyAutoStartPreference(enabled: Boolean) {
        logVerbose { "setSmartKeyAutoStartPreference(enabled = $enabled)" }
        launch(Dispatchers.Default) {
            setSmartKeyLoginPreference(SetSmartKeyLoginPreferenceUseCase.Params(enabled))
        }
    }

    /**
     * Enables or disables Adobe event tracking. In case of enable it enables it and sends event
     * that tracking was enabled. Disable has it other way around it sends information that
     * analytics will be disabled and then it disables them.
     *
     * @param enable true if adobe analytics should be enabled else false
     */
    private fun setAdobeEnabled(enable: Boolean) {
        logVerbose { "setAdobeEnabled(enabled = $enable)" }
        launch(Dispatchers.Default) {
            setAdobeUc(enable)

            if (enable) {
                Analytics.setEnabled(enable)
                Analytics.trackEvent(AnalyticsEvent.SETTINGS_ADOBE.apply { action = "on" })
            } else {
                Analytics.trackEvent(AnalyticsEvent.SETTINGS_ADOBE.apply { action = "off" })
                Analytics.setEnabled(enable)
            }
        }
    }

    /**
     * Handles settings dialog action based on which item was clicked. Hides the dialog and proceeds
     * to handle items action based on type.
     * - [SettingsDialogLineType.Theme] is handled by [setThemeMode].
     * - [SettingsDialogLineType.Language] is handled by [showLanguageChangeDialog].
     *
     * @param view used to get context
     * @param item used to decide handle action and contains data sent to it
     */
    private fun handleSettingsDialogAction(view: View, item: SettingsDialogLineVo) {
        logVerbose { "handleSettingsDialogAction(item = $item)" }
        avoidDoubleClick(view) {
            _settingDialogViewState.value = SettingsDialogViewState.HIDE
            if (!item.isChecked) {
                when (item.type) {
                    is SettingsDialogLineType.Theme -> setThemeMode(item.type.mode)
                    is SettingsDialogLineType.Language -> showLanguageChangeDialog(
                        view.context,
                        item.type.language
                    )
                }
            }
        }
    }

    /**
     * Sets specific ThemeMode (this change is also tracked by analytics).
     *
     * @param mode new mode to be set
     */
    private fun setThemeMode(mode: ThemeMode) {
        logVerbose { "setThemeMode(mode = $mode)" }
        launch {
            Analytics.trackEvent(AnalyticsEvent.SETTINGS_SKIN.apply { action = mode.name })
            changeThemeModeUc(ChangeThemeModeUseCase.Params(mode))
            loadSettings()
        }
    }

    /**
     * Shows dialog informing user about restarting the app due to language change. User can confirm
     * it resulting with app restart or cancel it which does nothing.
     *
     * @param context used to modify language
     * @param newLanguage to be set
     */
    private fun showLanguageChangeDialog(context: Context, newLanguage: Language?) {
        logVerbose { "showLanguageChangeDialog(newLanguage = $newLanguage)" }
        val config = GenericDialog.Config(
            criticalDialog = true,
            titleRes = R.string.settings_popup_title_language_change,
            messageRes = R.string.settings_popup_text_language_change,
            positiveRes = R.string.other_text_yes,
            positiveCallback = { changeLanguage(context, newLanguage) },
            negativeRes = R.string.other_text_no,
            negativeCallback = { _viewState.value = PreferenceViewState.DoNothing }
        )
        showDialog(config)
    }

    /**
     * Changes application language. Saves the language, since the language can be null it then
     * loads current application language and sets it into [LocaleManager]. After that it tracks
     * this action and proceeds to logout the user and inform the Fragment that the language was
     * changed.
     *
     * @param context used to set new language to LocaleManager
     * @param language new language to be set (null = choose language by the system)
     */
    private fun changeLanguage(context: Context, language: Language?) {
        logDebug { "changeLanguage(language = $language)" }
        launch(Dispatchers.Default) {
            setLanguageUc(SetLanguageUseCase.Params(language))
            val selectedLanguage = getLanguageInfoUc().language
            LocaleManager.setNewLocale(context, selectedLanguage.value)
            setNotificationsLanguage(selectedLanguage)

            analytics.trackEvent(AnalyticsEvent.SETTINGS_LANGUAGE.apply {
                action = selectedLanguage.value
            })
            logout()
            _viewState.postValue(PreferenceViewState.Restart)
        }
    }

    /**
     * Sets notification language.
     *
     * Note: This feature is now under Feature Flag (aka not in production).
     *
     * @param selectedLanguage new language to set to notifications
     */
    private suspend fun setNotificationsLanguage(selectedLanguage: Language) {
        if (BuildConfig.FEATURE_BUILD) {
            // errors are ignored
            getNotificationsSettings().getOrNull()?.let {
                if (it.notificationStatusFlag) {
                    setNotificationsSettings(
                        SetNotificationsSettingsUseCase.Params(
                            NotificationMaReq(
                                businessFunction = BusinessFunction.NOTIFICATIONS_GET_SETTINGS,
                                notificationStatusFlag = true,
                                language = selectedLanguage,
                                profileList = null
                            )
                        )
                    ).getOrNull()
                }
            }
        }
    }
}

