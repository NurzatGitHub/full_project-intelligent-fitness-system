package com.example.fitnesscoachai.domain.usecase

import com.example.fitnesscoachai.domain.catalog.CatalogTree
import com.example.fitnesscoachai.domain.model.MainCategory
import com.example.fitnesscoachai.domain.model.SubCategory

/**
 * Use case: подкатегории для выбранной главной категории.
 */
class GetSubCategories {
    operator fun invoke(main: MainCategory): List<SubCategory> = CatalogTree.subFor(main)
}
