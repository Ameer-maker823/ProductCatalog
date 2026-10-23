package com.group4.productcatalog

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.group4.productcatalog.data.Product
import com.group4.productcatalog.databinding.ActivityProductDetailBinding
import kotlinx.coroutines.launch
import java.util.Locale

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private val viewModel: DetailViewModel by viewModels()
    private var bound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.content.alpha = 0f
        binding.content.translationY = dp(24).toFloat()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            (binding.btnBack.layoutParams as FrameLayout.LayoutParams).apply {
                topMargin = bars.top + dp(12)
                leftMargin = bars.left + dp(16)
            }
            (binding.buyBar.layoutParams as FrameLayout.LayoutParams).apply {
                bottomMargin = bars.bottom + dp(14)
                leftMargin = bars.left + dp(16)
                rightMargin = bars.right + dp(16)
            }
            binding.content.setPadding(
                bars.left + dp(16), bars.top + dp(64), bars.right + dp(16), bars.bottom + dp(110)
            )
            binding.btnBack.requestLayout()
            binding.buyBar.requestLayout()
            insets
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnRetry.setOnClickListener { viewModel.load() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: DetailState) {
        val product = state.product
        binding.progress.visibility = if (state.loading) View.VISIBLE else View.GONE
        binding.errorState.visibility =
            if (!state.loading && product == null) View.VISIBLE else View.GONE
        binding.buyBar.visibility = if (product != null) View.VISIBLE else View.GONE

        state.error?.let {
            binding.tvError.setText(
                if (it == LoadError.NETWORK) R.string.error_network else R.string.error_generic
            )
        }
        if (product != null && !bound) {
            bound = true
            bind(product)
        }
    }

    private fun bind(product: Product) {
        binding.tvTitle.text = product.title
        binding.tvCategory.text = product.category.replace('-', ' ').replaceFirstChar { it.uppercase() }
        binding.tvRating.text = String.format(Locale.US, "%.1f", product.rating)
        binding.tvDetailPrice.text = String.format(Locale.US, "$%.2f", product.price)
        binding.tvDescription.text = product.description

        val urls = product.images.ifEmpty { listOf(product.thumbnail) }
        binding.viewPager.adapter = GalleryAdapter(urls)
        buildDots(urls.size)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateDots(position)
        })

        binding.content.animate().alpha(1f).translationY(0f).setDuration(400).start()
    }

    private fun buildDots(count: Int) {
        binding.dotRow.removeAllViews()
        if (count < 2) return
        repeat(count) {
            binding.dotRow.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(7), dp(7)).apply {
                    marginStart = dp(3)
                    marginEnd = dp(3)
                }
            })
        }
        updateDots(0)
    }

    private fun updateDots(selected: Int) {
        for (i in 0 until binding.dotRow.childCount) {
            val dot = binding.dotRow.getChildAt(i)
            val on = i == selected
            dot.layoutParams = (dot.layoutParams as LinearLayout.LayoutParams).apply {
                width = if (on) dp(18) else dp(7)
            }
            dot.background = GradientDrawable().apply {
                cornerRadius = dp(4).toFloat()
                setColor(ContextCompat.getColor(this@ProductDetailActivity, if (on) R.color.accent else R.color.text_muted))
            }
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_PRODUCT_ID = "product_id"
    }
}