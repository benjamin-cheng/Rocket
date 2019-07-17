package org.mozilla.focus.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import org.mozilla.focus.R
import org.mozilla.focus.activity.ui.shopping.ShoppingFragment

class ShoppingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ShoppingFragment.newInstance())
                .commitNow()
        }
        Log.d("ShoppingActivity", "onCreate()")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("ShoppingActivity", "onNewIntent()")
    }

    override fun onStart() {
        super.onStart()
        Log.d("ShoppingActivity", "onStart()")
    }

    override fun onResume() {
        super.onResume()
        Log.d("ShoppingActivity", "onResume()")
    }

    override fun onPause() {
        super.onPause()
        Log.d("ShoppingActivity", "onPause()")
    }

    override fun onStop() {
        super.onStop()
        Log.d("ShoppingActivity", "onStop()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ShoppingActivity", "onDestroy()")
    }

}
