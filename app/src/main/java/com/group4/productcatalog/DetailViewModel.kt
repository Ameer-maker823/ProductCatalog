package com.group4.productcatalog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.group4.productcatalog.data.AppDatabase
import com.group4.productcatalog.data.Product
import com.group4.productcatalog.data.RetrofitClient
import com.group4.productcatalog.data.toFavorite
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

data class DetailState(
    val loading: Boolean = true,
    val product: Product? = null,
    val error: LoadError? = null
)

class DetailViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {

    private val productId: Int = handle.get<Int>(ProductDetailActivity.EXTRA_PRODUCT_ID) ?: -1
    private val dao = AppDatabase.getInstance(app).favoriteDao()

    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state

    val isFavorite: StateFlow<Boolean> = dao.isFavorite(productId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

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

    fun toggleFavorite() {
        val product = _state.value.product ?: return
        viewModelScope.launch {
            if (isFavorite.value) dao.deleteById(product.id) else dao.insert(product.toFavorite())
        }
    }
}