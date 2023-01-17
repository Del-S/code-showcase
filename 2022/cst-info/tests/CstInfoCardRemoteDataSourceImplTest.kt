package cz.csob.smartbanking.cstinfo.infrastructure.source.part

import com.squareup.moshi.Moshi
import cz.csob.smartbanking.codebase.api.cstinfo.CstInfoApi
import cz.csob.smartbanking.core.domain.language.model.Language
import cz.csob.smartbanking.core.domain.language.model.LanguageInfo
import cz.csob.smartbanking.core.domain.language.usecase.GetLanguageInfoUseCase
import cz.csob.smartbanking.cstinfo.infrastructure.mapper.model.CstInfoCardInfoMapperTestModel
import cz.csob.smartbanking.cstinfo.infrastructure.mapper.model.CstInfoCardMapperTestModel
import cz.csob.smartbanking.cstinfo.infrastructure.source.testCstFetchFailed
import cz.csob.smartbanking.cstinfo.infrastructure.source.testCstFetchSuccess
import cz.csob.smartbanking.core.infrastructure.api.CsobResultCaller
import cz.eman.kaal.domain.result.Result
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import retrofit2.Response

/**
 * Test class for [CstInfoCardRemoteDataSourceImpl].
 *
 * @author eMan a.s.
 */
class CstInfoCardRemoteDataSourceImplTest : StringSpec({

    val mockCstInfoApi = mockk<CstInfoApi>()
    val mockGetLanguageInfo = mockk<GetLanguageInfoUseCase>()
    val mockCsobResultCaller = mockk<CsobResultCaller>()
    val mockMoshi = mockk<Moshi>()

    val sourceWithMockedCaller =
        CstInfoCardRemoteDataSourceImpl(
            cstInfoApi = mockCstInfoApi,
            getLanguageInfo = mockGetLanguageInfo,
            csobResultCaller = mockCsobResultCaller
        )
    val csobResultCaller = CsobResultCaller(mockMoshi)
    val source =
        CstInfoCardRemoteDataSourceImpl(
            cstInfoApi = mockCstInfoApi,
            getLanguageInfo = mockGetLanguageInfo,
            csobResultCaller = csobResultCaller
        )

    coEvery { mockGetLanguageInfo() } returns LanguageInfo(
        language = Language.CZECH,
        fromSystem = true
    )

    /**
     * Card limits CST tests.
     */
    "Card limits CST fetched correctly" {
        val limit = CstInfoCardMapperTestModel.cstCardLimit
        testCstFetchSuccess(mockCsobResultCaller, limit) {
            sourceWithMockedCaller.fetchCardLimits(limit.cid)
        }
    }

    "Card limits CST fetched correctly (mocked API only)" {
        val limit = CstInfoCardMapperTestModel.cstCardLimit
        coEvery {
            mockCstInfoApi.getCstInfoMa(any())
        } returns Response.success(CstInfoCardMapperTestModel.cstLimitDto)
        sourceWithMockedCaller.fetchCardLimits(limit.cid) shouldBe Result.success(limit)
    }

    "Card limits CST fetch fails" {
        testCstFetchFailed(mockCstInfoApi) {
            source.fetchCardLimits(CstInfoCardMapperTestModel.cstCardLimit.cid)
        }
    }

    /**
     * Cards CST tests.
     */
    "Cards CST (type + logical status) fetched correctly" {
        testCstFetchSuccess(mockCsobResultCaller, CstInfoCardMapperTestModel.cstTypeStatus) {
            sourceWithMockedCaller.fetchCardCst(CstInfoCardMapperTestModel.params)
        }
    }

    "Cards CST (type + logical status) fetched correctly (mocked API only)" {
        coEvery {
            mockCstInfoApi.getCstInfoMa(any())
        } returns Response.success(CstInfoCardMapperTestModel.cstTypeStatusDto)
        source.fetchCardCst(CstInfoCardMapperTestModel.params) shouldBe
            Result.success(CstInfoCardMapperTestModel.cstTypeStatus)
    }

    "Cards CST (type + logical status) fetch fails" {
        testCstFetchFailed(mockCstInfoApi) {
            source.fetchCardCst(CstInfoCardMapperTestModel.params)
        }
    }

    /**
     * Country code CST tests.
     */
    "Country code CST fetched correctly" {
        val countryCode = CstInfoCardMapperTestModel.countryCode
        testCstFetchSuccess(mockCsobResultCaller, countryCode) {
            sourceWithMockedCaller.fetchCountryCode(countryCode.cid)
        }
    }

    "Country code CST fetched correctly (mocked API only)" {
        val countryCode = CstInfoCardMapperTestModel.countryCode
        coEvery {
            mockCstInfoApi.getCstInfoMa(any())
        } returns Response.success(CstInfoCardMapperTestModel.cstCountryCodeDto)
        source.fetchCountryCode(countryCode.cid) shouldBe Result.success(countryCode)
    }

    "Country code CST fetch fails" {
        testCstFetchFailed(mockCstInfoApi) {
            source.fetchCountryCode(CstInfoCardMapperTestModel.countryCode.cid)
        }
    }

    /**
     * Card CST Info tests.
     */
    "Card CST Info fetched correctly (delivery method)" {
        testCstFetchSuccess(
            mockCsobResultCaller,
            CstInfoCardInfoMapperTestModel.csobCardInfoMethod
        ) {
            sourceWithMockedCaller.fetchCardInfo(CstInfoCardInfoMapperTestModel.paramsForMethod)
        }
    }

    "Card CST Info fetched correctly (mocked API only - delivery method)" {
        coEvery {
            mockCstInfoApi.getCstInfoMa(any())
        } returns Response.success(CstInfoCardInfoMapperTestModel.csobCardInfoDeliveryMethodDto)
        sourceWithMockedCaller.fetchCardInfo(CstInfoCardInfoMapperTestModel.paramsForMethod) shouldBe
            Result.success(CstInfoCardInfoMapperTestModel.csobCardInfoMethod)
    }


    "Cards CST Info fetch fails" {
        testCstFetchFailed(mockCstInfoApi) {
            source.fetchCardInfo(CstInfoCardInfoMapperTestModel.paramsForMethod)
        }
    }

    "Card CST Info fetched correctly (delivery address)" {
        testCstFetchSuccess(
            mockCsobResultCaller,
            CstInfoCardInfoMapperTestModel.csobCardInfoAddress
        ) {
            sourceWithMockedCaller.fetchCardInfo(CstInfoCardInfoMapperTestModel.paramsForAddress)
        }
    }

    "Card CST Info fetched correctly (mocked API only - delivery address)" {
        coEvery {
            mockCstInfoApi.getCstInfoMa(any())
        } returns Response.success(CstInfoCardInfoMapperTestModel.csobCardInfoDeliveryAddressDto)
        sourceWithMockedCaller.fetchCardInfo(CstInfoCardInfoMapperTestModel.paramsForAddress) shouldBe
            Result.success(CstInfoCardInfoMapperTestModel.csobCardInfoAddress)
    }
})
