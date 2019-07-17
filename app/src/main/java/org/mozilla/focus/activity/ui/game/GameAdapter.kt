package org.mozilla.focus.activity.ui.game

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_game.view.game_item
import org.mozilla.focus.R

class ShoppingAdapter(val context: Context, val items: ArrayList<String>) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_game, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.gameItem.text = items[position]
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val gameItem: TextView = view.game_item
}