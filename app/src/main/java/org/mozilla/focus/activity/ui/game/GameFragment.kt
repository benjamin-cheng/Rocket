package org.mozilla.focus.activity.ui.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_shopping.content_listing
import org.mozilla.focus.R
import org.mozilla.focus.activity.ui.shopping.ShoppingAdapter

class GameFragment : Fragment() {

    companion object {
        fun newInstance() = GameFragment()
    }

    private lateinit var viewModel: GameViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(GameViewModel::class.java)
        // TODO: Use the ViewModel
        val gameItems = ArrayList<String>()
        for (i in 0..100) {
            gameItems.add(i.toString())
        }

        content_listing.layoutManager = LinearLayoutManager(context)
        content_listing.adapter = ShoppingAdapter(context!!, gameItems)
    }

}
