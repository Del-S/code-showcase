package cz.csob.smartbanking.feature.login.domain.usecase

import cz.csob.smartbanking.codebase.domain.features.security.usecases.GetRetrofitReloadListenerUseCase
import cz.csob.smartbanking.codebase.domain.features.security.usecases.SetBypassSslCertificateUseCase
import cz.csob.smartbanking.codebase.domain.mepi.login.model.LoginOutput
import cz.csob.smartbanking.codebase.domain.mepi.login.model.Token
import cz.csob.smartbanking.codebase.domain.mepi.login.usecase.SaveTokenUseCase
import cz.csob.smartbanking.feature.login.domain.mapper.toBypassLoginToken
import cz.eman.kaal.domain.result.ErrorResult
import cz.eman.kaal.domain.result.Result
import cz.eman.kaal.domain.result.chain
import cz.eman.kaal.domain.usecases.UseCaseResult

/**
 * This UC is used to bypass login from debug setup.
 *
 * @author eMan a.s.
 */
class BypassLoginUseCase(
    private val saveToken: SaveTokenUseCase,
    private val setBypassSslCertificate: SetBypassSslCertificateUseCase,
    private val setBypassLoginToken: SetBypassLoginTokenUseCase,
    private val getLoginStateString: GetLoginStateStringUseCase,
    private val initCmiInstanceId: InitCmiInstanceIdUseCase,
    private val verifyLoginOutput: VerifyLoginOutputUseCase,
    private val getRetrofitReloadListener: GetRetrofitReloadListenerUseCase,
) : UseCaseResult<Unit, BypassLoginUseCase.Params>() {

    override suspend fun doWork(params: Params): Result<Unit> =
        params.loginTokenString.toBypassLoginToken()?.let {
            val token = Token(
                accessToken = it.accessToken,
                expiresIn = "0",
                tokenType = "bearer",
                scope = "SCOPEFULL"
            )

            saveToken(SaveTokenUseCase.Params(token))
            setBypassSslCertificate(SetBypassSslCertificateUseCase.Params(it.certificate))
            setBypassLoginToken(SetBypassLoginTokenUseCase.Params(it))

            initCmiInstanceId(InitCmiInstanceIdUseCase.Params(
                isApplicationActivated = false
            )).chain {
                verifyLoginOutput(LoginOutput(token, getLoginStateString(), null)).chain {
                    // We need to re-create Retrofit instance (and all APIs, Sources, Repos
                    // dependant on it) for SSL certificate pinning
                    getRetrofitReloadListener()?.reload()
                    Result.success(Unit)
                }
            }
        } ?: Result.error(ErrorResult()) // TODO: add error code

    /**
     * @property loginTokenString which should contain access token and cert
     */
    data class Params(val loginTokenString: String)
}

