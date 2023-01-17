package cz.csob.smartbanking.feature.login.domain.mapper

import cz.csob.smartbanking.codebase.domain.features.environment.model.Environment
import cz.csob.smartbanking.feature.login.domain.model.BypassLoginToken
import cz.csob.smartbanking.utils.extensions.safeLet

/**
 * Mapper for [BypassLoginToken].
 *
 * @author eMan a.s.
 */

/**
 * Maps [BypassLoginToken] into string that can be saved permanently. Current format is:
 * PREFIX_OLI**oli**|PREFIX_ENV**env**|PREFIX_ACCESS_TOKEN**accessToken**|REGEX_SSL_CERTIFICATE**certificate** .
 *
 * @return String which can be saved
 */
fun BypassLoginToken.toSaveString(): String = buildString {
    append(PREFIX_OLI)
    append(oli)
    append('|')
    append(PREFIX_ENV)
    append(env?.name)
    append('|')
    append(PREFIX_ACCESS_TOKEN)
    append(accessToken)
    append('|')
    append(PREFIX_SSL_CERTIFICATE)
    append(certificate)
}

/**
 * Maps String into [BypassLoginToken] or null when it cannot be done. Uses regexes to find values
 * for login token in string. Current format is:
 * PREFIX_OLI**oli**|PREFIX_ENV**env**|PREFIX_ACCESS_TOKEN**accessToken**|REGEX_SSL_CERTIFICATE**certificate** .
 * This format is returned by ID helper.
 *
 * There are only two required values and those are accessToken and ssl certificate which are
 * required to bypass the login. If they have been found then token is returned else null is
 * returned.
 *
 * @return [BypassLoginToken] or null when it cannot be parsed
 */
fun String.toBypassLoginToken(): BypassLoginToken? {
    val oli = REGEX_OLI.find(this)?.value
    val env = REGEX_ENV.find(this)?.value
    val accessToken = REGEX_ACCESS_TOKEN.find(this)?.value
    val sslCert = REGEX_SSL_CERTIFICATE.find(this)?.value

    return safeLet(accessToken, sslCert) { token, cert ->
        BypassLoginToken(
            accessToken = token,
            certificate = cert,
            oli = oli,
            env = env?.let { Environment.getEnvironmentByName(it) },
        )
    }
}

// Prefixes for bypass token values
private const val PREFIX_OLI = "OLI="
private const val PREFIX_ENV = "ENV="
private const val PREFIX_ACCESS_TOKEN = "AT="
private const val PREFIX_SSL_CERTIFICATE = "CER="

// Regexes for bypass token values
private val REGEX_OLI = "(?<=$PREFIX_OLI)([^|])*".toRegex()
private val REGEX_ENV = "(?<=$PREFIX_ENV)([^|])*".toRegex()
private val REGEX_ACCESS_TOKEN = "(?<=$PREFIX_ACCESS_TOKEN)([^|])*".toRegex()
private val REGEX_SSL_CERTIFICATE = "(?<=$PREFIX_SSL_CERTIFICATE).*".toRegex()
