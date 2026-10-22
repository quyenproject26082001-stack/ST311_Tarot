package com.tarot.card.meaning.core.extensions

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.tarot.card.meaning.R
import com.tarot.card.meaning.core.helper.InternetHelper
import com.tarot.card.meaning.core.utils.DataLocal
import com.tarot.card.meaning.core.utils.state.HandleState
import com.tarot.card.meaning.dialog.YesNoDialog


fun Context.initNetworkMonitor() {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d("nbhieu", "onAvailable")
            DataLocal.isConnectInternet.postValue(true)
        }
        override fun onLost(network: Network) {
            Log.d("nbhieu", "onLost")
            DataLocal.isConnectInternet.postValue(false)
        }
    }
    val request = NetworkRequest.Builder().build()
    connectivityManager.registerNetworkCallback(request, networkCallback)
}


fun Activity.checkInternet(action: () -> Unit) {
    InternetHelper.checkInternet(this) { result ->
        if (result == HandleState.SUCCESS) {
            action.invoke()
        } else {
            val dialog = YesNoDialog(
                this,
                R.string.no_internet,
                R.string.please_check_your_internet,
                isError = true
            )
            dialog.show()
            dialog.onYesClick = {
                dialog.dismiss()
                hideNavigation()
            }
        }
    }
}

