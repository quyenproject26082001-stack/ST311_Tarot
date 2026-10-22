package com.tarot.card.meaning.tarot

import android.view.ViewGroup
import android.widget.*
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tarot.card.meaning.R
import com.tarot.card.meaning.core.extensions.checkInternet
import com.tarot.card.meaning.databinding.*
import com.bumptech.glide.Glide
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TarotSelectionActivity : TarotActivity() {
    override val activityScreen = "select"

    private var selectionBinding: ActivityTarotSelectionBinding? = null
    private var autoCompleteJob: Job? = null
    private var autoCompleteKey: String? = null

    override fun updateScreen(): Boolean {
        if (selectionBinding == null) return false
        updateSelection()
        return true
    }

    override fun renderScreen() {
        renderSelection()
    }

    private fun renderSelection() {
        val form = ActivityTarotSelectionBinding.inflate(layoutInflater, binding.screenHost, true)
        selectionBinding = form
        if (ui.dark) {
            val surface = android.graphics.Color.parseColor("#111212")
            form.deck.setBackgroundColor(surface)
            form.selectionFooter.setBackgroundColor(surface)
        }
        form.selectDescription.text = resources.getQuantityString(R.plurals.tarot_select_cards, vm.activeSpread.cardCount, vm.activeSpread.cardCount)
        listOf(false to form.scrollMode, true to form.classicMode).forEach { (classic, tab) ->
            tab.background = if (classic == vm.classic) ui.shape(if (ui.dark) android.graphics.Color.parseColor("#111212") else ui.paper, 12) else null
            tab.setTextColor(if (classic == vm.classic) ui.gold else ui.muted)
            tab.setOnClickListener {
                if (vm.classic != classic) {
                    cancelAutoComplete()
                    selectionBinding = null
                    vm.classic = classic
                }
            }
        }
        val columns = if (vm.classic) 13 else 6
        form.deck.layoutManager = GridLayoutManager(this, columns)
        form.deck.setHasFixedSize(true)
        form.deck.itemAnimator = null
        form.deck.doOnLayout {
            form.deck.post {
                if (selectionBinding === form) {
                    form.deck.adapter = DeckAdapter(columns, form.deck.width, vm.deck, vm.cards)
                    updateSelection()
                }
            }
        }
        updateSelection()
    }

    private fun updateSelection() {
        val form = selectionBinding ?: return
        val selected = vm.selected
        form.selectedCount.text = vm.error ?: getString(R.string.tarot_selected_count, selected.size, vm.activeSpread.cardCount)
        (form.deck.adapter as? DeckAdapter)?.updateSelected(selected)
        updateAutoComplete(selected)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) updateSelection() else cancelAutoComplete()
    }

    override fun onPause() {
        cancelAutoComplete()
        super.onPause()
    }

    override fun onDestroy() {
        cancelAutoComplete()
        selectionBinding = null
        super.onDestroy()
    }

    private fun updateAutoComplete(selected: List<DrawnCard>) {
        val key = selectionKey(selected)
        if (!canComplete(key)) {
            cancelAutoComplete()
            return
        }
        if (autoCompleteJob?.isActive == true && autoCompleteKey == key) return
        cancelAutoComplete()
        autoCompleteKey = key
        autoCompleteJob = lifecycleScope.launch {
            delay(AUTO_COMPLETE_DELAY_MILLIS)
            if (!canComplete(key)) return@launch
            checkInternet {
                if (canComplete(key)) vm.complete()
            }
        }
    }

    private fun canComplete(key: String): Boolean =
        key.isNotEmpty() &&
            !isFinishing &&
            !isDestroyed &&
            hasWindowFocus() &&
            lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
            vm.screen == activityScreen &&
            !vm.busy &&
            vm.selected.size == vm.activeSpread.cardCount &&
            selectionKey(vm.selected) == key

    private fun selectionKey(selected: List<DrawnCard>): String =
        if (selected.size != vm.activeSpread.cardCount) ""
        else vm.activeSpread.name + ":" + selected.joinToString(",") { it.slug }

    private fun cancelAutoComplete() {
        autoCompleteJob?.cancel()
        autoCompleteJob = null
        autoCompleteKey = null
    }

    private inner class DeckAdapter(
        columns: Int,
        width: Int,
        private val draws: List<DrawnCard>,
        cards: List<TarotCard>
    ) : RecyclerView.Adapter<DeckHolder>() {
        private val cardsBySlug = cards.associateBy { it.slug }
        private var selectedSlugs = emptySet<String>()
        private val inset = if (columns == 6) ui.dp(3) else ui.dp(1)
        private val gap = ui.dp(2)
        private val rowGap = if (columns == 6) ui.dp(5) else gap
        private val usableWidth = (width - ui.dp(16) - ui.dp(14)).coerceAtLeast(ui.dp(100))
        private val faceWidth = (usableWidth / columns - inset - gap).coerceAtLeast(1)
        private val faceHeight = (faceWidth * 1.74f).toInt()

        fun updateSelected(selected: List<DrawnCard>) {
            val updatedSlugs = selected.map { it.slug }.toSet()
            if (selectedSlugs == updatedSlugs) return
            val changedSlugs = (selectedSlugs - updatedSlugs) + (updatedSlugs - selectedSlugs)
            selectedSlugs = updatedSlugs
            draws.forEachIndexed { index, draw ->
                if (draw.slug in changedSlugs) notifyItemChanged(index, true)
            }
        }

        override fun getItemCount() = draws.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckHolder {
            val image = layoutInflater.inflate(R.layout.item_tarot_deck, parent, false) as ImageView
            image.setPadding(inset, 0, gap, rowGap)
            image.layoutParams.height = faceHeight + rowGap
            val holder = DeckHolder(image)
            image.setOnClickListener {
                val position = holder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) vm.select(draws[position].slug)
            }
            return holder
        }

        override fun onBindViewHolder(holder: DeckHolder, position: Int, payloads: MutableList<Any>) {
            if (payloads.isEmpty() || !TarotTestConfig.REVEAL_ALL_CARDS) {
                onBindViewHolder(holder, position)
            } else {
                bindSelection(holder.image, draws[position])
            }
        }

        override fun onBindViewHolder(holder: DeckHolder, position: Int) {
            val draw = draws[position]
            val image = holder.image
            val picked = draw.slug in selectedSlugs
            val showFace = TarotTestConfig.showFace(picked)
            image.rotation = if (showFace && draw.reversed) 180f else 0f
            bindSelection(image, draw)
            if (showFace) {
                val card = cardsBySlug.getValue(draw.slug)
                Glide.with(image).load(card.imageUrl)
                    .override(faceWidth, faceHeight)
                    .dontAnimate()
                    .placeholder(R.drawable.tarot_deck_back)
                    .error(R.drawable.tarot_deck_back)
                    .into(image)
                val title = ui.translations.text(card.title)
                image.contentDescription = if (draw.reversed) getString(R.string.tarot_card_reversed, title) else title
            } else {
                Glide.with(image).clear(image)
                image.setImageResource(R.drawable.tarot_deck_back)
                image.contentDescription = getString(R.string.tarot_card_back, position + 1)
            }
        }

        private fun bindSelection(image: ImageView, draw: DrawnCard) {
            val picked = draw.slug in selectedSlugs
            image.isSelected = picked
            image.alpha = if (TarotTestConfig.REVEAL_ALL_CARDS && picked) .55f else 1f
        }
    }
    private class DeckHolder(val image: ImageView) : RecyclerView.ViewHolder(image)

    private companion object {
        const val AUTO_COMPLETE_DELAY_MILLIS = 1_000L
    }
}
