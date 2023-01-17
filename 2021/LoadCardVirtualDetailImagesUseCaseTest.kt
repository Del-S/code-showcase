package cz.csob.smartbanking.addon.cards.domain.feature.cards.usecase

import cz.csob.smartbanking.addon.cards.domain.feature.CardErrorCode
import cz.csob.smartbanking.addon.cards.domain.feature.cards.model.administration.CardVirtualImages
import cz.csob.smartbanking.addon.cards.domain.feature.cards.repository.CardsRepository
import cz.csob.smartbanking.addon.cards.domain.feature.cards.usecase.LoadCardVirtualDetailImagesUseCase.Companion.DEFAULT_BACK_TIMEOUT
import cz.csob.smartbanking.addon.cards.domain.feature.cards.usecase.LoadCardVirtualDetailImagesUseCase.Companion.DEFAULT_FRONT_TIMEOUT
import cz.csob.smartbanking.codebase.domain.features.globalsettings.model.GlobalSettingsKey
import cz.csob.smartbanking.codebase.domain.features.globalsettings.usecase.GetGlobalSettingsCurrentValueUseCase
import cz.eman.kaal.domain.result.ErrorCode
import cz.eman.kaal.domain.result.Result
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.apache.commons.io.input.NullInputStream
import java.io.InputStream

/**
 * Tests for [LoadCardVirtualDetailImagesUseCase].
 *
 * @author eMan s.r.o.
 */
class LoadCardVirtualDetailImagesUseCaseTest : StringSpec({

    val cardsRepository = mockk<CardsRepository>()
    val globalSettingsCurrentValue = mockk<GetGlobalSettingsCurrentValueUseCase>()

    val defaultFrontTime = 50L
    val defaultBackTime = 40L
    val defaultParams = LoadCardVirtualDetailImagesUseCase.Params("", "")
    val cardImageStream = NullInputStream(0)
    val cardImageResult = Result.success(cardImageStream)

    val virtualDetailUseCase = LoadCardVirtualDetailImagesUseCase(
        cardsRepository,
        globalSettingsCurrentValue
    )

    coEvery {
        globalSettingsCurrentValue(
            GetGlobalSettingsCurrentValueUseCase.Params(GlobalSettingsKey.CARD_FRONT_TIMEOUT.key)
        )
    } returns defaultFrontTime.toString()

    coEvery {
        globalSettingsCurrentValue(
            GetGlobalSettingsCurrentValueUseCase.Params(GlobalSettingsKey.CARD_BACK_TIMEOUT.key)
        )
    } returns defaultBackTime.toString()

    coEvery {
        cardsRepository.getCardVirtualImage(any())
    } returns cardImageResult

    coEvery {
        cardsRepository.saveVirtualImages(any())
    } returns Result.success(Unit)

    "CardVirtualDetail should be loaded successfully" {
        val callResult = virtualDetailUseCase(defaultParams)
        coVerify { cardsRepository.saveVirtualImages(any()) }
        callResult shouldBe Result.success(Unit)
    }

    "CardVirtualDetail should have GlobalSettings timeouts" {
        val callResult = virtualDetailUseCase(defaultParams)
        coVerify {
            cardsRepository.saveVirtualImages(
                CardVirtualImages(
                    cardImageStream,
                    cardImageStream,
                    defaultFrontTime,
                    defaultBackTime
                )
            )
        }
        callResult shouldBe Result.success(Unit)
    }

    "CardVirtualDetail should have default timeouts" {
        coEvery {
            globalSettingsCurrentValue(any())
        } returns null

        val callResult = virtualDetailUseCase(defaultParams)
        coVerify {
            cardsRepository.saveVirtualImages(
                CardVirtualImages(
                    cardImageStream,
                    cardImageStream,
                    DEFAULT_FRONT_TIMEOUT,
                    DEFAULT_BACK_TIMEOUT
                )
            )
        }
        callResult shouldBe Result.success(Unit)
    }

    "CardVirtualDetail should fail due to saving error" {
        val errorResult = Result.error<Unit>(ErrorCode.UNDEFINED)
        coEvery {
            cardsRepository.saveVirtualImages(any())
        } returns errorResult

        val callResult = virtualDetailUseCase(defaultParams)
        coVerify { cardsRepository.saveVirtualImages(any()) }
        callResult shouldBe errorResult
    }

    "CardVirtualDetail should fail due to front image error" {
        val frontUrl = "frontUrl"
        val params = LoadCardVirtualDetailImagesUseCase.Params(frontUrl, "")
        val errorResult = Result.error<InputStream>(ErrorCode.UNDEFINED)
        val errorResultVirtual = Result.error<Unit>(CardErrorCode.CARD_IMAGE_FRONT)
        coEvery {
            cardsRepository.getCardVirtualImage(frontUrl)
        } returns errorResult

        val callResult = virtualDetailUseCase(params)
        coVerify { cardsRepository.getCardVirtualImage(frontUrl) }
        callResult shouldBe errorResultVirtual
    }

    "CardVirtualDetail should fail due to back image error" {
        val backUrl = "backUrl"
        val params = LoadCardVirtualDetailImagesUseCase.Params("", backUrl)
        val errorResult = Result.error<InputStream>(ErrorCode.UNDEFINED)
        val errorResultVirtual = Result.error<Unit>(CardErrorCode.CARD_IMAGE_BACK)
        coEvery {
            cardsRepository.getCardVirtualImage(backUrl)
        } returns errorResult

        val callResult = virtualDetailUseCase(params)
        coVerify { cardsRepository.getCardVirtualImage(backUrl) }
        callResult shouldBe errorResultVirtual
    }
})
