package org.example.animation.io

import kotlinx.serialization.Serializable
import org.example.animation.model.BrushPreset

@Serializable
data class BrushStoreItem(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val category: String,
    val tags: List<String> = emptyList(),
    val rating: Float = 0f,
    val downloads: Int = 0,
    val previewPng: String? = null,
    val downloadUrl: String,
    val size: Int = 0
)

@Serializable
data class BrushStoreCategory(
    val id: String,
    val name: String,
    val description: String
)

@Serializable
data class BrushStoreResponse(
    val items: List<BrushStoreItem>,
    val categories: List<BrushStoreCategory>,
    val total: Int,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class BrushStoreInstallRequest(
    val brushId: String
)

@Serializable
data class BrushStoreInstallResponse(
    val success: Boolean,
    val brush: BrushPreset? = null,
    val error: String? = null
)