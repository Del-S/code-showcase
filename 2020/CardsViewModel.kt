package cz.csob.smartbanking.addon.cards.presentation.viewmodel

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.findNavController
import cz.csob.smartbanking.addon.cards.domain.model.CardListReq
import cz.csob.smartbanking.addon.cards.domain.model.CardListRes
import cz.csob.smartbanking.addon.cards.domain.model.CardSort
import cz.csob.smartbanking.addon.cards.domain.usecase.GetCardsListUseCase
import cz.csob.smartbanking.addon.cards.presentation.BR
import cz.csob.smartbanking.addon.cards.presentation.LOG_CARDS
import cz.csob.smartbanking.addon.cards.presentation.R
import cz.csob.smartbanking.addon.cards.presentation.mapper.toCardInformation
import cz.csob.smartbanking.addon.cards.presentation.mapper.toImageDataVo
import cz.csob.smartbanking.addon.cards.presentation.mapper.toVo
import cz.csob.smartbanking.addon.cards.presentation.model.CardAdministrationHolder
import cz.csob.smartbanking.addon.cards.presentation.model.CardDetailSectionVo
import cz.csob.smartbanking.addon.cards.presentation.model.CardInfoSectionVo
import cz.csob.smartbanking.addon.cards.presentation.model.CardInformationLineVo
import cz.csob.smartbanking.addon.cards.presentation.model.CardSettingsLineVo
import cz.csob.smartbanking.addon.cards.presentation.model.CardSettingsSectionVo
import cz.csob.smartbanking.addon.cards.presentation.model.CardVo
import cz.csob.smartbanking.addon.cards.presentation.view.CardsFragmentDirections
import cz.csob.smartbanking.codebase.domain.features.paging.model.Paging
import cz.csob.smartbanking.codebase.presentation.feature.cards.model.PaymentCardImageVo
import cz.csob.smartbanking.codebase.presentation.util.KLiveData
import cz.csob.smartbanking.codebase.presentation.util.KMutableLiveData
import cz.csob.smartbanking.codebase.presentation.util.launch
import cz.csob.smartbanking.codebase.presentation.viewmodel.BaseViewModel
import cz.eman.kaal.domain.result.Result
import cz.eman.kaal.presentation.adapter.binder.CompositeItemBinder
import cz.eman.kaal.presentation.adapter.binder.ConditionalDataBinder
import cz.eman.kaal.presentation.adapter.binder.ItemBinder
import cz.eman.kaal.presentation.adapter.binder.ItemBinderImpl
import cz.eman.kaal.presentation.adapter.binder.VariableBinder
import cz.eman.kaal.presentation.adapter.binder.VariableBinderImpl
import cz.eman.logger.logUserAction
import cz.eman.logger.logVerbose
import cz.eman.logger.logWarn

/**
 * View model to display cards and allow card administration. Does not handle card administration
 * for that is a [CardAdministrationViewModel].
 *
 * @author eMan a.s.
 * @see[BaseViewModel]
 */
class CardsViewModel(
    private val context: Context,
    private val getCardsList: GetCardsListUseCase,
    private val cardAdministrationHolder: CardAdministrationHolder
) : BaseViewModel() {

    private var cardList: List<CardVo> = emptyList()
    private var cardSelected = 0

    private val _initLoadComplete = KMutableLiveData(false)
    private val _noCards = KMutableLiveData(false)
    private val _cardImageDataList = MutableLiveData<List<PaymentCardImageVo>>()
    private val _cardSettings = MutableLiveData<List<CardSettingsLineVo>>()
    private val _cardInformation = MutableLiveData<List<CardInformationLineVo>>()
    private val _cardSelectedDeactivated = KMutableLiveData(true)
    private val _cardSelectedCanBeActivated = KMutableLiveData(false)

    val initLoadComplete: KLiveData<Boolean> = _initLoadComplete
    val noCards: KLiveData<Boolean> = _noCards
    val cardImageDataList: LiveData<List<PaymentCardImageVo>> = _cardImageDataList
    val cardDetailSections = listOf(CardSettingsSectionVo, CardInfoSectionVo)
    val cardSettings: LiveData<List<CardSettingsLineVo>> = _cardSettings
    val cardInformation: LiveData<List<CardInformationLineVo>> = _cardInformation
    val cardSelectedDeactivated: KLiveData<Boolean> = _cardSelectedDeactivated
    val cardSelectedCanBeActivated: KLiveData<Boolean> = _cardSelectedCanBeActivated

    val cardImageDataBinder =
        ItemBinderImpl<PaymentCardImageVo>(BR.cardData, R.layout.view_payment_card)
    val cardDetailSectionsBinder: ItemBinder<CardDetailSectionVo> = CompositeItemBinder(
        object : ConditionalDataBinder<CardDetailSectionVo>(BR.item, R.layout.view_card_info) {
            override fun canHandle(itemModel: CardDetailSectionVo) =
                itemModel is CardInfoSectionVo
        },
        object : ConditionalDataBinder<CardDetailSectionVo>(BR.item, R.layout.view_card_settings) {
            override fun canHandle(itemModel: CardDetailSectionVo) =
                itemModel is CardSettingsSectionVo
        }
    )
    val cardSettingsBinder =
        ItemBinderImpl<CardSettingsLineVo>(BR.item, R.layout.item_card_settings_line)
    val cardInformationBinder =
        ItemBinderImpl<CardInformationLineVo>(BR.item, R.layout.item_card_information_line)

    val cardDetailSectionsVariableBinders: Array<VariableBinder<CardDetailSectionVo>> =
        arrayOf(VariableBinderImpl(BR.viewModel, this))
    val cardSettingsVariableBinders: Array<VariableBinder<CardSettingsLineVo>> =
        arrayOf(VariableBinderImpl(BR.viewModel, this))
    val cardInfoVariableBinders: Array<VariableBinder<CardInformationLineVo>> =
        arrayOf(VariableBinderImpl(BR.viewModel, this))

    val cardSettingsOnClick: Function2<View, CardSettingsLineVo, Unit> =
        { view, item -> triggerSettingsAction(view, item.type) }


    fun loadCardsList() {
        logVerbose { "loadCardsList()" }

        launch {
            when (val result = triggerGetCardsList()) {
                is Result.Success -> mapCards(result.data)
                is Result.Error -> displayErrorDialog(R.string.payment_cards_error_list)
            }
        }
    }

    fun getDetailSectionTitle(position: Int): Int? =
        cardDetailSections.getOrNull(position)?.titleRes

    fun setSelectedCard(position: Int) {
        logUserAction(LOG_CARDS) { "setSelectedCard(position - $position)" }

        val cardSize = cardList.size
        if (position in 0 until cardSize) {
            cardSelected = position
            updateCardInformation()
        }
    }

    fun addCardToGPay(view: View) {
        logUserAction(LOG_CARDS) { "addCardToGPay()" }

        Toast.makeText(view.context, "TODO FRAME 5", Toast.LENGTH_SHORT).show()
    }

    fun activateCard(view: View) {
        logUserAction(LOG_CARDS) { "activateCard()" }

        startCardAdministration(view, CardAdministrationHolder.Type.ACTIVATE)
    }

    fun triggerSettingsAction(
        view: View,
        type: CardSettingsLineVo.Type
    ) {
        logUserAction(LOG_CARDS) { "triggerSettingsAction(view = $view, type: $type)" }

        val administrationType = when (type) {
            CardSettingsLineVo.Type.BLOCK -> CardAdministrationHolder.Type.BLOCK
            CardSettingsLineVo.Type.UNBLOCK -> CardAdministrationHolder.Type.UNBLOCK
            CardSettingsLineVo.Type.PIN -> CardAdministrationHolder.Type.PIN
            CardSettingsLineVo.Type.LIMITS -> {
                startCardLimits(view)
                null
            }
            CardSettingsLineVo.Type.INTERNET -> {
                startCnp(view)
                null
            }
        }

        administrationType?.let {
            startCardAdministration(view, it)
        }
    }

    /**
     * Displays InfoBottomSheet with more information for the user. Each
     * [CardInformationLineVo] item can have it's own info text which is displayed.
     *
     * @param view used to find nav controller
     * @param item used to load info text
     */
    fun showBottomInfo(view: View, item: CardInformationLineVo) {
        logUserAction {
            "showBottomInfo(view = $view, item = $item)"
        }

        if (item.infoTextRes == 0) {
            logWarn { "Item info text is empty -> not showing bottom info." }
            return
        }

        view.findNavController().navigate(
            CardsFragmentDirections.actionToBottomInfo(
                bodyText = item.infoTextRes
            ), null
        )
    }

    private fun startCardLimits(view: View) {
        logVerbose { "startCardLimits(view = $view)" }

        cardList.getOrNull(cardSelected)?.let {
            cardAdministrationHolder.card = it
            view.findNavController().navigate(
                CardsFragmentDirections.actionToCardLimits()
            )
        } ?: displayErrorDialog(R.string.payment_cards_error_no_card_selected)
    }

    private fun startCnp(view: View) {
        logVerbose { "startCnp(view = $view)" }

        cardList.getOrNull(cardSelected)?.let {
            val type = if (it.cardSettings.statusCNPFlag == true) {
                CardAdministrationHolder.Type.DISABLE_CNP
            } else {
                CardAdministrationHolder.Type.ENABLE_CNP
            }

            startCardAdministration(view, type)
        } ?: displayErrorDialog(R.string.payment_cards_error_no_card_selected)
    }

    private fun startCardAdministration(view: View, type: CardAdministrationHolder.Type) {
        logUserAction(LOG_CARDS) { "startCardAdministration(view = $view, type: $type) cards" }

        avoidDoubleClick {
            logVerbose { "startCardAdministration() - processing" }
            cardList.getOrNull(cardSelected)?.let {
                cardAdministrationHolder.card = it
                cardAdministrationHolder.cardAdministrationType = type
                view.findNavController().navigate(
                    CardsFragmentDirections.actionToCardAdministration()
                )
            } ?: displayErrorDialog(R.string.payment_cards_error_no_card_selected)
        }
    }

    private suspend fun triggerGetCardsList() = getCardsList(
        CardListReq(
            sortList = listOf(CardSort()),
            paging = Paging(
                rowsPerPage = 50,
                pageNumber = 1
            ),
            filterList = null
        )
    )

    private fun mapCards(newCardList: CardListRes) {
        logVerbose { "mapCards(newCardList = $newCardList)" }

        cardList =
            newCardList.cardDataList?.map { it.card.toVo(it.businessFunctionList) } ?: emptyList()
        _noCards.value = cardList.isEmpty()
        _cardImageDataList.value = newCardList.cardDataList?.map { it.card.toImageDataVo() }
        updateCardInformation()
        _initLoadComplete.value = true
    }

    private fun updateCardInformation() {
        logVerbose { "updateCardInformation()" }

        val card = cardList.getOrNull(cardSelected)
        val cardDeactivated = card?.deactivated ?: false
        _cardSettings.value = if (!cardDeactivated) {
            card?.let { CardSettingsLineVo.buildCardSettings(it) }
        } else {
            emptyList()
        }
        _cardSelectedCanBeActivated.value = card?.canBeActivated() ?: false
        _cardSelectedDeactivated.value = cardDeactivated
        _cardInformation.value = card?.toCardInformation(context)
    }
}


