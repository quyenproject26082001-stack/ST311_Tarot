package com.tarot.card.meaning.tarot

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tarot.card.meaning.R
import com.tarot.card.meaning.core.base.BaseActivity
import com.tarot.card.meaning.core.extensions.checkInternet
import com.tarot.card.meaning.core.extensions.hideNavigation
import com.tarot.card.meaning.core.helper.LanguageHelper
import com.tarot.card.meaning.core.helper.SharePreferenceHelper
import com.tarot.card.meaning.databinding.*
import com.tarot.card.meaning.dialog.YesNoDialog
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
abstract class TarotActivity : BaseActivity<ActivityTarotBinding>() {
    protected val vm: TarotViewModel by viewModels()
    protected lateinit var ui: TarotUi
    private var renderedScreen = ""
    protected var sheet: BottomSheetDialog? = null
    private var adLoading = false
    private var rewardPending: Spread? = null
    private var readingLockedOverlay: TarotReadingLockedOverlay? = null
    private var appliedDark = false
    private var appliedLanguage = ""
    protected abstract val activityScreen: String
    protected abstract fun renderScreen()
    protected open fun updateScreen(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        appliedDark = TarotPreferences(this).dark
        appliedLanguage = SharePreferenceHelper(this).getPreLanguage()
        setTheme(if (appliedDark) R.style.TarotThemeDark else R.style.TarotThemeLight)
        super.onCreate(savedInstanceState)
    }

    override fun setViewBinding() = ActivityTarotBinding.inflate(layoutInflater)

    override fun useDarkStatusBarIcons(): Boolean = !TarotPreferences(this).dark

    override fun initView() {
        vm.restoreScreen(activityScreen)
        if (TarotPreferences(this).profile == null) {
            startActivity(Intent(this, TarotProfileActivity::class.java))
            finish()
            return
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        bindWindowInsets()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    readingLockedOverlay?.isShowing == true -> closeReadingLockedOverlay()
                    vm.screen == "select" && vm.selected.isNotEmpty() -> {
                        val dialog = YesNoDialog(
                            this@TarotActivity,
                            R.string.tarot_discard_title,
                            R.string.tarot_discard_message,
                            noButtonText = R.string.tarot_keep_selecting,
                            yesButtonText = R.string.tarot_leave
                        )
                        dialog.onNoClick = { dialog.dismiss() }
                        dialog.onYesClick = { dialog.dismiss(); finishScreen() }
                        dialog.show()
                        dialog.hideNavigation(useDarkStatusBarIcons())
                    }
                    else -> finishScreen()
                }
            }
        })
    }

    // Each rendered screen binds its own actions and action-bar state.
    override fun viewListener() = Unit
    override fun initActionBar() = Unit

    override fun dataObservable() {
        if (isFinishing) return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.updates.collect {
                    if (vm.screen != activityScreen || renderedScreen != activityScreen || !updateScreen()) render()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() = onBackPressedDispatcher.onBackPressed()

    override fun onResume() {
        super.onResume()
        val appearanceChanged = appliedDark != TarotPreferences(this).dark ||
            appliedLanguage != sharePreference.getPreLanguage()
        if (appearanceChanged) refreshTheme()
        if (rewardPending != null) {
            consumeReward()
        } else if (!appearanceChanged && !isFinishing && (vm.screen != activityScreen || !updateScreen())) render()
    }

    protected fun refreshTheme() {
        val language = sharePreference.getPreLanguage()
        if (isFinishing || isDestroyed ||
            (appliedDark == vm.preferences.dark && appliedLanguage == language)) return
        val viewState = SparseArray<Parcelable>()
        binding.root.saveHierarchyState(viewState)
        closeReadingLockedOverlay()
        sheet?.dismiss()
        sheet = null
        appliedDark = vm.preferences.dark
        appliedLanguage = language
        LanguageHelper.setLocale(this)
        val themeStyle = if (appliedDark) R.style.TarotThemeDark else R.style.TarotThemeLight
        theme.setTo(resources.newTheme().apply { applyStyle(themeStyle, true) })
        setTheme(themeStyle)
        replaceViewBinding(setViewBinding()) {
            bindWindowInsets()
            render()
            binding.root.restoreHierarchyState(viewState)
        }
        window.setBackgroundDrawable(ColorDrawable(ui.paper))
        hideNavigation(useDarkStatusBarIcons())
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun bindWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.statusBars() or
                WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            binding.page.setPadding(0, safe.top, 0, safe.bottom)
            insets
        }
    }

    private fun consumeReward() {
        val spread = rewardPending ?: return
        rewardPending = null
        vm.begin(spread)
    }

    override fun onDestroy() {
        closeReadingLockedOverlay()
        sheet?.dismiss()
        sheet = null
        super.onDestroy()
    }

    private fun render() {
        if (isFinishing || isDestroyed) return
        if (vm.screen != activityScreen) {
            navigateToScreen()
            return
        }
        val oldScroll = binding.root.findViewWithTag<ScrollView>("content_scroll")?.scrollY ?: 0
        val keepScroll = renderedScreen == vm.screen
        ui = TarotUi(this, vm.preferences.dark)
        binding.screenHost.removeAllViews()
        binding.bottomTabs.removeAllViews()
        // Keep the phone layout readable on tablets without changing its proportions.
        val pageWidth = minOf(resources.displayMetrics.widthPixels, resources.getDimensionPixelSize(R.dimen.tarot_max_page_width))
        binding.page.layoutParams.width = pageWidth
        (binding.page.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.CENTER_HORIZONTAL
        binding.bottomTabShadow.layoutParams.width = pageWidth
        (binding.bottomTabShadow.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        binding.pageBackground.layoutParams.width = pageWidth
        (binding.pageBackground.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.CENTER_HORIZONTAL
        binding.pageBackground.applyTarotPageBackground(ui.dark)
        bindHeader()
        val hasTabs = vm.screen in listOf("home", "spreads", "cards", "history")
        binding.bottomTabs.isVisible = hasTabs
        binding.bottomTabShadow.isVisible = hasTabs
        if (hasTabs) bindBottomTabs()

        if (vm.cards.isEmpty()) {
            val content = scrollContent()
            ui.add(content, ui.text(vm.error?.let(ui.translations::text) ?: getString(R.string.tarot_loading)))
            if (vm.error != null) ui.add(content, ui.button(getString(R.string.tarot_retry)) { vm.load() })
        } else {
            renderScreen()
            if (keepScroll) binding.root.findViewWithTag<ScrollView>("content_scroll")?.let { scroll ->
                scroll.post { scroll.scrollTo(0, oldScroll) }
            }
        }
        ViewCompat.requestApplyInsets(binding.root)
        renderedScreen = vm.screen
    }

    protected fun finishScreen() {
        if (activityScreen != "home" && isTaskRoot) {
            startActivity(Intent(this, com.tarot.card.meaning.ui.home.HomeActivity::class.java))
        }
        finish()
    }

    private fun navigateToScreen() {
        val destination = vm.screen
        val state = vm.navigationState()
        vm.restoreScreen(activityScreen)
        if (activityScreen == "result" && destination == vm.resultOrigin) {
            finishScreen()
            return
        }
        val target = when (destination) {
            "home" -> com.tarot.card.meaning.ui.home.HomeActivity::class.java
            "spreads" -> TarotSpreadsActivity::class.java
            "cards" -> TarotCardsActivity::class.java
            "history" -> TarotHistoryActivity::class.java
            "settings" -> TarotSettingsActivity::class.java
            "horoscope" -> TarotHoroscopeActivity::class.java
            "select" -> TarotSelectionActivity::class.java
            "result" -> TarotResultActivity::class.java
            "detail" -> TarotCardDetailActivity::class.java
            else -> return
        }
        closeReadingLockedOverlay()
        sheet?.dismiss()
        sheet = null
        val intent = Intent(this, target).putExtras(state)
        if (destination == "home") {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        val tabs = setOf("home", "spreads", "cards", "history")
        if ((activityScreen != "home" && destination in tabs && activityScreen in tabs) ||
            (activityScreen == "select" && destination == "result")) finish()
    }

    protected fun scrollContent(): LinearLayout = TarotScrollContentBinding.inflate(layoutInflater, binding.screenHost, true).content

    private fun bindHeader() {
        val isHome = vm.screen == "home"
        val bar = binding.actionBar
        bar.brand.isVisible = isHome && !ui.dark && !ui.translations.isLocalized
        bar.brandDark.isVisible = isHome && (ui.dark || ui.translations.isLocalized)
        bar.tvCenter.isVisible = !isHome
        if (ui.dark) bar.tvCenter.setTextColor(ui.ink)
        bar.tvCenter.text = when (vm.screen) {
            "home" -> getString(R.string.tarot_app_title)
            "spreads" -> getString(R.string.tarot_spreads)
            "cards" -> getString(R.string.tarot_cards)
            "history" -> getString(R.string.tarot_history)
            "settings" -> getString(R.string.tarot_settings)
            "horoscope" -> getString(R.string.tarot_horoscope)
            "detail" -> ui.translations.text(vm.cards.firstOrNull { it.slug == vm.detailSlug }?.title.orEmpty())
            "select" -> ui.translations.text(vm.activeSpread.title)
            "result" -> ui.translations.text(vm.reading?.let { Spread.valueOf(it.spread).title }.orEmpty())
            else -> ""
        }
        // Reserve the toggle's width only on Cards; long spread names retain the design's space.
        val titleInset = resources.getDimensionPixelSize(if (vm.screen == "cards")
            R.dimen.tarot_compact_title_margin else R.dimen.tarot_wide_title_margin)
        (bar.tvCenter.layoutParams as ViewGroup.MarginLayoutParams).apply {
            marginStart = titleInset
            marginEnd = titleInset
        }.also { bar.tvCenter.layoutParams = it }
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
        bar.listMode.color = if (ui.dark || vm.cardGrid) Color.WHITE else ui.ink
        bar.gridMode.color = if (ui.dark || !vm.cardGrid) Color.WHITE else ui.ink
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
            item.root.setOnClickListener {
                if (!selected) {
                    if (screen == "cards") checkInternet { vm.screen = screen }
                    else vm.screen = screen
                }
            }
            binding.bottomTabs.addView(item.root)
        }
    }

    protected fun request(spread: Spread) {
        checkInternet {
            vm.request(spread, guidance = { if (!isFinishing && !isDestroyed) guidance(spread) },
                locked = { if (!isFinishing && !isDestroyed) locked(it) })
        }
    }

    private fun guidance(spread: Spread) {
        val form = DialogTarotGuidanceBinding.inflate(layoutInflater)
        val scroll = ScrollView(this).apply { addView(form.root); isVerticalScrollBarEnabled = false }
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(scroll)
        sheet = dialog
        form.close.setOnClickListener { dialog.dismiss() }
        form.continueReading.setOnClickListener {
            checkInternet {
                dialog.dismiss()
                vm.begin(spread)
            }
        }
        form.shuffleArt.setOnClickListener { form.shuffleArt.animate().rotationBy(8f).setDuration(120).withEndAction { form.shuffleArt.animate().rotation(0f).setDuration(120).start() }.start() }
        form.showGuidance.isChecked = vm.preferences.showGuidance
        form.showGuidance.setOnCheckedChangeListener { _, checked -> vm.preferences.showGuidance = checked }
        dialog.setOnShowListener {
            (scroll.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)
            dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            dialog.behavior.skipCollapsed = true
            dialog.behavior.isGestureInsetBottomIgnored = true
            ui.fitSheetToHiddenNavigation(dialog)
            dialog.hideNavigation(useDarkStatusBarIcons())
        }
        dialog.show()
        dialog.hideNavigation(useDarkStatusBarIcons())
        dialog.window?.setLayout(minOf(resources.displayMetrics.widthPixels, ui.dp(520)), -1)
    }

    protected fun bindCelticCards(diagram: TarotCelticDiagramBinding, cards: List<ReadingCard>) {
        val slots = listOf(diagram.situation, diagram.challenge, diagram.aspiration, diagram.foundation, diagram.past,
            diagram.future, diagram.internal, diagram.external, diagram.hopes, diagram.outcome)
        slots.zip(cards).forEachIndexed { index, (image, card) ->
            loadCard(image, card.imageUrl)
            image.contentDescription = "${index + 1}. ${card.position}: ${card.title}"
            image.rotation = (if (index == 1) 90f else 0f) + (if (card.reversed) 180f else 0f)
        }
    }

    protected fun readingRow(content: LinearLayout, entity: ReadingEntity, showFavorite: Boolean = false) {
        val reading = ui.translations.reading(vm.repository.snapshot(entity), vm.cards)
        val first = reading.cards.firstOrNull() ?: return
        val row = ItemTarotHistoryBinding.inflate(layoutInflater, content, true)
        row.readingTitle.text = ui.translations.text(Spread.valueOf(reading.spread).title)
        row.readingDate.text = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(reading.createdAt))
        row.readingSummary.text = first.interpretation
        // Miniature spread thumbnails preserve the original card order/orientation.
        if (reading.cards.size == 10) {
            val miniature = TarotCelticDiagramBinding.inflate(layoutInflater, row.thumbnailHost, false)
            bindCelticCards(miniature, reading.cards)
            row.thumbnailHost.addView(miniature.root, FrameLayout.LayoutParams(ui.dp(60), -2, Gravity.CENTER))
        } else {
            val fan = reading.cards.take(3)
            val cardWidth = ui.dp(56)
            val offset = ui.dp(18)
            val fanWidth = cardWidth + (fan.size - 1) * offset
            val leading = ((row.thumbnailHost.layoutParams.width - fanWidth) / 2).coerceAtLeast(0)
            fan.forEachIndexed { index, card ->
                val image = ui.image(card.slug, card.imageUrl, 0).apply { rotation = if (card.reversed) 180f else 0f }
                row.thumbnailHost.addView(image, FrameLayout.LayoutParams(cardWidth, -1).apply { leftMargin = leading + index * offset })
            }
        }
        row.favorite.isVisible = showFavorite
        row.favorite.isSelected = entity.favorite
        row.favorite.kind = if (entity.favorite) "heart_filled" else "heart"
        row.favorite.color = if (entity.favorite) Color.RED else ui.muted
        row.favorite.setOnClickListener { vm.favorite(entity) }
        row.root.setOnClickListener { checkInternet { vm.open(entity) } }
    }

    protected fun loadCard(image: ImageView, url: String) {
        Glide.with(image).load(url).placeholder(R.drawable.tarot_deck_back).error(R.drawable.tarot_deck_back).into(image)
    }

    private fun locked(remaining: Long) {
        closeReadingLockedOverlay()
        readingLockedOverlay = TarotReadingLockedOverlay(
            this, binding.settingsOverlayHost, binding.page, remaining,
            onWatch = {
                Toast.makeText(this, R.string.tarot_ads_not_available_yet, Toast.LENGTH_SHORT).show()
            },
            onClose = ::closeReadingLockedOverlay,
            onExpired = {
                closeReadingLockedOverlay()
                if (!isDestroyed && !isFinishing) vm.refresh()
            })
    }

    private fun closeReadingLockedOverlay() {
        readingLockedOverlay?.dismiss()
        readingLockedOverlay = null
    }

    private fun loadReward(spread: Spread, overlay: TarotReadingLockedOverlay) {
        val debug = applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        val unit = if (debug) "ca-app-pub-3940256099942544/5224354917" else getString(R.string.tarot_rewarded_id)
        if (unit.isBlank()) {
            Toast.makeText(this, R.string.tarot_ad_unavailable, Toast.LENGTH_SHORT).show()
            overlay.setLoading(false)
            return
        }
        adLoading = true
        MobileAds.initialize(this) {
            runOnUiThread {
                if (isDestroyed || isFinishing || !overlay.isShowing) {
                    adLoading = false
                    overlay.setLoading(false)
                    return@runOnUiThread
                }
                RewardedAd.load(this, unit, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        adLoading = false
                        if (!isDestroyed && !isFinishing) {
                            Toast.makeText(this@TarotActivity, R.string.tarot_ad_unavailable, Toast.LENGTH_SHORT).show()
                            overlay.setLoading(false)
                        }
                    }
                    override fun onAdLoaded(ad: RewardedAd) {
                        if (isDestroyed || isFinishing || !overlay.isShowing ||
                            !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            adLoading = false
                            overlay.setLoading(false)
                            return
                        }
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                adLoading = false
                                if (rewardPending != null && !isDestroyed && !isFinishing && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                    consumeReward()
                                }
                            }
                            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                                adLoading = false
                                rewardPending = null
                                if (!isDestroyed && !isFinishing) {
                                    Toast.makeText(this@TarotActivity, R.string.tarot_ad_unavailable, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        closeReadingLockedOverlay()
                        rewardPending = null
                        ad.show(this@TarotActivity) { rewardPending = spread }
                    }
                })
            }
        }
    }

    protected fun lockDuration(milliseconds: Long): String {
        val minutes = (milliseconds + 59_999) / 60_000
        return if (minutes >= 60) "${(minutes + 59) / 60}h" else "${minutes}m"
    }

}
