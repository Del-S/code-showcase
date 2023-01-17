package cz.csob.smartbanking.cstinfo.infrastructure.source

import cz.csob.smartbanking.codebase.api.cstinfo.CstInfoApi
import cz.csob.smartbanking.codebase.api.cstinfo.model.CstInfoResDto
import cz.csob.smartbanking.core.infrastructure.api.CsobResultCaller
import cz.eman.kaal.domain.result.HttpStatusErrorCode
import cz.eman.kaal.domain.result.Result
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Common functions for CST info data source tests.
 *
 * @author eMan a.s.
 */

/**
 * Tests CST fetch success using mocked caller.
 *
 * @param caller CSOB caller which should be a mock
 * @param expected expected fetched result
 * @param call which should return expected value
 */
suspend fun <T> testCstFetchSuccess(
    caller: CsobResultCaller,
    expected: T,
    call: suspend () -> T
) {
    coEvery {
        caller.callResult<CstInfoResDto, T>(
            any(),
            any(),
            any()
        )
    } returns Result.success(expected)
    call() shouldBe Result.success(expected)
}

/**
 * Tests CST fetch failed using cst api.
 *
 * @param api cst api that should be a mock
 * @param call which should return expected value
 */
suspend fun <T> testCstFetchFailed(
    api: CstInfoApi,
    call: suspend () -> T
) {
    val errorCode = HttpStatusErrorCode.INTERNAL_SERVER_ERROR
    val errorResponseBody = ResponseBody.create(MediaType.get("application/json"), "{}")
    coEvery {
        api.getCstInfoMa(any())
    } returns Response.error(errorCode.value, errorResponseBody)
    val result = call()
    result.shouldBeInstanceOf<Result.Error<*>>()
    result.error.code shouldBe errorCode
}
