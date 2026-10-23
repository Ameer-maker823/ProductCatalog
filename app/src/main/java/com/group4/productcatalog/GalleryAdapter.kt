package com.group4.productcatalog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.group4.productcatalog.databinding.ItemGalleryImageBinding

class GalleryAdapter(private val urls: List<String>) :
    RecyclerView.Adapter<GalleryAdapter.ImageViewHolder>() {

    class ImageViewHolder(val binding: ItemGalleryImageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemGalleryImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.binding.ivGallery.load(urls[position]) { crossfade(250) }
    }

    override fun getItemCount() = urls.size
}