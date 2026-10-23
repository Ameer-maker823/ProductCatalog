package com.group4.productcatalog.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteProduct(
    @PrimaryKey val id: Int,
    val title: String,
    val category: String,
    val price: Double,
    val rating: Double,
    val thumbnail: String,
    val savedAt: Long = System.currentTimeMillis()
) {
    fun toProduct() = Product(
        id = id,
        title = title,
        description = "",
        category = category,
        price = price,
        rating = rating,
        thumbnail = thumbnail
    )
}

fun Product.toFavorite() = FavoriteProduct(
    id = id,
    title = title,
    category = category,
    price = price,
    rating = rating,
    thumbnail = thumbnail
)