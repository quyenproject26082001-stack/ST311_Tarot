package com.tarot.card.meaning.ui.view

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarot.card.meaning.core.extensions.shareImagesPaths
import com.tarot.card.meaning.core.helper.MediaHelper
import com.tarot.card.meaning.core.utils.key.ValueKey
import com.tarot.card.meaning.core.utils.state.HandleState
import com.tarot.card.meaning.data.model.custom.SuggestionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class ViewViewModel : ViewModel() {
    private val _pathInternal = MutableStateFlow<String>("")
    val pathInternal: StateFlow<String> = _pathInternal.asStateFlow()

    var statusFrom = ValueKey.AVATAR_TYPE

    fun setPath(path: String) {
        _pathInternal.value = path
    }

    fun deleteFile(context: Context, path: String): Flow<HandleState> = flow {
        if (statusFrom == ValueKey.MY_DESIGN_TYPE || statusFrom == ValueKey.PRIDE_OVERLAY_TYPE) {
            emitAll(MediaHelper.deleteFileByPath(arrayListOf(path), context))
        } else {
            emit(HandleState.LOADING)
            val result = try {
                val originList = MediaHelper
                    .readListFromFile<SuggestionModel>(context, ValueKey.EDIT_FILE_INTERNAL)
                    .toCollection(ArrayList())

                val editDelete = originList.firstOrNull { it.pathInternalEdit?.equals(path) == true }

                if (editDelete != null) {
                    originList.remove(editDelete)
                    MediaHelper.writeListToFile(context, ValueKey.EDIT_FILE_INTERNAL, originList)
                    HandleState.SUCCESS
                } else {
                    HandleState.FAIL
                }
            } catch (e: Exception) {
                Log.e("nbhieu", "deleteFile: $e")
                HandleState.FAIL
            }
            emit(result)
        }
    }.flowOn(Dispatchers.IO)

    fun shareFiles(context: Activity) {
        viewModelScope.launch {
            context.shareImagesPaths(arrayListOf(_pathInternal.value))
        }
    }

    fun downloadFiles(context: Activity): Flow<HandleState> = flow {
        emitAll(
            MediaHelper.downloadPartsToExternal(
                context, arrayListOf(_pathInternal.value)
            )
        )
    }.flowOn(Dispatchers.IO)

    fun updateStatusFrom(status: Int) {
        statusFrom = status
    }
}
