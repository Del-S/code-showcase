package cz.sberbank.sbercoins.presentation.feature.achievements.delegate

import android.view.View
import android.widget.Toast
import androidx.navigation.findNavController
import cz.eman.kaal.domain.v2.Result
import cz.sberbank.sbercoins.BR
import cz.sberbank.sbercoins.R
import cz.sberbank.sbercoins.app.SberCoinsApplication
import cz.sberbank.sbercoins.domain.feature.achievements.model.AchievementDomainObject
import cz.sberbank.sbercoins.domain.feature.achievements.model.AchievementsDomainObject
import cz.sberbank.sbercoins.domain.feature.achievements.usecase.GetUserAchievementsUseCase
import cz.sberbank.sbercoins.presentation.core.util.DiffCallback
import cz.sberbank.sbercoins.presentation.core.view.adapter.binder.CompositeItemBinder
import cz.sberbank.sbercoins.presentation.core.view.adapter.binder.ConditionalDataBinder
import cz.sberbank.sbercoins.presentation.core.view.adapter.binder.ItemBinder
import cz.sberbank.sbercoins.presentation.feature.achievements.mapping.toViewObject
import cz.sberbank.sbercoins.presentation.feature.achievements.model.AchievementModel
import cz.sberbank.sbercoins.presentation.feature.achievements.model.AchievementOverviewViewObject
import cz.sberbank.sbercoins.presentation.feature.achievements.model.AchievementViewObject
import cz.sberbank.sbercoins.presentation.feature.achievements.view.AchievementsFragmentDirections
import cz.sberbank.sbercoins.presentation.feature.userprofile.model.UserProfile
import cz.sberbank.sbercoins.presentation.feature.userprofile.viewmodel.UserProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Delegate for getting and holding achievements.
 *
 * @author eMan s.r.o.
 */
class AchievementDelegate(private val getUserAchievements: GetUserAchievementsUseCase) {

    val model = AchievementModel()

    companion object {
        @JvmStatic
        val achievementSimpleBinder: ItemBinder<AchievementViewObject> = CompositeItemBinder(
            object : ConditionalDataBinder<AchievementViewObject>(BR.item, R.layout.item_achievement_simple) {
                override fun canHandle(itemModel: AchievementViewObject) = true
            }
        )
        @JvmStatic
        val achievementDiffer = object : DiffCallback<AchievementOverviewViewObject> {
            override fun areItemsTheSame(
                oldItem: AchievementOverviewViewObject,
                newItem: AchievementOverviewViewObject
            ) = oldItem.name == newItem.name

            override fun areContentsTheSame(
                oldItem: AchievementOverviewViewObject,
                newItem: AchievementOverviewViewObject
            ) = oldItem == newItem
        }

        private val logger: Timber.Tree
            get() = Timber.tag(UserProfileViewModel::class.java.simpleName)
    }

    /**
     * Loads achievements for specific user based on it's user profile.
     *
     * @param profile of the user to get achievements for
     */
    suspend fun loadUserAchievements(profile: UserProfile?) {
        if (model.profile.get() != profile) {
            model.changeProfile(profile)
            val userId = if (profile?.userId?.isEmpty() == true) null else profile?.userId
            when (val result = getUserAchievements(userId)) {
                is Result.Success -> {
                    mapAllAchievements(result.data.achievements)
                    mapAllAchievementsAndSections(result.data)
                }
                is Result.Error -> {
                    // TODO: Error state
                    logger.e(result.toString())
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            SberCoinsApplication.instance,
                            R.string.unexpected_error,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    /**
     * Handles achievement item click. This function only handles [AchievementViewObject] by marking it as selected item
     * and moving to detail screen.
     */
    fun onAchievementClick(view: View, itemModel: AchievementOverviewViewObject) {
        if (itemModel is AchievementViewObject) {
            model.selectedAchievement.set(itemModel)
            view.findNavController().navigate(
                AchievementsFragmentDirections.actionAchievementsScreenToAchievementDetailScreen()
            )
        }
    }

    /**
     * Maps achievements for user profile. Uses [filterUserAchievements] function to get either completed
     * achievements (works for all users) or not completed achievements (works only for currently logged user). It takes
     * only
     */
    private fun mapAllAchievements(data: List<AchievementDomainObject>) {
        model.achievementsCount.set(data.count())
        filterUserAchievements(data).sortedBy { it.completionDate }.apply {
            model.achievements.set(this)
        }
    }

    /**
     * Maps all achievements and sections to be displayed using one recycler view. Achievements are connected to
     * sections by section id. More information can be found in [mapAllAchievementsAndSectionsList].
     */
    private fun mapAllAchievementsAndSections(data: AchievementsDomainObject) {
        mapAllAchievementsAndSectionsList(data).apply {
            model.achievementsAndSections.set(this)
        }
    }

    /**
     * Maps achievements and their sections. Combines the two lists into one by merging the values based on section id.
     * It also orders them by their order.
     */
    private fun mapAllAchievementsAndSectionsList(result: AchievementsDomainObject): List<AchievementOverviewViewObject> =
        result.sections.values
            .mergeTransformAchievements(
                result.achievements,
                { it.toViewObject() },
                { it.toViewObject() },
                { it.order },
                { it.order },
                { section, achievements ->
                    if (model.profile.get() != null) {
                        achievements.filter { it.sectionId == section.sectionId && it.completed }
                    } else {
                        achievements.filter { it.sectionId == section.sectionId }
                    }
                }
            )

    /**
     * Filters achievements for user profile display. Uses an achievement list and tries to get only completed ones. If
     * there are no completed ones then it tries to load not completed ones. These can be loaded only for currently
     * logged user.
     */
    private fun filterUserAchievements(achievements: List<AchievementDomainObject>): List<AchievementViewObject> {
        val completed = achievements.filter { it.completed }.map { it.toViewObject() }
        if (completed.isNotEmpty()) {
            return completed
        }

        if (model.profile.get() != null) {
            return emptyList()
        }

        return achievements.filter { !it.completed }.map { it.toViewObject() }
    }

    /**
     * It merges two Collections together by adding items from the second list after their respective item in a first list.
     * Items from both lists can be transformed and sorted by any value.
     *
     * @param first First collection
     * @param second Second collection
     * @param firstTransform Transform function for elements which are only in [first] collection
     * @param secondTransform Transform function for elements which are only in [second] collection
     * @param firstSort Sort function for elements which are only in [first] collection
     * @param secondSort Sort function for elements which are only in [second] collection
     * @param filter Filter function connecting items from [second] collection to items from the [first] collection
     * @param allowEmpty allows adding value from [first] collection even if there are no items from [second] collection
     *
     * @param T1 Type of first collection value
     * @param T2 Type of second collection value
     * @param R1 Type of first collection value after sorting
     * @param R2 Type of second collection value after sorting
     * @param R Type of result value
     *
     * @return List of merged elements with type [R]
     */
    private inline fun <T1, T2, R1 : Comparable<R1>, R2 : Comparable<R2>, R> mergeTransform(
        first: Collection<T1>,
        second: Collection<T2>,
        firstTransform: (T1) -> R,
        secondTransform: (T2) -> R,
        crossinline firstSort: (T1) -> R1?,
        crossinline secondSort: (T2) -> R2?,
        filter: (T1, Collection<T2>) -> List<T2>,
        allowEmpty: Boolean = false
    ): List<R> {
        val result = ArrayList<R>()
        first.sortedBy(firstSort)
            .forEach {
                val secondList = filter(it, second).sortedBy(secondSort).map(secondTransform)
                if (allowEmpty || secondList.isNotEmpty()) {
                    result.add(firstTransform(it))
                    result.addAll(secondList)
                }
            }
        return result
    }

    /**
     * @see mergeTransform
     */
    private inline fun <T1, T2, R1 : Comparable<R1>, R2 : Comparable<R2>, R> Collection<T1>.mergeTransformAchievements(
        second: Collection<T2>,
        firstTransform: (T1) -> R,
        secondTransform: (T2) -> R,
        crossinline firstSort: (T1) -> R1?,
        crossinline secondSort: (T2) -> R2?,
        filter: (T1, Collection<T2>) -> List<T2>
    ): List<R> = mergeTransform(this, second, firstTransform, secondTransform, firstSort, secondSort, filter, false)
}
