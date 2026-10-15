package com.aaa.bbb.ccc.ui.random_character

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import com.aaa.bbb.ccc.R
import com.aaa.bbb.ccc.core.helper.InternetHelper
import com.aaa.bbb.ccc.core.utils.state.HandleState
import com.aaa.bbb.ccc.data.model.custom.SuggestionModel
import kotlinx.coroutines.flow.MutableStateFlow

class RandomCharacterViewModel : ViewModel() {

    val randomList = ArrayList<SuggestionModel>()
    // Data từ API hay không
    private val _isDataAPI = MutableStateFlow(false)
    //-----------------------------------------------------------------------------------------------------------------

    suspend fun updateRandomList(suggestionModel: SuggestionModel){
        randomList.add(suggestionModel)
    }
    fun upsideDownList() = randomList.shuffle()

    fun setIsDataAPI(isAPI: Boolean) {
        _isDataAPI.value = isAPI
    }

    fun checkDataInternet(context: AppCompatActivity, action: (() -> Unit)) {
        if (!_isDataAPI.value) {
            action.invoke()
            return
        }
        InternetHelper.checkInternet(context) { result ->
            if (result == HandleState.SUCCESS) {
                action.invoke()
            } else {
                // Show No Internet dialog
                val dialog = com.aaa.bbb.ccc.dialog.YesNoDialog(
                    context,
                    R.string.no_internet,
                    R.string.please_check_your_internet,
                    isError = true
                )
                dialog.show()
                dialog.onYesClick = {
                    dialog.dismiss()
                }
            }
        }
    }


}