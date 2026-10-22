package com.group4.productcatalog

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.group4.productcatalog.data.Product
import com.group4.productcatalog.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val adapter = ProductAdapter()
    private var sortOn = false

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
            insets
        }

        binding.rvProducts.adapter = adapter
        adapter.submitList(sampleProducts())

        buildChips(listOf("All", "Smartphones", "Laptops", "Fragrances", "Groceries"))
        binding.btnSort.setOnClickListener { toggleSort() }
    }

    private fun buildChips(names: List<String>) {
        binding.chipRow.removeAllViews()
        names.forEachIndexed { index, name ->
            val chip = TextView(this).apply {
                text = name
                textSize = 12f
                setPadding(dp(12), dp(6), dp(12), dp(6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(8) }
                setOnClickListener { selectChip(index) }
            }
            binding.chipRow.addView(chip)
        }
        selectChip(0)
    }

    private fun selectChip(index: Int) {
        for (i in 0 until binding.chipRow.childCount) {
            val chip = binding.chipRow.getChildAt(i) as TextView
            val on = i == index
            chip.setBackgroundResource(if (on) R.drawable.bg_chip_on else R.drawable.bg_chip_off)
            chip.setTextColor(
                ContextCompat.getColor(this, if (on) android.R.color.white else R.color.text_secondary)
            )
        }
    }

    private fun toggleSort() {
        sortOn = !sortOn
        binding.btnSort.setBackgroundResource(if (sortOn) R.drawable.bg_chip_on else R.drawable.bg_chip_off)
        val tint = ContextCompat.getColor(this, if (sortOn) android.R.color.white else R.color.text_primary)
        binding.btnSort.imageTintList = ColorStateList.valueOf(tint)
    }

    // Temporary: replaced by real API data in the next steps.
    private fun sampleProducts() = listOf(
        Product(1, "Wireless headphones", "", "electronics", 59.0, 4.6, ""),
        Product(2, "Smart watch", "", "electronics", 129.0, 4.4, ""),
        Product(3, "Leather backpack", "", "fashion", 84.0, 4.7, ""),
        Product(4, "Running shoes", "", "fashion", 72.0, 4.5, ""),
        Product(5, "Desk lamp", "", "home", 35.0, 4.3, ""),
        Product(6, "Sunglasses", "", "fashion", 48.0, 4.2, "")
    )

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}