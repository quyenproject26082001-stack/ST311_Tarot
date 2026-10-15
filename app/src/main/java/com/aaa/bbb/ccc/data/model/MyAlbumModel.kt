package com.aaa.bbb.ccc.data.model

data class MyAlbumModel(
    val path: String,
    val previewResId: Int? = null,
    val isFake: Boolean = false,
    var isShowSelection: Boolean = false,
    var isSelected: Boolean = false
)
