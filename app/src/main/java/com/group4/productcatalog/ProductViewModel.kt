package com.group4.productcatalog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.group4.productcatalog.data.AppDatabase
import com.group4.productcatalog.data.Product
import com.group4.productcatalog.data.ProductApi
import com.group4.productcatalog.data.RetrofitClient
import com.group4.productcatalog.data.toFavorite
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

enum class LoadError { NETWORK, GENERIC }

data class UiState(
    val products: List<Product> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: LoadError? = null
)

class ProductViewModel(app: Application) : AndroidViewModel(app) {

    private val api = RetrofitClient.api
    private val favoriteDao = AppDatabase.getInstance(app).favoriteDao()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories

    private val _category = MutableStateFlow<String?>(null)
    val category: StateFlow<String?> = _category

    private val _sortAsc = MutableStateFlow(false)
    val sortAsc: StateFlow<Boolean> = _sortAsc

    private val _showSaved = MutableStateFlow(false)
    val showSaved: StateFlow<Boolean> = _showSaved

    val favoriteIds: StateFlow<Set<Int>> = favoriteDao.getIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private var query = ""
    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var savedJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (_categories.value.isEmpty()) loadCategories()
        loadJob?.cancel()
        _state.value = UiState(loading = true)
        loadJob = viewModelScope.launch { loadPage(0) }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.loadingMore || s.endReached || s.error != null) return
        _state.value = s.copy(loadingMore = true)
        loadJob = viewModelScope.launch { loadPage(s.products.size) }
    }

    fun retry() {
        if (_state.value.products.isEmpty()) {
            refresh()
        } else {
            _state.value = _state.value.copy(error = null)
            loadMore()
        }
    }

    fun setCategory(slug: String?) {
        val wasSaved = _showSaved.value
        leaveSaved()
        if (!wasSaved && slug == _category.value && query.isBlank()) return
        searchJob?.cancel()
        _category.value = slug
        query = ""
        refresh()
    }

    fun showSavedProducts() {
        if (_showSaved.value) return
        searchJob?.cancel()
        loadJob?.cancel()
        query = ""
        _category.value = null
        _showSaved.value = true
        _state.value = UiState(endReached = true)
        savedJob = viewModelScope.launch {
            favoriteDao.getAll().collect { list ->
                _state.value = UiState(products = list.map { it.toProduct() }, endReached = true)
            }
        }
    }

    fun toggleSort() {
        if (_showSaved.value) return
        _sortAsc.value = !_sortAsc.value
        refresh()
    }

    fun onQueryChanged(text: String) {
        val trimmed = text.trim()
        if (trimmed == query) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            query = trimmed
            leaveSaved()
            if (trimmed.isNotEmpty()) _category.value = null
            refresh()
        }
    }

    fun toggleFavorite(product: Product) {
        viewModelScope.launch {
            if (product.id in favoriteIds.value) {
                favoriteDao.deleteById(product.id)
            } else {
                favoriteDao.insert(product.toFavorite())
            }
        }
    }

    private fun leaveSaved() {
        savedJob?.cancel()
        savedJob = null
        _showSaved.value = false
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                _categories.value = api.getCategories()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The chips are optional, so a failure here is not shown.
            }
        }
    }

    private suspend fun loadPage(skip: Int) {
        try {
            val sortBy = if (_sortAsc.value) "price" else null
            val order = if (_sortAsc.value) "asc" else null
            val category = _category.value
            val limit = ProductApi.PAGE_SIZE

            val response = when {
                query.isNotBlank() -> api.searchProducts(query, limit, skip, sortBy, order)
                category != null -> api.getByCategory(category, limit, skip, sortBy, order)
                else -> api.getProducts(limit, skip, sortBy, order)
            }

            val previous = if (skip == 0) emptyList() else _state.value.products
            val all = previous + response.products
            _state.value = UiState(
                products = all,
                endReached = response.products.isEmpty() || all.size >= response.total
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            fail(LoadError.NETWORK)
        } catch (e: Exception) {
            fail(LoadError.GENERIC)
        }
    }

    private fun fail(error: LoadError) {
        _state.value = _state.value.copy(loading = false, loadingMore = false, error = error)
    }
}