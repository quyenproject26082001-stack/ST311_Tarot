package com.aaa.bbb.ccc.tarot

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaa.bbb.ccc.R
import com.aaa.bbb.ccc.core.helper.LanguageHelper
import com.aaa.bbb.ccc.databinding.*
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** XML owns the appearance; this activity binds data and routes user actions. */
open class TarotActivity : AppCompatActivity() {
    private val vm: TarotViewModel by viewModels()
    private lateinit var binding: ActivityTarotBinding
    private lateinit var ui: TarotUi
    private var selectionBinding: FragmentTarotSelectionBinding? = null
    private var renderedScreen = ""
    private var sheet: BottomSheetDialog? = null
    private var adLoading = false
    private var rewardPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(if (TarotPreferences(this).dark) R.style.TarotThemeDark else R.style.TarotThemeLight)
        super.onCreate(savedInstanceState)
        LanguageHelper.setLocale(this)
        if (TarotPreferences(this).profile == null) {
            startActivity(Intent(this, TarotProfileActivity::class.java))
            finish()
            return
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    vm.screen == "home" -> finish()
                    vm.screen == "select" && vm.selected.isNotEmpty() -> AlertDialog.Builder(this@TarotActivity)
                        .setTitle(R.string.tarot_discard_title)
                        .setMessage(R.string.tarot_discard_message)
                        .setPositiveButton(R.string.tarot_leave) { _, _ -> vm.back() }
                        .setNegativeButton(R.string.tarot_keep_selecting, null).show()
                    else -> vm.back()
                }
            }
        })
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.updates.collect {
                    if (vm.screen == "select" && renderedScreen == "select" && selectionBinding != null) updateSelection()
                    else render()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (rewardPending) {
            rewardPending = false
            vm.begin(Spread.UNIVERSE)
        } else if (::binding.isInitialized && vm.screen != "select") render()
    }

    override fun onDestroy() {
        sheet?.dismiss()
        sheet = null
        super.onDestroy()
    }

    private fun render() {
        if (isFinishing || isDestroyed) return
        val oldScroll = if (::binding.isInitialized) binding.root.findViewWithTag<ScrollView>("content_scroll")?.scrollY ?: 0 else 0
        val keepScroll = renderedScreen == vm.screen
        ui = TarotUi(this, vm.preferences.dark)
        selectionBinding = null
        binding = ActivityTarotBinding.inflate(layoutInflater)
        // Keep the phone layout readable on tablets without changing its proportions.
        val pageWidth = minOf(resources.displayMetrics.widthPixels, resources.getDimensionPixelSize(R.dimen.tarot_max_page_width))
        binding.page.layoutParams.width = pageWidth
        (binding.page.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.CENTER_HORIZONTAL
        binding.pageBackground.layoutParams.width = pageWidth
        (binding.pageBackground.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.CENTER_HORIZONTAL
        if (ui.dark) binding.pageBackground.alpha = .06f
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            binding.page.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
        bindHeader()
        val hasTabs = vm.screen in listOf("home", "spreads", "cards", "history")
        binding.bottomTabs.isVisible = hasTabs
        if (hasTabs) bindBottomTabs()

        when {
            vm.cards.isEmpty() -> {
                val content = scrollContent()
                ui.add(content, ui.text(vm.error ?: getString(R.string.tarot_loading)))
                if (vm.error != null) ui.add(content, ui.button(getString(R.string.tarot_retry)) { vm.load() })
            }
            vm.screen == "select" -> renderSelection()
            vm.screen == "cards" -> renderLibrary()
            else -> {
                val content = scrollContent()
                when (vm.screen) {
                    "home" -> renderHome(content)
                    "spreads" -> renderSpreads(content)
                    "history" -> renderHistory(content)
                    "result" -> renderResult(content)
                    "detail" -> renderDetail(content)
                    "settings" -> TarotSettingsScreen(this, vm, ui, onThemeChanged = { recreate() }, onChanged = { render() }, onSheetOpened = { sheet = it }).render(content)
                    "horoscope" -> {
                        ui.heading(content, vm.preferences.profile?.zodiac.orEmpty())
                        ui.add(content, ui.text(getString(R.string.tarot_horoscope_unavailable), color = ui.muted))
                    }
                }
                if (keepScroll) (content.parent as ScrollView).post { (content.parent as ScrollView).scrollTo(0, oldScroll) }
            }
        }
        setContentView(binding.root)
        ViewCompat.requestApplyInsets(binding.root)
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = !ui.dark
            isAppearanceLightNavigationBars = !ui.dark
        }
        renderedScreen = vm.screen
    }

    private fun scrollContent(): LinearLayout = TarotScrollContentBinding.inflate(layoutInflater, binding.screenHost, true).content

    private fun bindHeader() {
        val isHome = vm.screen == "home"
        val bar = binding.actionBar
        bar.root.layoutParams.height = resources.getDimensionPixelSize(if (isHome) R.dimen.tarot_home_header_height else R.dimen.tarot_header_height)
        bar.brand.isVisible = isHome && !ui.dark
        bar.tvCenter.isVisible = !isHome || ui.dark
        bar.tvCenter.text = when (vm.screen) {
            "home" -> getString(R.string.tarot_app_title)
            "spreads" -> getString(R.string.tarot_spreads)
            "cards" -> getString(R.string.tarot_cards)
            "history" -> getString(R.string.tarot_history)
            "settings" -> getString(R.string.tarot_settings)
            "horoscope" -> getString(R.string.tarot_horoscope)
            "detail" -> vm.cards.firstOrNull { it.slug == vm.detailSlug }?.title.orEmpty()
            "select" -> vm.activeSpread.title
            "result" -> vm.reading?.let { Spread.valueOf(it.spread).title }.orEmpty()
            else -> ""
        }
        bar.btnActionBarLeft.isVisible = vm.screen !in listOf("home", "spreads", "cards", "history")
        bar.btnActionBarLeft.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        bar.btnActionBarRight.isVisible = isHome
        bar.btnActionBarRight.contentDescription = getString(R.string.tarot_settings)
        bar.btnActionBarRight.setOnClickListener { vm.screen = "settings" }
        bar.libraryToggle.isVisible = vm.screen == "cards"
        bar.listMode.setOnClickListener { vm.cardGrid = false }
        bar.gridMode.setOnClickListener { vm.cardGrid = true }
        bar.listMode.setBackgroundResource(if (!vm.cardGrid) R.drawable.tarot_bg_white_circle else android.R.color.transparent)
        bar.gridMode.setBackgroundResource(if (vm.cardGrid) R.drawable.tarot_bg_white_circle else android.R.color.transparent)
    }

    private fun bindBottomTabs() {
        listOf("home" to R.string.tarot_home, "spreads" to R.string.tarot_spreads,
            "cards" to R.string.tarot_cards, "history" to R.string.tarot_history).forEach { (screen, label) ->
            val item = ItemTarotTabBinding.inflate(layoutInflater, binding.bottomTabs, false)
            val selected = vm.screen == screen
            item.tabIcon.kind = screen
            item.tabIcon.color = if (selected) ui.gold else ui.muted
            item.tabLabel.setText(label)
            item.tabLabel.setTextColor(if (selected) ui.gold else ui.muted)
            item.selectionGlow.isVisible = selected
            item.root.isSelected = selected
            item.root.setOnClickListener { vm.screen = screen }
            binding.bottomTabs.addView(item.root)
        }
    }

    private fun renderHome(content: LinearLayout) {
        val profile = vm.preferences.profile ?: return
        val home = FragmentTarotHomeBinding.inflate(layoutInflater, content, true)
        home.greeting.text = getString(R.string.tarot_greeting, profile.name)
        home.zodiacName.text = profile.zodiac
        home.zodiacIcon.setImageResource(ui.zodiacResource(profile.zodiac))
        if (ui.dark) home.horoscope.setBackgroundResource(R.drawable.tarot_bg_panel)
        home.horoscope.setOnClickListener { vm.screen = "horoscope" }
        listOf(Spread.DAILY, Spread.WEEKLY, Spread.CELTIC).forEachIndexed { index, spread ->
            val action = ItemTarotActionBinding.inflate(layoutInflater, home.quickActions, false)
            action.actionArt.setImageResource(actionArt(spread))
            action.actionTitle.text = spread.title
            if (!ui.dark) action.actionTitle.setBackgroundColor(Color.parseColor(listOf("#F7FBFB", "#FDF9E5", "#EEE0FB")[index]))
            if (index < 2) (action.root.layoutParams as LinearLayout.LayoutParams).marginEnd = ui.dp(12)
            action.root.setOnClickListener { request(spread) }
            home.quickActions.addView(action.root)
        }
        home.emptyRecent.isVisible = vm.history.isEmpty()
        vm.history.take(3).forEach { readingRow(home.recentReadings, it) }
    }

    private fun actionArt(spread: Spread): Int = when (spread) {
        Spread.DAILY -> R.drawable.tarot_action_daily
        Spread.WEEKLY -> R.drawable.tarot_action_weekly
        else -> R.drawable.tarot_action_celtic
    }

    private fun renderSpreads(content: LinearLayout) {
        val form = FragmentTarotSpreadsBinding.inflate(layoutInflater, content, true)
        val icons = listOf(R.drawable.tarot_spread_icon_0, R.drawable.tarot_spread_icon_1, R.drawable.tarot_spread_icon_2,
            R.drawable.tarot_spread_icon_3, R.drawable.tarot_spread_icon_4, R.drawable.tarot_spread_icon_5, R.drawable.tarot_spread_icon_6, R.drawable.tarot_spread_icon_7)
        val colors = listOf("#FCFBF7", "#FFFBE9", "#F1E7FF", "#E1FBFB", "#FCFBF7", "#FFF6F8", "#FDF6EE", "#FCFBF7")
        val decorations = listOf(R.drawable.tarot_spread_decor_0, R.drawable.tarot_spread_decor_1, R.drawable.tarot_spread_decor_2,
            R.drawable.tarot_spread_decor_3, R.drawable.tarot_spread_decor_4, R.drawable.tarot_spread_decor_5, R.drawable.tarot_spread_decor_6, R.drawable.tarot_spread_decor_7)
        Spread.entries.forEachIndexed { index, spread ->
            val item = ItemTarotSpreadBinding.inflate(layoutInflater, form.spreadItems, false)
            item.root.background = ui.shape(if (ui.dark || index >= colors.size) ui.surface else Color.parseColor(colors[index]), 16, ui.green)
            // Only the first eight spread illustrations appear in the supplied
            // screenshot. A neutral card icon avoids assigning another spread's art.
            item.spreadIcon.setImageResource(icons.getOrNull(index) ?: R.drawable.tarot_ic_cards)
            item.spreadBackground.isVisible = !ui.dark && index < decorations.size
            decorations.getOrNull(index)?.let(item.spreadBackground::setImageResource)
            item.spreadTitle.text = spread.title
            if (ui.dark) item.spreadTitle.setTextColor(ui.ink)
            item.spreadDescription.text = spread.description
            val latest = vm.history.firstOrNull { it.spread == spread.name }
            val remaining = if (spread.rule == ReadingRule.COOLDOWN) TarotRules.remaining(latest?.createdAt, System.currentTimeMillis()) else 0
            item.spreadLock.isVisible = remaining > 0
            item.spreadLock.text = duration(remaining)
            if (remaining > 0) { item.spreadTitle.alpha = .6f; item.spreadIcon.alpha = .6f }
            item.root.setOnClickListener { request(spread) }
            form.spreadItems.addView(item.root)
        }
    }

    private fun request(spread: Spread) {
        vm.request(spread, guidance = { if (!isFinishing && !isDestroyed) guidance(spread) },
            locked = { if (!isFinishing && !isDestroyed) locked(it) })
    }

    private fun guidance(spread: Spread) {
        val form = DialogTarotGuidanceBinding.inflate(layoutInflater)
        val scroll = ScrollView(this).apply { addView(form.root); isVerticalScrollBarEnabled = false }
        val dialog = AlertDialog.Builder(this).setView(scroll).create()
        form.close.setOnClickListener { dialog.dismiss() }
        form.continueReading.setOnClickListener { dialog.dismiss(); vm.begin(spread) }
        form.shuffleArt.setOnClickListener { form.shuffleArt.animate().rotationBy(8f).setDuration(120).withEndAction { form.shuffleArt.animate().rotation(0f).setDuration(120).start() }.start() }
        form.showGuidance.isChecked = vm.preferences.showGuidance
        form.showGuidance.setOnCheckedChangeListener { _, checked -> vm.preferences.showGuidance = checked }
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(minOf(resources.displayMetrics.widthPixels - ui.dp(24), ui.dp(360)), -2)
    }

    private fun renderSelection() {
        val form = FragmentTarotSelectionBinding.inflate(layoutInflater, binding.screenHost, true)
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
            val availableWidth = selectionBinding?.deck?.width?.takeIf { it > 0 } ?: binding.screenHost.width
            image.layoutParams.height = (((availableWidth - ui.dp(32)).coerceAtLeast(ui.dp(100)) / columns) * 1.76f).toInt()
            val picked = vm.selected.any { it.slug == draw.slug }
            Glide.with(image).clear(image)
            image.rotation = if (picked && draw.reversed) 180f else 0f
            if (picked) loadCard(image, vm.cards.first { it.slug == draw.slug }.imageUrl)
            else image.setImageResource(R.drawable.tarot_deck_back)
            image.contentDescription = if (picked) "Selected ${draw.slug}" else getString(R.string.tarot_card_back, position + 1)
            image.setOnClickListener { vm.select(draw.slug) }
        }
    }
    private class DeckHolder(val image: ImageView) : RecyclerView.ViewHolder(image)

    private fun renderResult(content: LinearLayout) {
        val reading = vm.reading ?: return
        val spread = Spread.valueOf(reading.spread)
        if (spread == Spread.CELTIC) celticLayout(content, reading.cards)
        if (spread == Spread.UNIVERSE) {
            val pair = TarotPairBinding.inflate(layoutInflater, content, true)
            listOf(pair.firstCard, pair.secondCard).zip(reading.cards).forEach { (image, card) ->
                loadCard(image, card.imageUrl); image.rotation = if (card.reversed) 180f else 0f
            }
            pair.pairTitle.text = reading.cards.joinToString(" & ") { it.title }
        }
        reading.cards.forEach { card ->
            val item = ItemTarotResultCardBinding.inflate(layoutInflater, content, true)
            item.positionTitle.isVisible = reading.cards.size > 1 && spread != Spread.UNIVERSE
            item.positionTitle.text = card.position
            item.cardImage.isVisible = spread != Spread.UNIVERSE
            if (item.cardImage.isVisible) loadCard(item.cardImage, card.imageUrl)
            item.cardImage.rotation = if (card.reversed) 180f else 0f
            item.cardTitle.text = card.title
            val notice = when {
                card.reversed -> R.string.tarot_reverse_missing
                card.generalMeaning && spread != Spread.UNIVERSE -> R.string.tarot_general_meaning
                else -> null
            }
            item.meaningNotice.isVisible = notice != null
            if (notice != null) item.meaningNotice.setText(notice)
            item.answer.isVisible = card.answer != null
            item.answer.text = card.answer?.let { getString(R.string.tarot_answer, it) }
            item.interpretation.text = card.interpretation
        }
        ui.add(content, ui.button(getString(R.string.tarot_finish)) { vm.screen = vm.resultOrigin })
    }

    /** Placement and card aspect ratios are declared in tarot_celtic_diagram.xml. */
    private fun celticLayout(content: LinearLayout, cards: List<ReadingCard>) {
        val diagram = TarotCelticDiagramBinding.inflate(layoutInflater, content, true)
        bindCelticCards(diagram, cards)
    }

    private fun bindCelticCards(diagram: TarotCelticDiagramBinding, cards: List<ReadingCard>) {
        val slots = listOf(diagram.situation, diagram.challenge, diagram.aspiration, diagram.foundation, diagram.past,
            diagram.future, diagram.internal, diagram.external, diagram.hopes, diagram.outcome)
        slots.zip(cards).forEachIndexed { index, (image, card) ->
            loadCard(image, card.imageUrl)
            image.contentDescription = "${index + 1}. ${card.position}: ${card.title}"
            image.rotation = (if (index == 1) 90f else 0f) + (if (card.reversed) 180f else 0f)
        }
    }

    private fun renderLibrary() {
        val library = FragmentTarotLibraryBinding.inflate(layoutInflater, binding.screenHost, true)
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

    private fun renderDetail(content: LinearLayout) {
        val card = vm.cards.firstOrNull { it.slug == vm.detailSlug } ?: return
        val detail = ItemTarotResultCardBinding.inflate(layoutInflater, content, true)
        loadCard(detail.cardImage, card.imageUrl)
        detail.cardTitle.isVisible = false
        detail.interpretation.text = card.meaning
    }

    private fun renderHistory(content: LinearLayout) {
        if (vm.history.isEmpty()) {
            val empty = TarotEmptyHistoryBinding.inflate(layoutInflater, content, true)
            empty.beginReading.setOnClickListener { vm.screen = "spreads" }
        } else vm.history.forEach { readingRow(content, it, showFavorite = true) }
    }

    private fun readingRow(content: LinearLayout, entity: ReadingEntity, showFavorite: Boolean = false) {
        val reading = vm.repository.snapshot(entity)
        val first = reading.cards.firstOrNull() ?: return
        val row = ItemTarotHistoryBinding.inflate(layoutInflater, content, true)
        row.readingTitle.text = Spread.valueOf(reading.spread).title
        row.readingDate.text = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(reading.createdAt))
        row.readingSummary.text = first.interpretation
        // Miniature spread thumbnails preserve the original card order/orientation.
        if (reading.cards.size == 10) {
            val miniature = TarotCelticDiagramBinding.inflate(layoutInflater, row.thumbnailHost, false)
            bindCelticCards(miniature, reading.cards)
            row.thumbnailHost.addView(miniature.root, FrameLayout.LayoutParams(ui.dp(60), -2, Gravity.CENTER))
        } else {
            reading.cards.take(3).forEachIndexed { index, card ->
                val image = ui.image(card.slug, card.imageUrl, 0).apply { rotation = if (card.reversed) 180f else 0f }
                row.thumbnailHost.addView(image, FrameLayout.LayoutParams(ui.dp(46), -1).apply { leftMargin = ui.dp(index * 18) })
            }
        }
        row.favorite.isVisible = showFavorite
        row.favorite.isSelected = entity.favorite
        row.favorite.kind = if (entity.favorite) "heart_filled" else "heart"
        row.favorite.color = if (entity.favorite) Color.RED else ui.muted
        row.favorite.setOnClickListener { vm.favorite(entity) }
        row.root.setOnClickListener { vm.open(entity) }
    }

    private fun loadCard(image: ImageView, url: String) {
        Glide.with(image).load(url).placeholder(R.drawable.tarot_deck_back).error(R.drawable.tarot_deck_back).into(image)
    }

    private fun locked(remaining: Long) {
        sheet = ui.sheet(getString(R.string.tarot_reading_locked)) { box, dialog ->
            ui.add(box, ui.text(getString(R.string.tarot_locked_description, duration(remaining)), 14f, color = ui.muted), bottom = 20)
            val watch = ui.button(getString(R.string.tarot_watch_ad)) {}
            watch.setOnClickListener {
                if (!adLoading) {
                    watch.text = getString(R.string.tarot_ad_loading); watch.isEnabled = false
                    loadReward { watch.text = getString(R.string.tarot_watch_ad); watch.isEnabled = true; dialog.dismiss() }
                }
            }
            ui.add(box, watch, bottom = 8)
            ui.add(box, ui.text(duration(remaining), 14f, color = ui.gold).apply { gravity = Gravity.CENTER })
        }
    }

    // Existing rewarded-ad behavior is retained; production integration is out of scope.
    private fun loadReward(done: () -> Unit) {
        val debug = applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        val unit = if (debug) "ca-app-pub-3940256099942544/5224354917" else getString(R.string.tarot_rewarded_id)
        if (unit.isBlank()) { Toast.makeText(this, R.string.tarot_ad_unavailable, Toast.LENGTH_SHORT).show(); done(); return }
        adLoading = true
        MobileAds.initialize(this) {
            runOnUiThread {
                if (isDestroyed || isFinishing) { adLoading = false; return@runOnUiThread }
                RewardedAd.load(this, unit, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        adLoading = false
                        if (!isDestroyed && !isFinishing) { Toast.makeText(this@TarotActivity, R.string.tarot_ad_unavailable, Toast.LENGTH_SHORT).show(); done() }
                    }
                    override fun onAdLoaded(ad: RewardedAd) {
                        adLoading = false
                        if (isDestroyed || isFinishing || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) { done(); return }
                        done()
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                if (rewardPending && !isDestroyed && !isFinishing && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                    rewardPending = false; vm.begin(Spread.UNIVERSE)
                                }
                            }
                            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                                Toast.makeText(this@TarotActivity, R.string.tarot_ad_unavailable, Toast.LENGTH_SHORT).show()
                            }
                        }
                        ad.show(this@TarotActivity) { rewardPending = true }
                    }
                })
            }
        }
    }

    private fun duration(milliseconds: Long): String {
        val minutes = (milliseconds + 59_999) / 60_000
        return "${minutes / 60}h ${minutes % 60}m"
    }
}
