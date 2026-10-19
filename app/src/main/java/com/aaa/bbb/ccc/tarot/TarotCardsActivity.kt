package com.aaa.bbb.ccc.tarot

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaa.bbb.ccc.R
import com.aaa.bbb.ccc.databinding.*

class TarotCardsActivity : TarotActivity() {
    override val activityScreen = "cards"

    override fun renderScreen() {
        renderLibrary()
    }

    private fun renderLibrary() {
        val library = ActivityTarotLibraryBinding.inflate(layoutInflater, binding.screenHost, true)
        listOf("Major Arcana", "Minor Arcana - Pentacles", "Minor Arcana - Wands", "Minor Arcana - Cups", "Minor Arcana - Swords").forEach { category ->
            val chip = layoutInflater.inflate(R.layout.item_tarot_category, library.categories, false) as TextView
            chip.text = category.removePrefix("Minor Arcana - ")
            if (vm.category == category) { chip.setBackgroundResource(R.drawable.tarot_bg_button); chip.setTextColor(Color.WHITE) }
            chip.setOnClickListener { vm.category = category }
            library.categories.addView(chip)
        }
        library.cards.layoutManager = GridLayoutManager(this, if (vm.cardGrid) 3 else 1)
        library.cards.adapter = LibraryAdapter(vm.cards.filter { it.category == vm.category }, vm.cardGrid)
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
            holder.image.contentDescription = card.title
            holder.itemView.findViewById<TextView>(R.id.cardTitle)?.text = card.title
            holder.itemView.findViewById<TextView>(R.id.cardMeaning)?.text = card.meaning
            holder.itemView.setOnClickListener { vm.detailSlug = card.slug; vm.screen = "detail" }
        }
    }
    private class LibraryHolder(view: View) : RecyclerView.ViewHolder(view) { val image: ImageView = view.findViewById(R.id.cardImage) }
}
