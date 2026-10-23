package com.group4.productcatalog

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.group4.productcatalog.data.Product
import com.group4.productcatalog.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ProductViewModel by viewModels()
    private val adapter = ProductAdapter(
        onClick = { openDetail(it) },
        onFavorite = { viewModel.toggleFavorite(it) }
    )

    private var slugs: List<String> = emptyList()
    private var pulse: ObjectAnimator? = null
    private var lastError: LoadError? = null
    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.header.setPadding(
                bars.left + dp(16), bars.top + dp(12), bars.right + dp(16), dp(6)
            )
            binding.rvProducts.setPadding(
                bars.left + dp(9), dp(6), bars.right + dp(9), bars.bottom + dp(20)
            )
            binding.skeleton.setPadding(bars.left + dp(9), dp(6), bars.right + dp(9), 0)
            insets
        }

        binding.rvProducts.adapter = adapter
        binding.rvProducts.setHasFixedSize(true)
        binding.rvProducts.layoutAnimation =
            AnimationUtils.loadLayoutAnimation(this, R.anim.layout_grid_enter)
        setupPaging()

        binding.btnSort.setOnClickListener { viewModel.toggleSort() }
        binding.btnRetry.setOnClickListener { viewModel.retry() }
        binding.etSearch.doAfterTextChanged { viewModel.onQueryChanged(it?.toString().orEmpty()) }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else {
                false
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
                launch { viewModel.categories.collect { buildChips(it) } }
                launch { viewModel.category.collect { highlightChip() } }
                launch { viewModel.showSaved.collect { highlightChip() } }
                launch { viewModel.favoriteIds.collect { adapter.favoriteIds = it } }
                launch { viewModel.sortAsc.collect { styleSort(it) } }
            }
        }
    }

    private fun render(state: UiState) {
        val empty = state.products.isEmpty()

        binding.skeleton.visibility = if (state.loading) View.VISIBLE else View.GONE
        setPulse(state.loading)

        val wasEmpty = adapter.itemCount == 0
        adapter.submitList(state.products) {
            if (wasEmpty && state.products.isNotEmpty()) {
                binding.rvProducts.scheduleLayoutAnimation()
            }
        }
        binding.progressMore.visibility = if (state.loadingMore) View.VISIBLE else View.GONE

        val showEmpty = !state.loading && empty
        binding.emptyState.visibility = if (showEmpty) View.VISIBLE else View.GONE
        if (showEmpty) {
            binding.tvEmpty.setText(
                when (state.error) {
                    LoadError.NETWORK -> R.string.error_network
                    LoadError.GENERIC -> R.string.error_generic
                    null -> if (viewModel.showSaved.value) R.string.empty_saved else R.string.empty_products
                }
            )
            binding.btnRetry.visibility = if (state.error != null) View.VISIBLE else View.GONE
        }

        if (state.error == null) {
            snackbar?.dismiss()
            snackbar = null
        } else if (!empty && state.error != lastError) {
            val message = if (state.error == LoadError.NETWORK) R.string.error_network else R.string.error_generic
            snackbar = Snackbar.make(binding.main, message, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.action_retry) { viewModel.retry() }
                .also { it.show() }
        }
        lastError = state.error
    }

    private fun setupPaging() {
        binding.rvProducts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val manager = recyclerView.layoutManager as GridLayoutManager
                if (manager.findLastVisibleItemPosition() >= adapter.itemCount - 6) {
                    viewModel.loadMore()
                }
            }
        })
    }

    private fun setPulse(on: Boolean) {
        if (on) {
            if (pulse == null) {
                pulse = ObjectAnimator.ofFloat(binding.skeleton, View.ALPHA, 0.45f, 1f).apply {
                    duration = 800
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    start()
                }
            }
        } else {
            pulse?.cancel()
            pulse = null
            binding.skeleton.alpha = 1f
        }
    }

    private fun buildChips(categories: List<String>) {
        slugs = categories
        binding.chipRow.removeAllViews()
        val names = listOf(getString(R.string.category_all), getString(R.string.chip_saved)) +
                categories.map { it.toTitle() }
        names.forEachIndexed { index, name ->
            val chip = TextView(this).apply {
                text = name
                textSize = 12f
                setPadding(dp(12), dp(6), dp(12), dp(6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(8) }
                setOnClickListener { onChipClick(index) }
            }
            binding.chipRow.addView(chip)
        }
        highlightChip()
    }

    private fun onChipClick(index: Int) {
        when (index) {
            0 -> viewModel.setCategory(null)
            1 -> viewModel.showSavedProducts()
            else -> viewModel.setCategory(slugs[index - 2])
        }
        if (binding.etSearch.text.isNotEmpty()) binding.etSearch.setText("")
    }

    private fun highlightChip() {
        val category = viewModel.category.value
        val selected = when {
            viewModel.showSaved.value -> 1
            category == null -> 0
            else -> slugs.indexOf(category) + 2
        }
        for (i in 0 until binding.chipRow.childCount) {
            val chip = binding.chipRow.getChildAt(i) as TextView
            val on = i == selected
            chip.setBackgroundResource(if (on) R.drawable.bg_chip_on else R.drawable.bg_chip_off)
            chip.setTextColor(
                ContextCompat.getColor(this, if (on) android.R.color.white else R.color.text_secondary)
            )
        }
    }

    private fun styleSort(on: Boolean) {
        binding.btnSort.setBackgroundResource(if (on) R.drawable.bg_chip_on else R.drawable.bg_chip_off)
        val tint = ContextCompat.getColor(this, if (on) android.R.color.white else R.color.text_primary)
        binding.btnSort.imageTintList = ColorStateList.valueOf(tint)
    }

    private fun hideKeyboard() {
        WindowCompat.getInsetsController(window, binding.etSearch)
            .hide(WindowInsetsCompat.Type.ime())
        binding.etSearch.clearFocus()
    }

    private fun openDetail(product: Product) {
        val intent = Intent(this, ProductDetailActivity::class.java)
            .putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.id)
        val options = ActivityOptionsCompat.makeCustomAnimation(this, R.anim.detail_enter, R.anim.main_hold)
        startActivity(intent, options.toBundle())
    }

    private fun String.toTitle() = replace('-', ' ').replaceFirstChar { it.uppercase() }

    override fun onDestroy() {
        pulse?.cancel()
        super.onDestroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}