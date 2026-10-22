package com.tarot.card.meaning.core.utils.share.whatsapp

import android.content.Context
import com.tarot.card.meaning.data.local.PersistenceRepository

object LocalStorageUtils {

    fun readData(context: Context, key: String): Any? {
        return PersistenceRepository.getStringSetting(context, key, "")
            .takeIf { it.isNotEmpty() }
    }
}
