package com.aaa.bbb.ccc.tarot

import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaa.bbb.ccc.R
import com.aaa.bbb.ccc.databinding.*
import com.bumptech.glide.Glide

class TarotSelectionActivity : TarotActivity() {
    override val activityScreen = "select"

    private var selectionBinding: ActivityTarotSelectionBinding? = null

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
        form.selectDescription.text = resources.getQuantityString(R.plurals.tarot_select_cards, vm.activeSpread.cardCount, vm.activeSpread.cardCount)
        listOf(false to form.scrollMode, true to form.classicMode).forEach { (classic, tab) ->
            tab.background = if (classic == vm.classic) ui.shape(ui.paper, 16) else null
            tab.setTextColor(if (classic == vm.classic) ui.gold else ui.muted)
            tab.setOnClickListener {
                if (vm.classic != classic) { selectionBinding = null; vm.classic = classic }
            }
        }
        val columns = if (vm.classic) 13 else 6
        form.deck.layoutManager = GridLayoutManager(this, columns)
        form.deck.adapter = DeckAdapter(columns)
        form.deck.itemAnimator = null
        form.viewReading.setOnClickListener { vm.complete() }
        updateSelection()
    }

    private fun updateSelection() {
        val form = selectionBinding ?: return
        form.selectedCount.text = vm.error ?: getString(R.string.tarot_selected_count, vm.selected.size, vm.activeSpread.cardCount)
        form.viewReading.isEnabled = vm.selected.size == vm.activeSpread.cardCount && !vm.busy
        form.deck.adapter?.notifyDataSetChanged()
    }

    private inner class DeckAdapter(private val columns: Int) : RecyclerView.Adapter<DeckHolder>() {
        override fun getItemCount() = vm.deck.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = DeckHolder(layoutInflater.inflate(R.layout.item_tarot_deck, parent, false) as ImageView)
        override fun onBindViewHolder(holder: DeckHolder, position: Int) {
            val draw = vm.deck[position]
            val image = holder.image
            val deck = selectionBinding?.deck
            val availableWidth = deck?.width?.takeIf { it > 0 } ?: binding.screenHost.width
            val usableWidth = (availableWidth - (deck?.paddingLeft ?: ui.dp(16)) -
                (deck?.paddingRight ?: ui.dp(14))).coerceAtLeast(ui.dp(100))
            val inset = if (columns == 6) ui.dp(3) else ui.dp(1)
            val gap = ui.dp(2)
            val rowGap = if (columns == 6) ui.dp(5) else gap
            image.setPadding(inset, 0, gap, rowGap)
            val faceWidth = (usableWidth / columns - inset - gap).coerceAtLeast(1)
            image.layoutParams.height = (faceWidth * 1.74f).toInt() + rowGap
            val picked = vm.selected.any { it.slug == draw.slug }
            val showFace = TarotTestConfig.showFace(picked)
            Glide.with(image).clear(image)
            image.rotation = if (showFace && draw.reversed) 180f else 0f
            image.isSelected = picked
            // With every face visible, dim picked cards so testers can still identify them.
            image.alpha = if (TarotTestConfig.REVEAL_ALL_CARDS && picked) .55f else 1f
            if (showFace) {
                val card = vm.cards.first { it.slug == draw.slug }
                loadCard(image, card.imageUrl)
                image.contentDescription = if (draw.reversed) getString(R.string.tarot_card_reversed, card.title) else card.title
            } else {
                image.setImageResource(R.drawable.tarot_deck_back)
                image.contentDescription = getString(R.string.tarot_card_back, position + 1)
            }
            image.setOnClickListener { vm.select(draw.slug) }
        }
    }
    private class DeckHolder(val image: ImageView) : RecyclerView.ViewHolder(image)
}
