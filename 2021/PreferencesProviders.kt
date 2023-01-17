package cz.csob.smartbanking.core.infrastructure.pref

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.enableOrAddPrimaryKey
import com.chibatching.kotpref.PreferencesProvider
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.shaded.protobuf.InvalidProtocolBufferException
import cz.csob.smartbanking.core.infrastructure.security.AndroidKeyStoreManager
import cz.eman.logger.logWarn
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private val KEY_ENCRYPTION_SCHEME = EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV
private val VALUE_ENCRYPTION_SCHEME =
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM

/**
 * Provides implementation of [PreferencesProvider] that provides encrypted Shared Preferences.
 *
 * @see EncryptedPreferencesProvider
 * @see EncryptedSharedPreferences
 */
fun encryptedPreferencesProvider(): PreferencesProvider = EncryptedPreferencesProvider

private object EncryptedPreferencesProvider : PreferencesProvider {

    private val instances: MutableMap<String, SharedPreferences> = ConcurrentHashMap()
    private val clearedKeystore = AtomicBoolean(false)

    override fun get(context: Context, name: String, mode: Int): SharedPreferences {
        require(mode == Context.MODE_PRIVATE) { "Mode 0x%x isn't supported".format(mode) }
        return instances.computeIfAbsent(name) { fileName ->
            createEncryptedPreferences(context, fileName)
        }
    }

    /**
     * Tries to create an instance of encrypted SharedPreferences using [createPreferences]. If the
     * first creation fails it will try to fix the issue using [handleCreateException] if that does
     * not work it recreates shared preferences completely using [tryRecreateSharedPreferences].
     *
     * Since the lib is not able to open the preferences (usually due to missing or invalid
     * encryption/master key). It will destroy the preferences and refreshes the master key. There
     * is no way to recover data anymore so there is no need to crash the app an just do a "soft"
     * reset by deleting the preferences.
     *
     * @param context used to load preferences and keystore
     * @param fileName used to identify preferences file; can not contain path separators.
     * @return The SharedPreferences instance that encrypts all data.
     * @see createPreferences
     * @see handleCreateException
     * @see tryRecreateSharedPreferences
     */
    @Throws(GeneralSecurityException::class, IOException::class)
    private fun createEncryptedPreferences(context: Context, fileName: String): SharedPreferences {
        logWarn("createEncryptedPreferences(fileName = $fileName)")
        return try {
            createPreferences(context, fileName)
        } catch (ex: Exception) {
            synchronized(this) {
                try {
                    handleCreateException(ex, context, fileName)
                } catch (ex: Exception) {
                    tryRecreateSharedPreferences(context, fileName, true)
                }
            }
        }
    }

    /**
     * Handles create exception based on it's type. There are three types checked:
     * 1) [InvalidKeyException] encryption key (usually master key) is not valid or usable ->
     * preferences is completely destroyed with the master key to create it again. Done using
     * [tryRecreateSharedPreferences] and is destructive.
     * 2) [GeneralSecurityException] primary encryption key (do not confuse with master) is not
     * valid or usable -> tries to fix primary encryption key (promote or create a new one) using
     * [fixPrimaryKey]. Usually destructive when new key is created.
     * 3) [InvalidProtocolBufferException] tink is not able to decrypt data for whatever reason ->
     * recreates current shared pref file but does not recreate master key.
     * 4) Every other exception is re-thrown.
     *
     * Note: master key is used to open [AndroidKeysetManager] to access primary key and value
     * encryption keys. This key is stored in AndroidKeystore which is not reliable.
     *
     * Possible destructive fix for issues:
     * 1) Tink: https://github.com/google/tink/issues/413,
     *    Google: https://issuetracker.google.com/issues/164901843.
     * 2) Google: https://issuetracker.google.com/issues/176215143.
     * 3) Google other issues: https://issuetracker.google.com/issues?q=status:open%20componentid:618647&s=created_time:desc.
     *
     * @param ex to determine what type of error occurred
     * @param context used to access shared pref file or [AndroidKeysetManager]
     * @param fileName of preferences being created
     * @return The SharedPreferences instance that encrypts all data.
     * @see tryRecreateSharedPreferences
     * @see fixPrimaryKey
     */
    private fun handleCreateException(
        ex: Exception,
        context: Context,
        fileName: String
    ): SharedPreferences {
        logWarn("handleCreateException()", ex)
        return when (ex) {
            is InvalidKeyException ->
                tryRecreateSharedPreferences(context, fileName, true)
            is GeneralSecurityException -> fixPrimaryKey(context, fileName)
            is InvalidProtocolBufferException ->
                tryRecreateSharedPreferences(context, fileName, false)
            else -> throw ex
        }
    }

    /**
     * Tries to fix primary keys for preference file using [enableOrAddPrimaryKey]. After this it
     * tries to create preferences file.
     *
     * Note: that this function can and will throw and exception when master key is not valid.
     *
     * @param context used to load preferences and keystore
     * @param fileName used to identify preferences file; can not contain path separators.
     * @return The SharedPreferences instance that encrypts all data.
     */
    private fun fixPrimaryKey(context: Context, fileName: String): SharedPreferences {
        logWarn("fixPrimaryKey()")
        enableOrAddPrimaryKey(
            fileName,
            AndroidKeyStoreManager.getOrCreateMasterKey(context),
            context,
            KEY_ENCRYPTION_SCHEME,
            VALUE_ENCRYPTION_SCHEME
        )
        return createPreferences(context, fileName)
    }

    /**
     * Tries to recreates preferences by deleting them and creating them again. This should be done
     * when encryption or master keys have changed. There is an option to recreate master key in
     * which should be triggered in cases when it is not usable anymore (not in keystore or not
     * usable by the encryption library).
     *
     * Note: master key recreation can be done only once per application runs (checked using atomic
     * [clearedKeystore]) since we do not want to recreate the master key every time we create
     * shared pref file. It would never save any data since for every share pref there would be a
     * different master key and it would recreate it every single time.
     *
     * @param context used to load preferences and keystore
     * @param fileName used to identify preferences file; can not contain path separators.
     * @param recreateMasterKey true if master key should be recreated
     * @return The SharedPreferences instance that encrypts all data.
     */
    private fun tryRecreateSharedPreferences(
        context: Context,
        fileName: String,
        recreateMasterKey: Boolean
    ): SharedPreferences {
        logWarn("tryRecreateSharedPreferences(recreateMasterKey = $recreateMasterKey)")
        if (recreateMasterKey && !clearedKeystore.get()) {
            AndroidKeyStoreManager.getOrCreateMasterKey(context, true)
            clearedKeystore.set(true)
        }
        context.deleteSharedPreferences(fileName)
        return createPreferences(context, fileName)
    }

    /**
     * Creates an instance of [EncryptedSharedPreferences] for specific [fileName].
     *
     * @param context used to load preferences and keystore
     * @param fileName used to identify preferences file
     * @return [SharedPreferences] (encrypted)
     * @throws GeneralSecurityException when encryption keys could not be used
     * @throws IOException when fileName can not be used
     * @see EncryptedSharedPreferences.create
     */
    @Throws(GeneralSecurityException::class, IOException::class)
    private fun createPreferences(context: Context, fileName: String): SharedPreferences {
        return EncryptedSharedPreferences.create(
            fileName,
            AndroidKeyStoreManager.getOrCreateMasterKey(context),
            context,
            KEY_ENCRYPTION_SCHEME,
            VALUE_ENCRYPTION_SCHEME
        )
    }
}
