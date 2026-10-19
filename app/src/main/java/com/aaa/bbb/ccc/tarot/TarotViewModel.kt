package com.aaa.bbb.ccc.tarot

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class TarotViewModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {
    val preferences = TarotPreferences(app)
    val repository = TarotRepository(app)
    private val gson = Gson()
    private val changed = MutableStateFlow(0)
    val updates = changed.asStateFlow()
    var cards: List<TarotCard> = emptyList(); private set
    var history: List<ReadingEntity> = emptyList(); private set
    var error: String? = null; private set
    var busy = false; private set
    var screen: String
        get() = saved["screen"] ?: "home"
        set(value) { saved["screen"] = value; refresh() }
    var category: String
        get() = saved["category"] ?: "Major Arcana"
        set(value) { saved["category"] = value; refresh() }
    var cardGrid: Boolean
        get() = saved["card_grid"] ?: false
        set(value) { saved["card_grid"] = value; refresh() }
    var classic: Boolean
        get() = saved["classic"] ?: false
        set(value) { saved["classic"] = value; refresh() }
    var activeSpread: Spread
        get() = Spread.valueOf(saved["spread"] ?: Spread.DAILY.name)
        set(value) { saved["spread"] = value.name }
    var deck: List<DrawnCard>
        get() = saved.get<String>("deck")?.let { gson.fromJson(it, Array<DrawnCard>::class.java).toList() } ?: emptyList()
        private set(value) { saved["deck"] = gson.toJson(value) }
    var selected: List<DrawnCard>
        get() = saved.get<String>("selected")?.let { gson.fromJson(it, Array<DrawnCard>::class.java).toList() } ?: emptyList()
        private set(value) { saved["selected"] = gson.toJson(value) }
    var reading: ReadingSnapshot?
        get() = saved.get<String>("reading")?.let { gson.fromJson(it, ReadingSnapshot::class.java) }
        private set(value) { saved["reading"] = value?.let(gson::toJson) }
    var detailSlug: String
        get() = saved["detail_slug"] ?: "the-fool"
        set(value) { saved["detail_slug"] = value }
    var resultOrigin: String
        get() = saved["result_origin"] ?: "home"
        private set(value) { saved["result_origin"] = value }

    init {
        load()
        viewModelScope.launch {
            repository.dao.observe().collect { history = it; refresh() }
        }
    }

    fun load() {
        viewModelScope.launch {
            error = null
            try { cards = repository.loadCards() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "Unable to load cards" }
            refresh()
        }
    }

    fun restoreScreen(screen: String) {
        saved["screen"] = screen
    }

    fun navigationState(): Bundle = Bundle().apply {
        listOf("screen", "category", "spread", "deck", "selected", "reading", "detail_slug", "result_origin").forEach { key ->
            saved.get<String>(key)?.let { putString(key, it) }
        }
        listOf("card_grid", "classic").forEach { key ->
            saved.get<Boolean>(key)?.let { putBoolean(key, it) }
        }
    }

    fun refresh() { changed.value += 1 }

    fun request(spread: Spread, guidance: () -> Unit, locked: (Long) -> Unit) {
        if (busy || cards.isEmpty()) return
        busy = true
        viewModelScope.launch {
            try {
                val latest = repository.dao.latest(spread.name)
                val now = System.currentTimeMillis()
                val remaining = TarotTestConfig.remaining(spread, latest?.createdAt, now)
                activeSpread = spread
                resultOrigin = screen.takeIf { it == "home" || it == "spreads" } ?: "home"
                when {
                    latest != null && TarotTestConfig.reuseDailyReading(spread, latest.createdAt, now) -> open(latest)
                    remaining > 0 -> locked(remaining)
                    else -> {
                        activeSpread = spread
                        resultOrigin = screen.takeIf { it == "home" || it == "spreads" } ?: "home"
                        if (preferences.showGuidance) guidance() else begin(spread)
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message; refresh() }
            finally { busy = false }
        }
    }

    fun begin(spread: Spread) {
        activeSpread = spread
        deck = TarotRules.deck(cards, preferences.reverseChance)
        selected = emptyList()
        reading = null
        screen = "select"
    }

    fun select(slug: String) {
        if (busy || screen != "select" || selected.size >= activeSpread.cardCount || selected.any { it.slug == slug }) return
        val draw = deck.firstOrNull { it.slug == slug } ?: return
        selected = selected + draw
        refresh()
    }

    fun complete() {
        if (busy || selected.size != activeSpread.cardCount) return
        busy = true
        refresh()
        viewModelScope.launch {
            try {
                // Recheck the daily rule at commit time, except in explicit tester mode.
                val latest = repository.dao.latest(activeSpread.name)
                val now = System.currentTimeMillis()
                if (latest != null && TarotTestConfig.reuseDailyReading(activeSpread, latest.createdAt, now)) {
                    open(latest)
                } else {
                    val interpreted = selected.mapIndexed { index, draw ->
                        TarotRules.interpret(cards.first { it.slug == draw.slug }, activeSpread, index, draw.reversed)
                    }
                    val combined = if (activeSpread == Spread.UNIVERSE) {
                        TarotInterpretations.universeMessage(
                            first = cards.first { it.slug == selected[0].slug }, firstReversed = selected[0].reversed,
                            second = cards.first { it.slug == selected[1].slug }, secondReversed = selected[1].reversed)
                    } else null
                    val snapshot = ReadingSnapshot(UUID.randomUUID().toString(), activeSpread.name, now, interpreted, combined)
                    repository.save(snapshot)
                    reading = snapshot
                    screen = "result"
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "Unable to save reading. Please try again." }
            finally { busy = false; refresh() }
        }
    }

    fun open(entity: ReadingEntity) {
        resultOrigin = screen.takeIf { it in listOf("home", "spreads", "history") } ?: "home"
        reading = repository.snapshot(entity)
        screen = "result"
    }

    fun favorite(entity: ReadingEntity) {
        viewModelScope.launch { repository.dao.favorite(entity.id, !entity.favorite) }
    }

    fun back() {
        screen = when (screen) {
            "result", "select" -> resultOrigin
            "detail" -> "cards"
            else -> "home"
        }
    }
}
