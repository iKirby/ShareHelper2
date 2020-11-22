package me.ikirby.shareagent.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.ikirby.shareagent.databinding.ItemParamsListBinding

class ParamsListAdapter : RecyclerView.Adapter<ParamsListAdapter.ParamsListViewHolder>() {

    private val list = mutableListOf<String>()
    var onItemClickListener: OnItemClickListener? = null

    class ParamsListViewHolder(binding: ItemParamsListBinding) : RecyclerView.ViewHolder(binding.root) {
        val textView = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParamsListViewHolder {
        val binding = ItemParamsListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParamsListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParamsListViewHolder, position: Int) {
        holder.textView.text = list[position]
        holder.textView.setOnLongClickListener {
            onItemClickListener?.onClick(it, position)
            true
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun addItem(str: String) {
        list.add(str)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        list.removeAt(position)
        notifyDataSetChanged()
    }

    fun getList(): List<String> {
        return list.toList()
    }
}