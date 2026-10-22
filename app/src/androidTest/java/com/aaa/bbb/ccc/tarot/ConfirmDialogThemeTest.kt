package com.tarot.card.meaning.tarot

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tarot.card.meaning.R
import com.tarot.card.meaning.databinding.DialogConfirmBinding
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfirmDialogThemeTest {
    private fun resource(context: Context, attribute: Int): Int {
        val value = TypedValue()
        check(context.theme.resolveAttribute(attribute, value, true))
        return value.resourceId
    }

    @Test fun confirmDialogUsesThemeSpecificArtworkAndTextColors() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            listOf(
                Triple(R.style.Theme_BaseProject, R.drawable.bg_dialog_delete_exit, R.drawable.ic_yes_dialog),
                Triple(R.style.TarotThemeLight, R.drawable.bg_dialog_delete_exit, R.drawable.ic_yes_dialog),
                Triple(R.style.TarotThemeDark, R.drawable.tarot_confirm_dark_background, R.drawable.tarot_confirm_dark_keep)
            ).forEach { (theme, panel, noButton) ->
                val context = ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().targetContext, theme)
                val binding = DialogConfirmBinding.inflate(LayoutInflater.from(context))
                assertEquals(panel, resource(context, R.attr.tarotConfirmBackground))
                assertEquals(noButton, resource(context, R.attr.tarotConfirmNoBackground))
                assertEquals(
                    if (theme == R.style.TarotThemeDark) R.drawable.tarot_confirm_dark_leave else R.drawable.ic_no_dialog,
                    resource(context, R.attr.tarotConfirmYesBackground)
                )
                assertEquals(
                    Color.parseColor(if (theme == R.style.TarotThemeDark) "#E3E2CE" else "#172A2A"),
                    binding.tvTitle.currentTextColor
                )
                assertEquals(
                    Color.parseColor(if (theme == R.style.TarotThemeDark) "#CDDEDA" else "#60706D"),
                    binding.tvDescription.currentTextColor
                )
                assertEquals(
                    Color.parseColor(if (theme == R.style.TarotThemeDark) "#FFFFFF" else "#172A2A"),
                    binding.btnNo.currentTextColor
                )
            }
        }
    }
}
