package cz.csob.smartbanking.cstinfo.infrastructure.mapper

import cz.csob.smartbanking.core.domain.language.model.Language
import cz.csob.smartbanking.cstinfo.infrastructure.mapper.model.CstInfoCardInfoMapperTestModel
import cz.csob.smartbanking.cstinfo.infrastructure.mapper.model.CstInfoCardMapperTestModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

/**
 * Tests for [CstInfoCardMapper].
 *
 * @author eMan a.s.
 */
class CstInfoCardMapperTest : StringSpec({

    /**
     * Cards CST (type + logical status) tests.
     */
    "Cards CST (type + logical status) mapped correctly" {
        CstInfoCardMapper.mapCardCstFromDto(
            CstInfoCardMapperTestModel.cstTypeStatusDto,
            CstInfoCardMapperTestModel.params,
            Language.CZECH
        ) shouldBe CstInfoCardMapperTestModel.cstTypeStatus
    }

    "Cards CST (type + logical status) mapped correctly (ignoring invalid cst)" {
        CstInfoCardMapper.mapCardCstFromDto(
            CstInfoCardMapperTestModel.cstTypeStatusDtoWithInvalid,
            CstInfoCardMapperTestModel.params,
            Language.CZECH
        ) shouldBe CstInfoCardMapperTestModel.cstTypeStatus
    }

    "Cards CST (type + logical status) mapped correctly but empty" {
        CstInfoCardMapper.mapCardCstFromDto(
            CstInfoCardMapperTestModel.cstTypeStatusDtoEmpty,
            CstInfoCardMapperTestModel.params,
            Language.CZECH
        ) shouldBe CstInfoCardMapperTestModel.cstTypeStatusEmpty
    }

    "Cards CST (type + logical status) map does not throw exception due to missing sub query" {
        assertDoesNotThrow {
            CstInfoCardMapper.mapCardCstFromDto(
                CstInfoCardMapperTestModel.cstTypeStatusDto,
                CstInfoCardMapperTestModel.params,
                Language.ENGLISH
            )
        }
    }

    /**
     * Cards CST Info tests.
     */
    "Card CST Info mapped correctly (delivery method)" {
        CstInfoCardMapper.mapCardInfoFromDto(CstInfoCardInfoMapperTestModel.csobCardInfoDeliveryMethodDto) shouldBe
            CstInfoCardInfoMapperTestModel.csobCardInfoMethod
    }

    "Card CST Info mapped correctly (delivery address)" {
        CstInfoCardMapper.mapCardInfoFromDto(CstInfoCardInfoMapperTestModel.csobCardInfoDeliveryAddressDto) shouldBe
            CstInfoCardInfoMapperTestModel.csobCardInfoAddress
    }

    "Card CST Info map does not throw (delivery address)" {
        assertDoesNotThrow {
            CstInfoCardMapper.mapCardInfoFromDto(CstInfoCardInfoMapperTestModel.csobCardInfoDeliveryAddressFailDto)
        }
    }

    /**
     * Card limit CST tests.
     */
    "Card limits CST mapped correctly" {
        CstInfoCardMapper.mapCardLimitsFromDto(CstInfoCardMapperTestModel.cstLimitDto) shouldBe
            CstInfoCardMapperTestModel.cstCardLimit
    }

    "Card limit CST mapping fails with exception" {
        assertThrows<NullPointerException> {
            CstInfoCardMapper.mapCardLimitsFromDto(
                CstInfoCardMapperTestModel.cstLimitDtoMissingRows
            )
        }
        assertThrows<NullPointerException> {
            CstInfoCardMapper.mapCardLimitsFromDto(
                CstInfoCardMapperTestModel.cstLimitDtoMissingSubQuery
            )
        }
    }

    /**
     * Country code CST tests.
     */
    "Country code CST mapped correctly" {
        CstInfoCardMapper.mapCountryCodeFromDto(
            CstInfoCardMapperTestModel.countryCode.cid,
            CstInfoCardMapperTestModel.cstCountryCodeDto
        ) shouldBe CstInfoCardMapperTestModel.countryCode
    }

    "Country code CST mapped correctly (ignoring invalid cst)" {
        CstInfoCardMapper.mapCountryCodeFromDto(
            CstInfoCardMapperTestModel.countryCode.cid,
            CstInfoCardMapperTestModel.cstCountryCodeDtoWithInvalid
        ) shouldBe CstInfoCardMapperTestModel.countryCode
    }

    "Country code CST mapped correctly but empty" {
        CstInfoCardMapper.mapCountryCodeFromDto(
            CstInfoCardMapperTestModel.countryCode.cid,
            CstInfoCardMapperTestModel.cstCountryCodeDtoEmpty
        ) shouldBe  CstInfoCardMapperTestModel.countryCodeEmpty
    }
})
