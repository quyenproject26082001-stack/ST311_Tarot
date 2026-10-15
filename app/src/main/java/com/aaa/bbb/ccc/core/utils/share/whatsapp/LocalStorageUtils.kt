package com.aaa.bbb.ccc.core.utils.share.whatsapp

import android.content.Context
import com.aaa.bbb.ccc.data.local.PersistenceRepository

object LocalStorageUtils {

    fun readData(context: Context, key: String): Any? {
        return PersistenceRepository.getStringSetting(context, key, "")
            .takeIf { it.isNotEmpty() }
    }
}
