package cz.csob.smartbanking.cstinfo.infrastructure.mapper.model

import cz.csob.smartbanking.codebase.api.cstinfo.model.CstInfoResDto
import cz.csob.smartbanking.codebase.api.cstinfo.model.CstInfoRowsDto
import cz.csob.smartbanking.cstinfo.domain.model.cards.CardInfoRequest
import cz.csob.smartbanking.cstinfo.domain.model.cards.CardInfoResponse
import cz.csob.smartbanking.cstinfo.infrastructure.mapper.CstInfoCardMapper
import cz.csob.smartbanking.cstinfo.infrastructure.mapper.CstInfoMapper

object CstInfoCardInfoMapperTestModel {

    private const val STATE_CS = "Česká republika"
    private const val STATE_EN = "Czech republic"
    private const val CARD_TYPE = "Debit MC ctls"
    private const val CARD_TYPE_CID = 377L
    private const val CITY = "Mechov"
    private const val ZIPCODE = "15000"
    private const val ADDRESS = "U Čapího hnízda 10"
    private const val COUNTRY_CID = 61L // Czech
    private const val NATIVE_NAME = "Generální ředitel sperma banky"
    private const val NATIVE_NAME_EN = "Chief Executive Officer sperm bank"

    /**
     * Call params (will be send to the api).
     */
    val paramsForMethod = CardInfoRequest(
        cardUnimaskedNumber = "1234 **** **** 5678",
        branchId = "2",
        cardTypeCid = 1L,
        countryCid = 2L
    )

    val paramsForAddress = CardInfoRequest(
        cardUnimaskedNumber = "5678 **** **** 1234",
        branchId = null,
        cardTypeCid = 10L,
        countryCid = 20L
    )

    private val cardInfoMethodCstMap = mapOf(
        CstInfoMapper.ROW_KEY_DATA_ID to 2.0,
        CstInfoMapper.ROW_KEY_NATIVE_NAME to NATIVE_NAME,
        CstInfoMapper.ROW_KEY_NAME_EN to NATIVE_NAME_EN,
        CstInfoMapper.ROW_KEY_STREET to ADDRESS,
        CstInfoMapper.ROW_KEY_CITY to CITY,
        CstInfoMapper.ROW_KEY_PSC to ZIPCODE,
    )

    private val cardInfoAddressCstMap = mapOf(
        CstInfoMapper.ROW_KEY_DATA_ID to COUNTRY_CID.toDouble(),
        CstInfoMapper.ROW_KEY_DESC_CS to STATE_CS,
        CstInfoMapper.ROW_KEY_DESC_EN to STATE_EN
    )

    private val csobBranch = CstInfoRowsDto(rows = listOf(cardInfoMethodCstMap))

    private val csobCountry = CstInfoRowsDto(rows = listOf(cardInfoAddressCstMap))

    private val cardTypeCstMap = mapOf(
        CstInfoMapper.ROW_KEY_DATA_ID to CARD_TYPE_CID.toDouble(),
        CstInfoMapper.ROW_KEY_DESC_CS to CARD_TYPE,
        CstInfoMapper.ROW_KEY_DESC_EN to CARD_TYPE
    )

    private val csobCardType = CstInfoRowsDto(rows = listOf(cardTypeCstMap))

    val csobCardInfoDeliveryMethodDto: CstInfoResDto = mapOf(
        CstInfoCardMapper.SUB_QUERY_CARD_BRANCH to csobBranch,
        CstInfoCardMapper.SUB_QUERY_CARD_TYPE to csobCardType
    )

    val csobCardInfoDeliveryAddressDto: CstInfoResDto = mapOf(
        CstInfoCardMapper.SUB_QUERY_CARD_COUNTRY to csobCountry,
        CstInfoCardMapper.SUB_QUERY_CARD_TYPE to csobCardType
    )

    val csobCardInfoDeliveryAddressFailDto: CstInfoResDto = mapOf(
        CstInfoCardMapper.SUB_QUERY_CARD_COUNTRY to csobCountry
    )

    val csobCardInfoMethod = CardInfoResponse(
        branchId = 2,
        nativeName = NATIVE_NAME,
        nameEn = NATIVE_NAME_EN,
        street = ADDRESS,
        city = CITY,
        psc = ZIPCODE,
        cardTypeId = CARD_TYPE_CID,
        descriptionState = "",
        descriptionCardEn = CARD_TYPE,
        descriptionStateEn = "",
        descriptionCard = CARD_TYPE
    )

    val csobCardInfoAddress = CardInfoResponse(
        branchId = COUNTRY_CID,
        cardTypeId = CARD_TYPE_CID,
        descriptionState = STATE_CS,
        descriptionStateEn = STATE_EN,
        descriptionCard = CARD_TYPE,
        descriptionCardEn = CARD_TYPE
    )
}

