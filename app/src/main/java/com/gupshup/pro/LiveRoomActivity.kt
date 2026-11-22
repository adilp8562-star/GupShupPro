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
import android.widget.EditText
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

class LiveRoomActivity : AppCompatActivity() {

    private val dbUrl = "https://gupshuppro-9ab9e-default-rtdb.firebaseio.com"
    private lateinit var dbRef: DatabaseReference

    private var roomId = "100001"
    private var roomName = "Official VIP Club"
    private var userId = "777777"
    private var userName = "Its Meroyal"
    private var userCoins = 500000
    private var vipLevel = 100

    private lateinit var tvCoins: TextView
    private lateinit var tvChatLog: TextView
    private lateinit var tvGlobalPati: TextView
    private lateinit var tvRocketAnim: TextView
    private var patiAnimator: ObjectAnimator? = null
    private var rocketAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("GupShupPrefs", Context.MODE_PRIVATE)
        roomId = intent.getStringExtra("ROOM_ID") ?: "100001"
        roomName = intent.getStringExtra("ROOM_NAME") ?: "Official VIP Club"
        userId = intent.getStringExtra("USER_ID") ?: prefs.getString("short_uid", "777777") ?: "777777"
        userName = intent.getStringExtra("USER_NAME") ?: prefs.getString("user_name", "Its Meroyal") ?: "Its Meroyal"
        userCoins = intent.getIntExtra("USER_COINS", prefs.getInt("user_coins", 500000))
        vipLevel = intent.getIntExtra("VIP_LEVEL", prefs.getInt("vip_level", 100))

        dbRef = FirebaseDatabase.getInstance(dbUrl).reference

        buildUi()
        listenRoomChat()
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

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 10, 12, 10)
            background = box("#1A103C", "#FFD700", 2, 16f)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 }
        }

        topBar.addView(TextView(this).apply {
            text = "🎙️ $roomName ($roomId)\n👑 $userName • LV.$vipLevel"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFD700"))
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })

        tvCoins = TextView(this).apply {
            text = "🪙 $userCoins"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#00FFAA"))
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { rightMargin = 8 }
        }
        topBar.addView(tvCoins)

        topBar.addView(Button(this).apply {
            text = "Leave"
            textSize = 11f
            setTextColor(Color.WHITE)
            background = box("#3A1638", "#FF5252", 2, 12f)
            setOnClickListener { finish() }
        })
        col.addView(topBar)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 }
        }

        btnRow.addView(Button(this).apply {
            text = "🎰 Girdy 30s"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A0B2E"))
            background = box("#FFD700", "#FFFFFF", 2, 14f)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = 6 }
            setOnClickListener {
                startActivity(Intent(this@LiveRoomActivity, GreedyGameActivity::class.java).apply {
                    putExtra("USER_NAME", userName)
                    putExtra("USER_COINS", userCoins)
                })
            }
        })

        btnRow.addView(Button(this).apply {
            text = "🚀 Rocket (5000 🪙)"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = box("#4C1D95", "#00FFAA", 2, 14f)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = 6 }
            setOnClickListener { sendGiftRocket(5000) }
        })
        col.addView(btnRow)

        tvChatLog = TextView(this).apply {
            text = "💬 Room chat started...\n"
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(12, 12, 12, 12)
            minHeight = 260
            background = box("#140D2E", "#5B21B6", 2, 14f)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 }
        }
        col.addView(tvChatLog)

        val chatBoxRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val etMsg = EditText(this).apply {
            hint = "Type message..."
            textSize = 13f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#A78BFA"))
            setPadding(12, 10, 12, 10)
            background = box("#1A103C", "#FFD700", 2, 14f)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = 8 }
        }
        chatBoxRow.addView(etMsg)

        chatBoxRow.addView(Button(this).apply {
            text = "Send"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A0B2E"))
            background = box("#00FFAA", "#FFFFFF", 2, 14f)
            setOnClickListener {
                val msg = etMsg.text.toString().trim()
                if (msg.isNotEmpty()) {
                    dbRef.child("rooms").child(roomId).child("chat").push().setValue(
                        mapOf(
                            "sender" to userName,
                            "text" to msg,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                    etMsg.setText("")
                }
            }
        })
        col.addView(chatBoxRow)

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

        tvRocketAnim = TextView(this).apply {
            text = "🚀"
            textSize = 44f
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL)
        }
        root.addView(tvRocketAnim)

        setContentView(root)
    }

    private fun sendGiftRocket(cost: Int) {
        if (userCoins < cost) {
            Toast.makeText(this, "Not enough coins", Toast.LENGTH_SHORT).show()
            return
        }
        userCoins -= cost
        tvCoins.text = "🪙 $userCoins"
        getSharedPreferences("GupShupPrefs", Context.MODE_PRIVATE).edit().putInt("user_coins", userCoins).apply()

        dbRef.child("rooms").child(roomId).child("chat").push().setValue(
            mapOf(
                "sender" to userName,
                "text" to "sent a 🚀 Rocket Gift ($cost coins)!",
                "timestamp" to System.currentTimeMillis()
            )
        )
        launchRocket()
    }

    private fun launchRocket() {
        rocketAnimator?.cancel()
        tvRocketAnim.visibility = View.VISIBLE
        val screenH = resources.displayMetrics.heightPixels.toFloat()
        rocketAnimator = ObjectAnimator.ofFloat(tvRocketAnim, "translationY", screenH * 0.7f, -200f).apply {
            duration = 1800L
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tvRocketAnim.visibility = View.GONE
                }
            })
            start()
        }
    }

    private fun listenRoomChat() {
        dbRef.child("rooms").child(roomId).child("chat").limitToLast(25)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val sender = snapshot.child("sender").getValue(String::class.java) ?: "User"
                    val text = snapshot.child("text").getValue(String::class.java) ?: return
                    tvChatLog.append("$sender: $text\n")
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
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
        rocketAnimator?.cancel()
        super.onDestroy()
    }
}
