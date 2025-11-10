package com.sanna.provcalapp.ui.dashboard

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class DashboardOption(
    val key: String,
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int
)
