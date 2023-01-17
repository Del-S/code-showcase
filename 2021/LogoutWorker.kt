package cz.csob.smartbanking.codebase.infrastructure.logout

import android.content.Context
import android.webkit.CookieManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import cz.csob.smartbanking.codebase.data.logout.LogoutLocalSource
import cz.csob.smartbanking.codebase.domain.logout.usecase.LogoutUseCase
import cz.csob.smartbanking.codebase.domain.mepi.login.usecase.GetTokenUseCase
import cz.eman.logger.logVerbose
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent

/**
 * Logout service handling user logout when the app is in the background.
 *
 * @author eMan a.s.
 */
class LogoutWorker(context: Context, params: WorkerParameters) : Worker(context, params),
    KoinComponent {

    private val logoutLocalSource: LogoutLocalSource by getKoin().getScope(SCOPE_RETROFIT_ID)
        .inject()

    override fun doWork(): Result = runBlocking {
        this@LogoutWorker.logVerbose { "LogoutWorker.doWork()" }
        val hasToken = getKoin().get<GetTokenUseCase>()()
        when (hasToken) {
            null -> CookieManager.getInstance().removeAllCookies(null)
            else -> {
                val logout = getKoin().get<LogoutUseCase>()
                logout()
            }
        }
        val logout = getKoin().get<LogoutUseCase>()
        logout()
        logoutLocalSource.setIsAfterBackgroundLogout(isForBidon = hasToken == null)
        Result.success()
    }

    companion object {
        // TODO: Not ideal Unify with other parts of the app
        const val SCOPE_RETROFIT_ID = "RetrofitScopeId"
    }
}
