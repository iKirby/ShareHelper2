package me.ikirby.osscomponent

import android.app.ActionBar
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import me.ikirby.shareagent.R

class OSSComponentAdapter(private val itemClickListener: OSSItemClickListener) :
    RecyclerView.Adapter<OSSComponentAdapter.OSSComponentViewHolder>() {

    private var list = emptyList<OSSComponent>()

    class OSSComponentViewHolder(view: TextView) : RecyclerView.ViewHolder(view) {
        val textView = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OSSComponentViewHolder {
        val displayMetrics = parent.context.resources.displayMetrics
        val padding =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16F, displayMetrics).toInt()
        val view = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.WRAP_CONTENT
            )
            setPadding(padding, padding, padding, padding)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
            with(TypedValue()) {
                context.theme.resolveAttribute(R.attr.selectableItemBackground, this, true)
                setBackgroundResource(resourceId)

                context.theme.resolveAttribute(android.R.attr.textColorPrimary, this, true)
                setTextColor(ContextCompat.getColor(context, resourceId))
            }
        }
        return OSSComponentViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: OSSComponentViewHolder, position: Int) {
        holder.textView.text = list[position].name
        holder.textView.setOnClickListener {
            itemClickListener.onClick(list[position].licenseUrl)
        }
    }

    fun setData(list: List<OSSComponent>) {
        this.list = list
        notifyDataSetChanged()
    }
}
