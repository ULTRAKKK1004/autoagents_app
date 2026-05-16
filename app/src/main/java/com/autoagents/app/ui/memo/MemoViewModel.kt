package com.autoagents.app.ui.memo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoagents.app.App
import com.autoagents.app.data.db.MemoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoListViewModel : ViewModel() {
    private val dao = App.get().database.memoDao()
    val memos: StateFlow<List<MemoEntity>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class MemoEditViewModel : ViewModel() {
    private val dao = App.get().database.memoDao()
    private val _memo = MutableStateFlow(
        MemoEntity(title = "", contentMarkdown = "", tags = null, createdAt = 0, updatedAt = 0)
    )
    val memo: StateFlow<MemoEntity> = _memo

    fun load(id: Long) {
        if (id <= 0L) return
        viewModelScope.launch {
            dao.getById(id)?.let { _memo.value = it }
        }
    }

    fun update(transform: (MemoEntity) -> MemoEntity) {
        _memo.value = transform(_memo.value)
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val current = _memo.value
            if (current.title.isBlank() && current.contentMarkdown.isBlank()) {
                onSaved()
                return@launch
            }
            if (current.id <= 0L) {
                dao.insert(current.copy(createdAt = now, updatedAt = now))
            } else {
                dao.update(current.copy(updatedAt = now))
            }
            onSaved()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            val current = _memo.value
            if (current.id > 0L) dao.delete(current)
            onDone()
        }
    }
}
