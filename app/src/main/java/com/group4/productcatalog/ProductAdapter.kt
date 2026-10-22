package com.group4.productcatalog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.group4.productcatalog.data.Product
import com.group4.productcatalog.databinding.ItemProductBinding
import java.util.Locale

class ProductAdapter(
    private val onClick: (Product) -> Unit = {},
    private val onFavorite: (Product) -> Unit = {}
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(DiffCallback()) {

    class ProductViewHolder(val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        with(holder.binding) {
            tvProductName.text = product.title
            tvProductCategory.text = product.category.replaceFirstChar { it.uppercase() }
            tvPrice.text = String.format(Locale.US, "$%.2f", product.price)
            tvRating.text = String.format(Locale.US, "%.1f", product.rating)
            productCard.setOnClickListener { onClick(product) }
            btnFavorite.setOnClickListener { onFavorite(product) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(old: Product, new: Product) = old.id == new.id
        override fun areContentsTheSame(old: Product, new: Product) = old == new
    }
}