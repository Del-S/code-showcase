package cz.csob.smartbanking.cstinfo.infrastructure.mapper

import cz.csob.smartbanking.codebase.api.cstinfo.model.CstInfoResDto
import cz.csob.smartbanking.core.domain.language.model.Language
import cz.csob.smartbanking.cstinfo.domain.model.CountryCode
import cz.csob.smartbanking.cstinfo.domain.model.cards.CardInfoResponse
import cz.csob.smartbanking.cstinfo.domain.model.cards.CardLimits
import cz.csob.smartbanking.cstinfo.domain.model.cards.CardLogicalStatus
import cz.csob.smartbanking.cstinfo.domain.model.cards.CardType
import cz.csob.smartbanking.cstinfo.domain.model.cards.CardsCstRequest
import cz.csob.smartbanking.cstinfo.domain.model.cards.CardsCstResponse
import cz.eman.logger.logDebug
import cz.eman.logger.logVerbose
import cz.eman.logger.logWarn

/**
 * Cst info mapper for payment cards and related information.
 *
 * @author: eMan a.s.
 */
object CstInfoCardMapper : CstInfoMapper() {

    /**
     * Maps [CardsCstResponse] which contains payment card information like description, statuses
     * and other information.
     *
     * @param dto containing CST information
     * @param request used to load cid list to search for cst values with these cids
     * @param language used to decide sub-query for cst
     * @return [CardsCstResponse]
     */
    fun mapCardCstFromDto(dto: CstInfoResDto, request: CardsCstRequest, language: Language) =
        CardsCstResponse(
            cardTypeList = mapCardType(dto, request.cardTypeCidList, language),
            logicalStatusList = mapCardLogicalStatus(dto, request.logicalStatusCidList, language)
        )

    /**
     * Maps [CardInfoResponse] which contains information about cards. Takes first and second row and tries to map
     * it. When rows are missing completely then it will log a warning and throw [NullPointerException] because this function
     * cannot return null.
     *
     * @param dto containing CST information
     * @return [CardInfoResponse]
     * @throws NullPointerException when mapping fails and cst result it null
     */
    fun mapCardInfoFromDto(dto: CstInfoResDto): CardInfoResponse =
        dto.let {
            dto[SUB_QUERY_CARD_BRANCH]?.rows?.firstOrNull()?.let { row ->
                CardInfoResponse(
                    branchId = (row[ROW_KEY_DATA_ID] as Double).toLong(),
                    nativeName = row[ROW_KEY_NATIVE_NAME] as String,
                    nameEn = row[ROW_KEY_NAME_EN] as String,
                    street = row[ROW_KEY_STREET] as String,
                    city = row[ROW_KEY_CITY] as String,
                    psc = row[ROW_KEY_PSC] as String
                )
            } ?: dto[SUB_QUERY_CARD_COUNTRY]?.rows?.firstOrNull()?.let { row ->
                CardInfoResponse(
                    branchId = (row[ROW_KEY_DATA_ID] as Double).toLong(),
                    descriptionState = row[ROW_KEY_DESC_CS] as String,
                    descriptionStateEn = row[ROW_KEY_DESC_EN] as String
                )
            }
        }.also {
            dto[SUB_QUERY_CARD_TYPE]?.rows?.firstOrNull()?.let { row ->
                it?.cardTypeId = (row[ROW_KEY_DATA_ID] as Double).toLong()
                it?.descriptionCard = row[ROW_KEY_DESC_CS] as String
                it?.descriptionCardEn = row[ROW_KEY_DESC_EN] as String
            }
        }!!

    /**
     * Maps [CardLimits] which contains payment card limits. Takes first row and tries to map
     * it. If it cannot it will log a warning and throw [NullPointerException] because this function
     * cannot return null.
     *
     * @param dto containing CST information
     * @return [CardLimits]
     * @throws NullPointerException when mapping fails and cst result it null
     */
    @Throws(NullPointerException::class)
    fun mapCardLimitsFromDto(dto: CstInfoResDto): CardLimits =
        dto[SUB_QUERY_CARD_LIMITS]?.rows?.firstOrNull()?.let { row ->
            try {
                CardLimits(
                    cid = (row[ROW_KEY_DATA_ID] as Double).toLong(),
                    minLimit = row[KEY_LIMIT_MIN] as Double,
                    maxLimit = row[KEY_LIMIT_MAX] as Double,
                    stepLimit = row[KEY_LIMIT_STEP] as Double
                )
            } catch (ex: Exception) {
                logWarn("CST 683 parsing failed", ex)
                logDebug("Row (CardLimitsCst): $row")
                null
            }
        }!!

    /**
     * Maps [CountryCode]. Tries to find value in the list based on cid and maps it to
     * [CountryCode].
     *
     * @param dto containing CST information
     * @return [CountryCode] or null
     * @see findCountryCodeCstValueTypedByQuery
     */
    fun mapCountryCodeFromDto(cid: Long, dto: CstInfoResDto): CountryCode {
        return CountryCode(
            cid,
            countryCode = findCountryCodeCstValueTypedByQuery(dto, SUB_QUERY_COUNTRY_CODE, cid),
            countryCodeAlternative = findCountryCodeCstValueTypedByQuery(
                dto,
                SUB_QUERY_COUNTRY_CODE_ALTERNATIVE,
                cid
            )
        )
    }

    /**
     * to fetch countries code and country name
     *
     * @param dto containing CST information
     * @param language app language
     * @return [Map<String, String>] or emptyMap countries with codes
     */
    fun mapCountries(dto: CstInfoResDto, language: Language): Map<String, String> {
        val key = when (language) {
            Language.ENGLISH -> ROW_KEY_DESC_EN
            Language.CZECH -> ROW_KEY_DESC_CS
        }

        val subQuery = when (language) {
            Language.ENGLISH -> SUB_QUERY_COUNTRIES_EN
            Language.CZECH -> SUB_QUERY_COUNTRIES_CZ
        }
        return dto.get(subQuery)?.rows?.map {
            Pair(it[ROW_KEY_CODE_A2] as String, it[key] as String)
        }?.toMap() ?: emptyMap()
    }

    /**
     * Maps [CardType] from [CstInfoResDto]. Tries to find and map cst values based on [cidList].
     * When row is invalid then it is ignored (warning is logged) but others are still mapped
     * correctly. When rows are missing completely then it returns empty list.
     *
     * @param dto containing cst data
     * @param cidList used to find specific cst data
     * @param language used to decide sub-query (cz/en)
     * @return List of [CardType]
     * @see findCstValueTyped
     */
    private fun mapCardType(
        dto: CstInfoResDto,
        cidList: List<Long>,
        language: Language
    ): List<CardType> {
        val subQuery = when (language) {
            Language.CZECH -> SUB_QUERY_CARD_TYPE_CZ
            Language.ENGLISH -> SUB_QUERY_CARD_TYPE_EN
        }

        logVerbose { "mapCardType()" }
        return cidList.mapNotNull { cid ->
            findCstValueTyped("CST 683", dto, subQuery, cid) { row ->
                val desc = (row[ROW_KEY_DESC_CS] ?: row[ROW_KEY_DESC_CZ]
                ?: row[ROW_KEY_DESC_EN]) as String?
                val eligible = row[KEY_ELIGIBLE_TOKEN_ID]?.let {
                    (it as Double).toLong() == 1L
                } ?: false
                CardType(
                    cid = cid,
                    description = desc,
                    eligibleTokenId = eligible
                )
            }
        }
    }

    /**
     * Maps [CardLogicalStatus] from [CstInfoResDto]. Tries to find and map cst values based on
     * [cidList]. When row is invalid then it is ignored (warning is logged) but others are still
     * mapped correctly. When rows are missing completely then it returns empty list.
     *
     * @param dto containing cst data
     * @param cidList used to find specific cst data
     * @param language used to decide sub-query (cz/en)
     * @return List of [CardLogicalStatus]
     * @see findCstValueTyped
     */
    private fun mapCardLogicalStatus(
        dto: CstInfoResDto,
        cidList: List<Long>,
        language: Language
    ): List<CardLogicalStatus> {
        val subQuery = when (language) {
            Language.CZECH -> SUB_QUERY_CARD_STATUS_CZ
            Language.ENGLISH -> SUB_QUERY_CARD_STATUS_EN
        }

        logVerbose { "mapCardLogicalStatus()" }
        return cidList.mapNotNull { cid ->
            findCstValue("CST 687", dto, subQuery, cid)?.let {
                CardLogicalStatus(
                    cid = cid,
                    description = it
                )
            }
        }
    }

    /**
     * Tries to find values countryCode or countryCodeAlternative in the list
     * based on cid for create instance of [CountryCode].
     * @param dto containing CST information
     * @return countryCode or countryCodeAlternative
     * @see findCstValueTyped
     */
    private fun findCountryCodeCstValueTypedByQuery(
        dto: CstInfoResDto,
        query: String,
        cid: Long
    ) = findCstValueTyped("CST 683", dto, query, cid)
    { row ->
        try {
            row[if (query == SUB_QUERY_COUNTRY_CODE) {
                ROW_KEY_CODE_A2
            } else {
                ROW_KEY_CODE_A3
            }] as String
        } catch (ex: Exception) {
            logWarn("CST 683 parsing failed", ex)
            logDebug("Row (CountryCode): $row")
            null
        }
    } ?: ""

    internal const val SUB_QUERY_CARD_LIMITS = "csob_cardlimits"
    internal const val SUB_QUERY_CARD_TYPE_CZ = "csob_cardtype_cz"
    internal const val SUB_QUERY_CARD_STATUS_CZ = "csob_cardstatus_cz"
    internal const val SUB_QUERY_COUNTRY_CODE = "csob_code_a2"
    internal const val SUB_QUERY_COUNTRY_CODE_ALTERNATIVE = "csob_code_a3"
    internal const val KEY_LIMIT_MIN = "LIMIT_MIN"
    internal const val KEY_LIMIT_MAX = "LIMIT_MAX"
    internal const val KEY_LIMIT_STEP = "LIMIT_STEP"
    internal const val KEY_ELIGIBLE_TOKEN_ID = "ELIGIBLE_TOKENID"
    internal const val ROW_KEY_CODE_A2 = "CODE_A2"
    internal const val ROW_KEY_CODE_A3 = "CODE_A3"
    internal const val SUB_QUERY_CARD_TYPE = "csob_cardtype"
    internal const val SUB_QUERY_CARD_BRANCH = "csob_branch"
    internal const val SUB_QUERY_CARD_COUNTRY = "csob_country"
    private const val SUB_QUERY_CARD_TYPE_EN = "csob_cardtype_en"
    private const val SUB_QUERY_CARD_STATUS_EN = "csob_cardstatus_en"
}
