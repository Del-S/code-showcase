package cz.csob.smartbanking.cstinfo.infrastructure.source.part

import cz.csob.smartbanking.codebase.api.cstinfo.CstInfoApi
import cz.csob.smartbanking.core.domain.language.model.Language
import cz.csob.smartbanking.core.domain.language.usecase.GetLanguageInfoUseCase
import cz.csob.smartbanking.core.infrastructure.api.CsobResultCaller
import cz.csob.smartbanking.cstinfo.data.source.part.CstInfoCardRemoteDataSource
import cz.csob.smartbanking.cstinfo.domain.model.CountryCode
import cz.csob.smartbanking.cstinfo.domain.model.cards.CardInfoRequest
import cz.csob.smartbanking.cstinfo.domain.model.cards.CardInfoResponse
import cz.csob.smartbanking.cstinfo.domain.model.cards.CardLimits
import cz.csob.smartbanking.cstinfo.domain.model.cards.CardsCstRequest
import cz.csob.smartbanking.cstinfo.domain.model.cards.CardsCstResponse
import cz.csob.smartbanking.cstinfo.infrastructure.mapper.CstInfoCardMapper
import cz.csob.smartbanking.cstinfo.infrastructure.mapper.CstInfoMapper.Companion.SUB_QUERY_COUNTRIES_CZ
import cz.csob.smartbanking.cstinfo.infrastructure.mapper.CstInfoMapper.Companion.SUB_QUERY_COUNTRIES_EN
import cz.eman.kaal.domain.result.ErrorResult
import cz.eman.kaal.domain.result.Result
import cz.eman.kaal.domain.result.mapResult
import cz.eman.kaal.domain.result.toSuccessIfNotNull
import cz.eman.logger.logVerbose

/**
 * Implementation of cst info remote data source for payment cards and related information.
 *
 * @author eMan a.s.
 */
open class CstInfoCardRemoteDataSourceImpl(
    cstInfoApi: CstInfoApi,
    getLanguageInfo: GetLanguageInfoUseCase,
    csobResultCaller: CsobResultCaller
) : CstInfoCommonRemoteDataSourceImpl(cstInfoApi, getLanguageInfo, csobResultCaller),
    CstInfoCardRemoteDataSource {

    /**
     * Fetches [CardsCstResponse] which contains payment card type information (CST 683) and card
     * logical status information (CST 687).
     *
     * @param params contains cid lists for which CST should be loaded
     * @return [Result] success with [CardsCstResponse] or error
     * @see fetchCst
     * @see CstInfoCardMapper.mapCardCstFromDto
     */
    override suspend fun fetchCardCst(params: CardsCstRequest): Result<CardsCstResponse> {
        logVerbose { "fetchCardCst(params = $params)" }
        val language = getLanguageInfo().language
        val query = when (language) {
            Language.CZECH -> QUERY_CARD_CZ
            Language.ENGLISH -> QUERY_CARD_EN
        }
        val apiParams: ArrayList<Pair<String, String>> = arrayListOf()
        mapCidList(params.cardTypeCidList)?.let { apiParams.add("CardTypeCid" to it) }
        mapCidList(params.logicalStatusCidList)?.let { apiParams.add("LogicalStatusCid" to it) }

        return fetchCst(query, apiParams) {
            CstInfoCardMapper.mapCardCstFromDto(it, params, language)
        }
    }

    /**
     * Fetches [CardInfoResponse] which contains delivery information about cards.
     *
     * @param info for which to load cst information
     * @return [Result] success with[CardInfoResponse] or error
     * @see fetchCst
     * @see CstInfoCardMapper.mapCardInfoFromDto
     */
    override suspend fun fetchCardInfo(
        info: CardInfoRequest
    ): Result<CardInfoResponse> {
        logVerbose { "fetchCardInfo(info = $info)" }
        val apiParams: ArrayList<Pair<String, String>> = arrayListOf(
            ROW_CARD_TYPE_CID to info.cardTypeCid.toString()
        )
        var query = QUERY_CARD_COUNTRY
        info.branchId?.let {
            apiParams.add(ROW_CARD_BRANCH_CID to info.branchId.toString())
            query = QUERY_CARD_BRANCH
        } ?: apiParams.add(
            ROW_CARD_COUNTRY_CID to info.countryCid.toString()
        )

        return fetchCst(query, apiParams) {
            CstInfoCardMapper.mapCardInfoFromDto(it)
        }
    }

    /**
     * Fetches [CardLimits] used in payment cards (CST 683).
     *
     * @return [Result] success with [CardLimits] or error
     * @see fetchCst
     * @see CstInfoCardMapper.mapCardLimitsFromDto
     */
    override suspend fun fetchCardLimits(cid: Long): Result<CardLimits> {
        logVerbose { "fetchCardLimits(cid = $cid)" }
        return fetchCst(QUERY_CARD_DATA, arrayListOf("CardTypeCid" to cid.toString())) {
            CstInfoCardMapper.mapCardLimitsFromDto(it)
        }
    }

    /**
     * Fetches List of [CountryCode] (CST 2).
     *
     * @param cid for which to load cst information
     * @return [Result] success with [CountryCode] or error
     * @see fetchCst
     * @see CstInfoCardMapper.mapCountryCodeFromDto
     */
    override suspend fun fetchCountryCode(cid: Long): Result<CountryCode> {
        logVerbose { "fetchCountryCode(cid = $cid)" }
        return fetchCst(QUERY_COUNTY_CODE, arrayListOf("StateId" to cid.toString())) {
            CstInfoCardMapper.mapCountryCodeFromDto(cid, it)
        }.mapResult {
            it.toSuccessIfNotNull { ErrorResult() }
        }
    }

    /**
     * Fetches Map of codes and related countries and countries (CST 2).
     *
     * @return [Result] success with [Map<String, String>] or error
     * @see fetchCst
     * @see CstInfoCardMapper.mapCountries
     */
    override suspend fun fetchCountries(): Result<Map<String, String>> {
        logVerbose { "fetchCountries()" }
        val language = getLanguageInfo().language
        val query = when (language) {
            Language.CZECH -> SUB_QUERY_COUNTRIES_CZ
            Language.ENGLISH -> SUB_QUERY_COUNTRIES_EN
        }
        return fetchCst(query) {
            CstInfoCardMapper.mapCountries(
                it, language
            )
        }.mapResult {
            it.toSuccessIfNotNull { ErrorResult() }
        }
    }

    companion object {
        private const val QUERY_CARD_DATA = "csob_carddata"
        private const val QUERY_CARD_CZ = "csob_cards_cz"
        private const val QUERY_CARD_EN = "csob_cards_en"
        private const val QUERY_COUNTY_CODE = "csob_country_code"
        private const val QUERY_CARD_BRANCH = "csob_branch"
        private const val QUERY_CARD_COUNTRY = "csob_country_card"
        private const val ROW_CARD_BRANCH_CID = "BranchCid"
        private const val ROW_CARD_COUNTRY_CID = "CountryCid"
        private const val ROW_CARD_TYPE_CID = "CardTypeCid"
    }
}
