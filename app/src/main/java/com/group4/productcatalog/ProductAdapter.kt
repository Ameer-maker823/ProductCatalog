package com.group4.productcatalog

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.group4.productcatalog.data.Product
import com.group4.productcatalog.databinding.ItemProductBinding
import java.util.Locale

class ProductAdapter(
    private val onClick: (Product) -> Unit = {},
    private val onFavorite: (Product) -> Unit = {}
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(DiffCallback()) {

    private var changedIds: Set<Int> = emptySet()

    var favoriteIds: Set<Int> = emptySet()
        set(value) {
            changedIds = (field - value) + (value - field)
            field = value
            notifyItemRangeChanged(0, itemCount, FAVORITE)
        }

    class ProductViewHolder(val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        with(holder.binding) {
            tvProductName.text = product.title
            tvProductCategory.text = product.category.replace('-', ' ').replaceFirstChar { it.uppercase() }
            tvPrice.text = String.format(Locale.US, "$%.2f", product.price)
            tvRating.text = String.format(Locale.US, "%.1f", product.rating)
            ivProduct.load(product.thumbnail) { crossfade(250) }
            productCard.setOnClickListener { onClick(product) }
            btnFavorite.setOnClickListener { onFavorite(product) }
            productCard.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN ->
                        v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }
                false
            }
        }
        styleHeart(holder.binding, product.id in favoriteIds, animate = false)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.contains(FAVORITE)) {
            val id = getItem(position).id
            styleHeart(holder.binding, id in favoriteIds, animate = id in changedIds)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private fun styleHeart(binding: ItemProductBinding, saved: Boolean, animate: Boolean) {
        val button = binding.btnFavorite
        val context = button.context
        button.setImageResource(if (saved) R.drawable.ic_heart_filled else R.drawable.ic_heart)
        val tint = ContextCompat.getColor(context, if (saved) R.color.heart else R.color.text_primary)
        button.imageTintList = ColorStateList.valueOf(tint)
        if (animate) {
            button.animate().scaleX(1.3f).scaleY(1.3f).setDuration(120).withEndAction {
                button.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
            }.start()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(old: Product, new: Product) = old.id == new.id
        override fun areContentsTheSame(old: Product, new: Product) = old == new
    }

    private companion object {
        const val FAVORITE = "favorite"
    }
}