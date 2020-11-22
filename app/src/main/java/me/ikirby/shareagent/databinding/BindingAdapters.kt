package me.ikirby.shareagent.databinding

import android.util.Log
import android.view.View
import androidx.databinding.BindingAdapter

@BindingAdapter("app:show")
fun bindShow(view: View, show: Boolean) {
    Log.d("bindShow", "set visible = $show")
    if (show) {
        view.visibility = View.VISIBLE
    } else {
        view.visibility = View.GONE
    }
}
