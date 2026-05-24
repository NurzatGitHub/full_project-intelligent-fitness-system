package com.example.fitnesscoachai.ui.home

import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.domain.model.MainCategory

/**
 * Маппинг MainCategory → ресурсы для UI (название, цвет карточки).
 */
object MainCategoryDisplay {
    fun titleRes(main: MainCategory): Int = when (main) {
        MainCategory.BACK -> R.string.category_back
        MainCategory.CHEST -> R.string.category_chest
        MainCategory.LEGS -> R.string.category_legs
        MainCategory.ARMS -> R.string.category_arms
        MainCategory.ABS -> R.string.category_abs
        MainCategory.CARDIO -> R.string.category_cardio
    }

    fun colorRes(main: MainCategory): Int = when (main) {
        MainCategory.BACK -> R.color.category_back
        MainCategory.CHEST -> R.color.category_chest
        MainCategory.LEGS -> R.color.category_upper_legs
        MainCategory.ARMS -> R.color.category_upper_arms
        MainCategory.ABS -> R.color.category_waist
        MainCategory.CARDIO -> R.color.category_cardio
    }

    fun iconRes(main: MainCategory): Int = when (main) {
        MainCategory.BACK -> R.drawable.ic_category_back
        MainCategory.CHEST -> R.drawable.ic_category_chest
        MainCategory.LEGS -> R.drawable.ic_category_legs
        MainCategory.ARMS -> R.drawable.ic_category_arms
        MainCategory.ABS -> R.drawable.ic_category_abs
        MainCategory.CARDIO -> R.drawable.ic_category_cardio
    }

    /** Фоновое фото/картинка карточки. Подставьте свои drawable (например category_back_bg). */
    fun backgroundRes(main: MainCategory): Int = when (main) {
        MainCategory.BACK -> R.drawable.back
        MainCategory.CHEST -> R.drawable.chest
        MainCategory.LEGS -> R.drawable.legs
        MainCategory.ARMS -> R.drawable.arms
        MainCategory.ABS -> R.drawable.abs
        MainCategory.CARDIO -> R.drawable.cardio
    }
}
