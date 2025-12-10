package com.sanna.provcalapp.utils

import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.sanna.provcalapp.R

/**
 * Extension functions for Fragment to show Snackbar notifications
 */

/**
 * Show error message as Snackbar with red background
 */
fun Fragment.showErrorSnackbar(message: String) {
    view?.let { v ->
        Snackbar.make(v, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.error_red))
            .setTextColor(Color.WHITE)
            .show()
    }
}

/**
 * Show success message as Snackbar with green background
 */
fun Fragment.showSuccessSnackbar(message: String) {
    view?.let { v ->
        Snackbar.make(v, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.success_green))
            .setTextColor(Color.WHITE)
            .show()
    }
}

/**
 * Show info message as Snackbar with default background
 */
fun Fragment.showInfoSnackbar(message: String) {
    view?.let { v ->
        Snackbar.make(v, message, Snackbar.LENGTH_SHORT)
            .show()
    }
}
