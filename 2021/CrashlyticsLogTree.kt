package cz.csob.smartbanking.app.log

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.internal.persistence.FileStoreImpl
import cz.csob.smartbanking.app.SmartBankingApplication
import cz.csob.smartbanking.codebase.domain.features.logs.usecase.DeleteCrashlyticsLogUseCase
import cz.eman.kaal.domain.result.onSuccess
import cz.eman.logger.logVerbose
import java.io.File
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.getKoin

/**
 * Crashlytics Log [timber.log.Timber.Tree] implementation
 *
 * @property defaultTag Default tag value, which is used if logged tag is null or blank
 * @param level Logging level threshold ([Log.INFO] is default)
 *
 * @author [eMan a.s.](mailto:info@eman.cz)
 */
class CrashlyticsLogTree(
    private val application: SmartBankingApplication,
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance(),
    private val defaultTag: String = "Crashlytics",
    private val level: Int = Log.INFO
) : AbstractLogTree(level) {

    init {
        require(level >= Log.INFO) {
            "Log level of CrashlyticsLogTree cannot be lower than INFO for security reasons"
        }
        require(defaultTag.isNotBlank()) { "Default tag of CrashlyticsLogTree cannot be blank" }
        deleteLogFiles()
    }

    /**
     * Logs tag, message and throwable message into crashlytics.
     */
    override fun log(priority: Int, tag: String?, message: String, throwable: Throwable?) {
        crashlytics.log(buildString {
            append(tag)
            append(": ")
            append(message)
            throwable?.let {
                append(it.message)
            }
        })
    }

    /**
     * Deletes crashlytics log files using [DeleteCrashlyticsLogUseCase]. Loads log directory same
     * as crashlytics (CrashlyticsCore.getLogFileDir()). Loads use case from koin and uses
     * AppCoroutineScope to trigger coroutine which deletes log files when needed. Success result
     * is logged since UC does not have error result.
     *
     * @see FileStoreImpl
     * @see File
     * @see DeleteCrashlyticsLogUseCase
     */
    private fun deleteLogFiles() {
        val fileStore = FileStoreImpl(application.applicationContext)
        val logsDirectory = File(fileStore.filesDir, "log-files")
        val deleteUc = getKoin().get<DeleteCrashlyticsLogUseCase>()
        application.appCoroutineScope.launch {
            deleteUc(DeleteCrashlyticsLogUseCase.Params(logsDirectory)).onSuccess {
                logVerbose { "CrashlyticsLogTree log delete result $it" }
            }
        }
    }
}
