package me.ikirby.shareagent.databinding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ShareActivityViewModel : ViewModel() {
    val processing = MutableLiveData(false)
    val selectedAction = MutableLiveData(false)

    val isText = MutableLiveData(false)
    val isFile = MutableLiveData(false)
    val canAdd = MutableLiveData(false)
    val canFetchTitle = MutableLiveData(false)

    val subject = MutableLiveData("")
    val content = MutableLiveData("")
    val url = MutableLiveData<String>(null)
}
