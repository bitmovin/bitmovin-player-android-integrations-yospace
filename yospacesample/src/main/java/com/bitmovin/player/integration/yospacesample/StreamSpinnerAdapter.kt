package com.bitmovin.player.integration.yospacesample

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class StreamSpinnerAdapter(context: Context, private val streams: List<Stream>, resId: Int) : ArrayAdapter<Stream>(context, resId) {

    override fun getCount(): Int = streams.size

    override fun getItem(position: Int): Stream = streams[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = buildView(
        position, convertView, parent
    )

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = buildView(
        position, convertView, parent
    )

    private fun buildView(position: Int, convertView: View?, parent: ViewGroup): View {
        val label = super.getView(position, convertView, parent) as TextView
        label.setTextColor(Color.BLACK)
        label.text = streams[position].title
        label.setPadding(16, 24, 16, 24)
        return label
    }
}
