package com.tarot.card.meaning.data.model

data class LanguageModel(
    val code: String,
    val name: String,
    val flag: Int,
    var activate: Boolean = false
)
