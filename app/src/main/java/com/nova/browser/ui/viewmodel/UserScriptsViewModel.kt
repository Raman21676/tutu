package com.nova.browser.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.browser.data.local.db.UserScriptEntity
import com.nova.browser.domain.repository.UserScriptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserScriptsViewModel @Inject constructor(
    private val repository: UserScriptRepository
) : ViewModel() {

    val scripts = repository.getAllScripts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enabledScripts = repository.getEnabledScripts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(script: UserScriptEntity) {
        viewModelScope.launch {
            if (script.id == 0L) {
                repository.insert(script)
            } else {
                repository.update(script)
            }
        }
    }

    fun delete(script: UserScriptEntity) {
        viewModelScope.launch {
            repository.delete(script)
        }
    }

    fun toggleEnabled(script: UserScriptEntity) {
        viewModelScope.launch {
            repository.setEnabled(script.id, !script.enabled)
        }
    }
}
