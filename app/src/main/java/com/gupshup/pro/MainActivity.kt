package com.gupshup.pro

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val dbUrl = "https://gupshuppro-9ab9e-default-rtdb.firebaseio.com"
    private lateinit var dbRef: DatabaseReference

    private var userId = "777777"
    private var userName = "Its Meroyal"
    private var userCoins = 500000
    private var vipLevel = 100
    private val officialRoomId = "100001"

    private lateinit var tvCoins: TextView
    private lateinit var tvGlobalPati: TextView
    private var patiAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("GupShupPrefs", Context.MODE_PRIVATE)
        userId = prefs.getString("short_uid", "777777") ?: "777777"
        userName = prefs.getString("user_name", "Its Meroyal") ?: "Its Meroyal"
        userCoins = prefs.getInt("user_coins", 500000)
        vipLevel = prefs.getInt("vip_level", 100)

        dbRef = FirebaseDatabase.getInstance(dbUrl).reference

        buildUi()
        listenGlobalWins()
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("GupShupPrefs", Context.MODE_PRIVATE)
        userCoins = prefs.getInt("user_coins", userCoins)
        if (::tvCoins.isInitialized) {
            tvCoins.text = "🪙 $userCoins"
        }
    }

    private fun box(bg: String, stroke: String, widthPx: Int, radiusPx: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(bg))
            if (widthPx > 0) setStroke(widthPx, Color.parseColor(stroke))
            cornerRadius = radiusPx
        }
    }

    private fun buildUi() {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0B0719"))
        }

        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(-1, -1)
        }

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 76, 18, 28)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14, 12, 14, 12)
            background = box("#1A103C", "#FFD700", 2, 18f)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 14 }
        }

        header.addView(TextView(this).apply {
            text = "👑 $userName (ID: $userId)\nVIP Lv.$vipLevel"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFD700"))
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })

        tvCoins = TextView(this).apply {
            text = "🪙 $userCoins"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#00FFAA"))
        }
        header.addView(tvCoins)
        col.addView(header)

        col.addView(Button(this).apply {
            text = "🎙️ Enter Official Room ($officialRoomId)"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A0B2E"))
            background = box("#FFD700", "#FFFFFF", 2, 16f)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 }
            setOnClickListener {
                startActivity(Intent(this@MainActivity, LiveRoomActivity::class.java).apply {
                    putExtra("ROOM_ID", officialRoomId)
                    putExtra("ROOM_NAME", "Official VIP Club")
                    putExtra("USER_ID", userId)
                    putExtra("USER_NAME", userName)
                    putExtra("USER_COINS", userCoins)
                    putExtra("VIP_LEVEL", vipLevel)
                })
            }
        })

        col.addView(Button(this).apply {
            text = "🎰 Play Girdy 30s Game"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = box("#4C1D95", "#00FFAA", 2, 16f)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 }
            setOnClickListener {
                startActivity(Intent(this@MainActivity, GreedyGameActivity::class.java).apply {
                    putExtra("USER_NAME", userName)
                    putExtra("USER_COINS", userCoins)
                })
            }
        })

        col.addView(Button(this).apply {
            text = "🎁 Claim Daily Bonus (+10000 🪙)"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#00FFAA"))
            background = box("#1A103C", "#00FFAA", 2, 14f)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 }
            setOnClickListener {
                userCoins += 10000
                tvCoins.text = "🪙 $userCoins"
                getSharedPreferences("GupShupPrefs", Context.MODE_PRIVATE).edit().putInt("user_coins", userCoins).apply()
                Toast.makeText(this@MainActivity, "Added +10000 coins!", Toast.LENGTH_SHORT).show()
            }
        })

        col.addView(Button(this).apply {
            text = "📢 Test Win Banner"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFD700"))
            background = box("#1A103C", "#FFD700", 2, 14f)
            setOnClickListener {
                val sampleWin = 5000L * (Random.nextInt(5) + 1)
                dbRef.child("globalWins").push().setValue(
                    mapOf(
                        "winner" to userName,
                        "amount" to sampleWin,
                        "game" to "Girdy 30s",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
        })

        scroll.addView(col)
        root.addView(scroll)

        tvGlobalPati = TextView(this).apply {
            text = ""
            textSize = 12f
            maxLines = 1
            gravity = Gravity.CENTER_VERTICAL
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A0B2E"))
            background = box("#FFD700", "#FFFFFF", 2, 14f)
            setPadding(20, 8, 20, 8)
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(-2, -2).apply {
                topMargin = 18
            }
        }
        root.addView(tvGlobalPati)

        setContentView(root)
    }

    private fun listenGlobalWins() {
        val openedAt = System.currentTimeMillis()
        dbRef.child("globalWins").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val ts = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                if (ts < openedAt - 2000L) return
                val winner = snapshot.child("winner").getValue(String::class.java) ?: return
                val amount = snapshot.child("amount").getValue(Long::class.java) ?: return
                val game = snapshot.child("game").getValue(String::class.java) ?: "Girdy 30s"
                showGlobalPati("👑 GUPSHUP PRO • MEGA WIN! • $winner hit $amount COINS in $game! 🎉")
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showGlobalPati(msg: String) {
        patiAnimator?.cancel()
        tvGlobalPati.text = msg
        tvGlobalPati.visibility = View.VISIBLE
        val screenW = resources.displayMetrics.widthPixels.toFloat()
        tvGlobalPati.measure(0, 0)
        val textW = tvGlobalPati.measuredWidth.toFloat().coerceAtLeast(300f)

        patiAnimator = ObjectAnimator.ofFloat(tvGlobalPati, "translationX", screenW, -textW - 30f).apply {
            duration = 8000L
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tvGlobalPati.visibility = View.GONE
                }
            })
            start()
        }
    }

    override fun onDestroy() {
        patiAnimator?.cancel()
        super.onDestroy()
    }
}
