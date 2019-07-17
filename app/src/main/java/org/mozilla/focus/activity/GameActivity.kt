package org.mozilla.focus.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.mozilla.focus.R
import org.mozilla.focus.activity.ui.game.GameFragment

class GameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, GameFragment.newInstance())
                .commitNow()
        }

        Log.d("GameActivity", "onCreate()")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("GameActivity", "onNewIntent()")
    }

    override fun onStart() {
        super.onStart()
        Log.d("GameActivity", "onStart()")
    }

    override fun onResume() {
        super.onResume()
        Log.d("GameActivity", "onResume()")
    }

    override fun onPause() {
        super.onPause()
        Log.d("GameActivity", "onPause()")
    }

    override fun onStop() {
        super.onStop()
        Log.d("GameActivity", "onStop()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("GameActivity", "onDestroy()")
    }

}
