package com.example.snitchsms.roomdatabase

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.snitchsms.contacts.model.SmsSaveModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsViewModel(private val repository: SmsRepository) : ViewModel() {

    val allItems: LiveData<List<SmsSaveModel>> = repository.getAll()

    fun insertItem(chatModel: SmsSaveModel) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(chatModel)
        }
    }
    fun deleteSelectedItems(selectedItems: List<SmsSaveModel>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSelectedItems(selectedItems)
        }
    }
    fun deleteItem() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete()
        }
    }
    suspend fun isContactBlocked(contactName: String): Boolean {
        return repository.isContactBlocked(contactName)
    }
     fun blockContact(phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.blockContact(phoneNumber)
        }
    }
    fun unblockContact(contactName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.unblockContact(contactName)
        }
    }
        fun deleteAllMessagesOfChat(contactName: String) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.deleteMessagesForContact(contactName)
            }
        }

    fun deleteItem(item: SmsSaveModel) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteItem(item)
        }
    }

    class Factory(private val repository: SmsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SmsViewModel::class.java)) {
                return SmsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}


