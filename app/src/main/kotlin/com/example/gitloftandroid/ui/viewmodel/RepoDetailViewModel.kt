package com.example.gitloftandroid.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gitloftandroid.data.model.GitHubContentItem
import com.example.gitloftandroid.data.repository.GitHubRepository
import com.example.gitloftandroid.util.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RepoDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStorage = TokenStorage(application)
    private val gitHubRepository = GitHubRepository(tokenStorage)

    // README
    private val _readmeContent = MutableStateFlow<String?>(null)
    val readmeContent: StateFlow<String?> = _readmeContent.asStateFlow()

    private val _isReadmeLoading = MutableStateFlow(false)
    val isReadmeLoading: StateFlow<Boolean> = _isReadmeLoading.asStateFlow()

    private val _readmeError = MutableStateFlow<String?>(null)
    val readmeError: StateFlow<String?> = _readmeError.asStateFlow()

    // CODE EXPLORER
    private val _codeItems = MutableStateFlow<List<GitHubContentItem>>(emptyList())
    val codeItems: StateFlow<List<GitHubContentItem>> = _codeItems.asStateFlow()

    private val _isCodeLoading = MutableStateFlow(false)
    val isCodeLoading: StateFlow<Boolean> = _isCodeLoading.asStateFlow()

    private val _codeError = MutableStateFlow<String?>(null)
    val codeError: StateFlow<String?> = _codeError.asStateFlow()

    private val _pathStack = MutableStateFlow<List<String>>(emptyList())
    val pathStack: StateFlow<List<String>> = _pathStack.asStateFlow()

    private val _selectedFile = MutableStateFlow<GitHubContentItem?>(null)
    val selectedFile: StateFlow<GitHubContentItem?> = _selectedFile.asStateFlow()

    private val _selectedFileContent = MutableStateFlow<String?>(null)
    val selectedFileContent: StateFlow<String?> = _selectedFileContent.asStateFlow()

    private val _isFileLoading = MutableStateFlow(false)
    val isFileLoading: StateFlow<Boolean> = _isFileLoading.asStateFlow()

    private val _fileError = MutableStateFlow<String?>(null)
    val fileError: StateFlow<String?> = _fileError.asStateFlow()

    val codeSearchText = MutableStateFlow("")
    val codeFilter = MutableStateFlow("All") // "All", "Folders", "Files"

    private val dirCache = mutableMapOf<String, List<GitHubContentItem>>()

    fun loadReadme(owner: String, repo: String) {
        if (_readmeContent.value != null) return

        viewModelScope.launch {
            _isReadmeLoading.value = true
            _readmeError.value = null
            try {
                val content = gitHubRepository.getReadme(owner, repo)
                _readmeContent.value = content
            } catch (e: Exception) {
                _readmeError.value = "README not found or empty."
            } finally {
                _isReadmeLoading.value = false
            }
        }
    }

    fun loadDirectory(owner: String, repo: String, path: String = "") {
        val cacheKey = "$owner/$repo/$path"
        dirCache[cacheKey]?.let {
            _codeItems.value = it
            return
        }

        viewModelScope.launch {
            _isCodeLoading.value = true
            _codeError.value = null
            try {
                val items = gitHubRepository.getRepoContents(owner, repo, path)
                // Folders first, then files alphabetically
                val sorted = items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                dirCache[cacheKey] = sorted
                _codeItems.value = sorted
            } catch (e: Exception) {
                _codeError.value = e.localizedMessage ?: "Failed to read directory."
            } finally {
                _isCodeLoading.value = false
            }
        }
    }

    fun navigateInto(owner: String, repo: String, item: GitHubContentItem) {
        if (item.isDirectory) {
            val newStack = _pathStack.value + item.name
            _pathStack.value = newStack
            loadDirectory(owner, repo, newStack.joinToString("/"))
        } else {
            loadFile(owner, repo, item)
        }
    }

    fun navigateBack(owner: String, repo: String): Boolean {
        if (_selectedFile.value != null) {
            _selectedFile.value = null
            _selectedFileContent.value = null
            return true
        }

        val stack = _pathStack.value
        if (stack.isNotEmpty()) {
            val newStack = stack.dropLast(1)
            _pathStack.value = newStack
            loadDirectory(owner, repo, newStack.joinToString("/"))
            return true
        }
        return false
    }

    fun loadFile(owner: String, repo: String, item: GitHubContentItem) {
        _selectedFile.value = item
        if (item.isImage) {
            _selectedFileContent.value = null
            _fileError.value = null
            _isFileLoading.value = false
            return
        }

        viewModelScope.launch {
            _isFileLoading.value = true
            _fileError.value = null
            try {
                val content = gitHubRepository.getFileContent(owner, repo, item.path)
                _selectedFileContent.value = content
            } catch (e: Exception) {
                _fileError.value = "Binary or unreadable file: ${e.localizedMessage}"
            } finally {
                _isFileLoading.value = false
            }
        }
    }

    fun closeFile() {
        _selectedFile.value = null
        _selectedFileContent.value = null
    }
}
