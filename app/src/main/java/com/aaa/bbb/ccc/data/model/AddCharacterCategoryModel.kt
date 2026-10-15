package com.aaa.bbb.ccc.data.model

data class AddCharacterCategoryModel(
    val name: String,
    val items: ArrayList<SelectedModel>,
    var isSelected: Boolean = false
)
