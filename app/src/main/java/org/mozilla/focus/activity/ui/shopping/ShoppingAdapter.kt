package org.mozilla.focus.activity.ui.shopping

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_shopping.view.shopping_item
import org.mozilla.focus.R

class ShoppingAdapter(val context: Context, val items: ArrayList<String>) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_shopping, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.shoppingItem.text = items[position]
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val shoppingItem: TextView = view.shopping_item
}