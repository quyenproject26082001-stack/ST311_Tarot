package com.aaa.bbb.ccc.tarot

import android.content.Context
import android.widget.EditText
import android.widget.Toast
import com.aaa.bbb.ccc.R

/** Shared error presentation; does not change the XML layout. */
fun showProfileValidationError(context: Context, error: ProfileValidationError, nameInput: EditText? = null) {
    val message = context.getString(when (error) {
        ProfileValidationError.NAME_REQUIRED -> R.string.tarot_name_required
        ProfileValidationError.NAME_TOO_LONG -> R.string.tarot_name_too_long
        ProfileValidationError.NAME_UNSUPPORTED_CHARACTER -> R.string.tarot_name_hidden_characters
        ProfileValidationError.INVALID_BIRTHDAY -> R.string.tarot_invalid_birthday
        ProfileValidationError.INVALID_ZODIAC -> R.string.tarot_invalid_zodiac
        ProfileValidationError.BIRTHDAY_ZODIAC_MISMATCH -> R.string.tarot_birthday_zodiac_mismatch
    })
    if (nameInput != null && error in listOf(ProfileValidationError.NAME_REQUIRED,
            ProfileValidationError.NAME_TOO_LONG, ProfileValidationError.NAME_UNSUPPORTED_CHARACTER)) {
        nameInput.error = message
        nameInput.requestFocus()
    } else Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
