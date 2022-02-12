package com.screenslicerfree.detailsView

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.screenslicerfree.database.DAOScrenshots

class DetailsViewModelFactory (
    private val itemUri: String,
    private val dataSource: DAOScrenshots) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailsViewModel::class.java)) {
            return DetailsViewModel(itemUri, dataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}