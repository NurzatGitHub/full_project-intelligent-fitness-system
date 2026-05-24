package com.example.fitnesscoachai.domain.model

/**
 * Главные категории каталога (экран 1).
 */
enum class MainCategory(val id: String, val titleEn: String) {
    BACK("back", "Back"),
    CHEST("chest", "Chest"),
    LEGS("legs", "Legs"),
    ARMS("arms", "Arms"),
    ABS("abs", "Abs"),
    CARDIO("cardio", "Cardio");

    companion object {
        fun fromId(id: String?): MainCategory? = entries.find { it.id == id }
    }
}

/**
 * Подкатегория внутри главной категории (экран 2).
 */
data class SubCategory(
    val id: String,
    val titleEn: String,
    val main: MainCategory
)
