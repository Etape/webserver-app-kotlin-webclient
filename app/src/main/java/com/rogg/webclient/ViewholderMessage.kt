package com.rogg.webclient

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*


class ViewholderMessage(private val dataSet: ArrayList<MainActivity.Message>)  :
        RecyclerView.Adapter<ViewholderMessage.ViewHolder>() {
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val message: TextView
        val time: TextView
        init {
            // Define click listener for the ViewHolder's View
            message = view.findViewById(R.id.message)
            time = view.findViewById(R.id.time)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_message, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        viewHolder.message.text = dataSet[position].message
        viewHolder.time.text = getDateTime(dataSet[position].time)
    }
    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size
    private fun getDateTime(s: Long): String? {
        try {
            val sdf = SimpleDateFormat("kk:mm")
            val netDate = Date(s * 1000)
            return sdf.format(netDate)
        } catch (e: Exception) {
            return e.toString()
        }
    }


}
