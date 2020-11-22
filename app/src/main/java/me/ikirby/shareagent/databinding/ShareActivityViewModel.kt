package me.ikirby.shareagent.databinding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ShareActivityViewModel: ViewModel() {
    val processing = MutableLiveData(false)

    val isText = MutableLiveData(false)
    val isFile = MutableLiveData(false)

    val content = MutableLiveData("")
}
