package cz.csob.smartbanking.cstinfo.infrastructure.source.part

import cz.csob.smartbanking.codebase.api.cstinfo.CstInfoApi
import cz.csob.smartbanking.codebase.api.cstinfo.model.CstInfoMaReqDto
import cz.csob.smartbanking.codebase.api.cstinfo.model.CstInfoResDto
import cz.csob.smartbanking.core.domain.language.usecase.GetLanguageInfoUseCase
import cz.csob.smartbanking.core.infrastructure.api.ApiConstants
import cz.csob.smartbanking.core.infrastructure.api.CsobResultCaller
import cz.eman.kaal.domain.result.Result

/**
 * Base implementation for Cst info remote data source.
 *
 * @author eMan a.s.
 */
open class BaseCstInfoRemoteDataSourceImpl(
    protected val cstInfoApi: CstInfoApi,
    protected val getLanguageInfo: GetLanguageInfoUseCase,
    protected val csobResultCaller: CsobResultCaller
) {

    /**
     * Fetches CST value using [cstInfoApi] and specified params of this function. Use [map]
     * function to map api result [CstInfoResDto] to specific CST domain object.
     *
     * @param query under which CST information are saved
     * @param params to modify cst call (specific CIDs and other)
     * @param map function to map Dto to T (Do)
     * @return [Result] success with [T] or error
     * @see CsobResultCaller.callResult
     * @see CstInfoApi.getCstInfoMa
     */
    protected suspend fun <T> fetchCst(
        query: String,
        params: ArrayList<Pair<String, String>>? = null,
        map: suspend (CstInfoResDto) -> T
    ): Result<T> {
        return csobResultCaller.callResult(
            responseCall = {
                cstInfoApi.getCstInfoMa(
                    cstInfoMaReqDto = CstInfoMaReqDto(
                        applicationNames = ApiConstants.APPLICATION_NAME,
                        name = query,
                        params = params
                    )
                )
            },
            errorMessage = { "Cannot fetch $query cst data" },
            map = map
        )
    }

    /**
     * Maps list of cids into one string to be used for loading multiple CST values at once. It also
     * filters duplicate values. Example list of "1 2 2 3" is converted into string "1,2,3".
     *
     * @param list to be converted
     * @return [String] or null when list is empty
     */
    protected fun mapCidList(list: List<Long>): String? {
        if (list.isEmpty()) {
            return null
        }

        return list.distinct().joinToString(",")
    }
}
