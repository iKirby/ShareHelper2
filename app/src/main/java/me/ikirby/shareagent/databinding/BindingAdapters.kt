package me.ikirby.shareagent.databinding

import android.view.View
import androidx.databinding.BindingAdapter

@BindingAdapter("app:show")
fun bindShow(view: View, show: Boolean) {
    if (show) {
        view.visibility = View.VISIBLE
    } else {
        view.visibility = View.GONE
    }
}
