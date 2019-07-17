package org.mozilla.focus.activity.ui.shopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_shopping.content_listing
import org.mozilla.focus.R

class ShoppingFragment : Fragment() {

    companion object {
        fun newInstance() = ShoppingFragment()
    }

    private lateinit var viewModel: ShoppingViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_shopping, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ShoppingViewModel::class.java)
        // TODO: Use the ViewModel
        val shoppingItems = ArrayList<String>()
        for (i in 0..100) {
            shoppingItems.add(i.toString())
        }

        content_listing.layoutManager = LinearLayoutManager(context)
        content_listing.adapter = ShoppingAdapter(context!!, shoppingItems)
    }
}
