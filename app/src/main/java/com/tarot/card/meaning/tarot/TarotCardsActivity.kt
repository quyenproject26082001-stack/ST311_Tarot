package com.tarot.card.meaning.tarot

import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tarot.card.meaning.R
import com.tarot.card.meaning.core.extensions.checkInternet
import com.tarot.card.meaning.databinding.*

class TarotCardsActivity : TarotActivity() {
    override val activityScreen = "cards"
    private var libraryBinding: ActivityTarotLibraryBinding? = null
    private var renderedCategory = ""
    private var renderedGrid = false
    private var renderedCards = emptyList<TarotCard>()
    private var pendingLayoutState: Parcelable? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        pendingLayoutState = savedInstanceState?.getParcelable(LIBRARY_LAYOUT_STATE)
        super.onCreate(savedInstanceState)
    }

    override fun updateScreen(): Boolean =
        libraryBinding != null &&
            renderedCategory == vm.category &&
            renderedGrid == vm.cardGrid &&
            renderedCards == visibleCards()

    override fun renderScreen() {
        renderLibrary()
    }

    private fun renderLibrary() {
        val library = ActivityTarotLibraryBinding.inflate(layoutInflater, binding.screenHost, true)
        libraryBinding = library
        renderedCategory = vm.category
        renderedGrid = vm.cardGrid
        val cards = visibleCards()
        renderedCards = cards
        listOf("Major Arcana", "Minor Arcana - Pentacles", "Minor Arcana - Wands", "Minor Arcana - Cups", "Minor Arcana - Swords").forEach { category ->
            val chip = layoutInflater.inflate(R.layout.item_tarot_category, library.categories, false) as TextView
            chip.text = ui.translations.text(category.removePrefix("Minor Arcana - "))
            if (vm.category == category) { chip.setBackgroundResource(R.drawable.tarot_bg_button); chip.setTextColor(Color.WHITE) }
            chip.setOnClickListener { vm.category = category }
            library.categories.addView(chip)
        }
        library.cards.layoutManager = GridLayoutManager(this, if (vm.cardGrid) 3 else 1)
        library.cards.adapter = LibraryAdapter(cards, vm.cardGrid)
        pendingLayoutState?.let { state ->
            library.cards.layoutManager?.onRestoreInstanceState(state)
            pendingLayoutState = null
        }
    }

    private fun visibleCards(): List<TarotCard> = vm.cards.filter { it.category == vm.category }

    override fun onSaveInstanceState(outState: Bundle) {
        libraryBinding?.cards?.layoutManager?.onSaveInstanceState()?.let {
            outState.putParcelable(LIBRARY_LAYOUT_STATE, it)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        libraryBinding = null
        super.onDestroy()
    }

    private inner class LibraryAdapter(private val cards: List<TarotCard>, private val grid: Boolean) : RecyclerView.Adapter<LibraryHolder>() {
        override fun getItemCount() = cards.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryHolder {
            val resource = if (grid) R.layout.item_tarot_library_grid else R.layout.item_tarot_library
            return LibraryHolder(layoutInflater.inflate(resource, parent, false))
        }
        override fun onBindViewHolder(holder: LibraryHolder, position: Int) {
            val card = cards[position]
            loadCard(holder.image, card.imageUrl)
            holder.image.contentDescription = ui.translations.text(card.title)
            holder.itemView.findViewById<TextView>(R.id.cardTitle)?.text = ui.translations.text(card.title)
            holder.itemView.findViewById<TextView>(R.id.cardMeaning)?.text = ui.translations.text(card.meaning)
            holder.itemView.setOnClickListener {
                checkInternet {
                    vm.detailSlug = card.slug
                    vm.screen = "detail"
                }
            }
        }
    }
    private class LibraryHolder(view: View) : RecyclerView.ViewHolder(view) { val image: ImageView = view.findViewById(R.id.cardImage) }

    private companion object {
        const val LIBRARY_LAYOUT_STATE = "library_layout_state"
    }
}
