package com.group4.productcatalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group4.productcatalog.data.Product
import com.group4.productcatalog.data.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

data class DetailState(
    val loading: Boolean = true,
    val product: Product? = null,
    val error: LoadError? = null
)

class DetailViewModel(handle: SavedStateHandle) : ViewModel() {

    private val productId: Int = handle.get<Int>(ProductDetailActivity.EXTRA_PRODUCT_ID) ?: -1

    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state

    init {
        load()
    }

    fun load() {
        _state.value = DetailState(loading = true)
        viewModelScope.launch {
            try {
                _state.value = DetailState(loading = false, product = RetrofitClient.api.getProduct(productId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = DetailState(loading = false, error = LoadError.NETWORK)
            } catch (e: Exception) {
                _state.value = DetailState(loading = false, error = LoadError.GENERIC)
            }
        }
    }
}