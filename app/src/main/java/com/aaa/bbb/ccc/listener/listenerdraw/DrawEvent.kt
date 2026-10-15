package com.aaa.bbb.ccc.listener.listenerdraw

import android.view.MotionEvent
import com.aaa.bbb.ccc.core.custom.drawview.DrawView


interface DrawEvent {
    fun onActionDown(tattooView: DrawView?, event: MotionEvent?)
    fun onActionMove(tattooView: DrawView?, event: MotionEvent?)
    fun onActionUp(tattooView: DrawView?, event: MotionEvent?)
}