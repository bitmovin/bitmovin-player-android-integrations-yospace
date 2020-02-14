package com.bitmovin.player.integration.yospacesample

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.adapter_item.view.*

import java.util.ArrayList

class ListAdapter : RecyclerView.Adapter<ListAdapter.ViewHolder>() {

    private val items = ArrayList<ListItem>()

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        LayoutInflater.from(viewGroup.context).inflate(R.layout.adapter_item, viewGroup, false)
    )

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun add(item: ListItem) {
        items.add(item)
        notifyItemInserted(itemCount)
    }

    fun clear() {
        val itemCount = itemCount
        items.clear()
        notifyItemRangeRemoved(0, itemCount)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(adItem: ListItem) {
            itemView.entry_one_text_view.text = adItem.entryOne
            itemView.entry_two_text_view.text = adItem.entryTwo
            itemView.entry_three_text_view.text = adItem.entryThree
            itemView.entry_four_text_view.text = adItem.entryFour
        }
    }
}
