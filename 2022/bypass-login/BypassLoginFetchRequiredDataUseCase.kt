package cz.csob.smartbanking.feature.login.domain.usecase

import cz.csob.smartbanking.codebase.domain.mepi.login.usecase.CallPostLoginOperationsUseCase
import cz.csob.smartbanking.codebase.domain.pushapp.usecase.CreateNotificationChannelPushAppUseCase
import cz.csob.smartbanking.globalsettings.domain.usecase.GetGlobalSettingsUseCase
import cz.csob.smartbanking.products.domain.marketinginfo.usecase.FetchServiceSmartTipsUseCase
import cz.eman.kaal.domain.result.Result
import cz.eman.kaal.domain.result.chain
import cz.eman.kaal.domain.result.map
import cz.eman.kaal.domain.usecases.UseCaseResultNoParams

/**
 * This UC is used to fetch and save information required during bypass. It saves token metadata and
 * icid. Then it fetches global settings and calls post login operations which are required to pass.
 * If any of those fails it will not continue and result Result.error. If they pass then it creates
 * notification channel and fetches service smart tip. These two are not checked for result, thus
 * it will be a Result.success even if they fail.
 *
 * @author eMan a.s.
 */
class BypassLoginFetchRequiredDataUseCase(
    private val getTokenMetadata: CallGetTokenMetadataUseCase,
    private val saveTokenMetadata: SaveTokenMetadataUseCase,
    private val saveIcid: SaveIcidUseCase,
    private val getGlobalSettings: GetGlobalSettingsUseCase,
    private val callPostLoginOperations: CallPostLoginOperationsUseCase,
    private val createNotificationChannelPushApp: CreateNotificationChannelPushAppUseCase,
    private val fetchServiceSmartTips: FetchServiceSmartTipsUseCase,
) : UseCaseResultNoParams<Unit>() {

    override suspend fun doWork(params: Unit): Result<Unit> = getTokenMetadata().chain {
        saveTokenMetadata(SaveTokenMetadataUseCase.Params(it))
        saveIcid(SaveIcidUseCase.Params(it.userInfo))

        getGlobalSettings(GetGlobalSettingsUseCase.Params()).chain {
            callPostLoginOperations().chain {
                createNotificationChannelPushApp()
                fetchServiceSmartTips(FetchServiceSmartTipsUseCase.Params())
                Result.success(Unit)
            }
        }
    }.map { }
}
