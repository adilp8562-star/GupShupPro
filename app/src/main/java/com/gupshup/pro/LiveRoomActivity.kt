package com.gupshup.pro

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random

class LiveRoomActivity : AppCompatActivity() {

    private val dbUrl = "https://gupshuppro-9ab9e-default-rtdb.firebaseio.com"
    private lateinit var dbRef: DatabaseReference

    private var roomId = "100001"
    private var roomName = "Official VIP Club"
    private var userId = "777777"
    private var userName = "Its Meroyal"
    private var userCoins = 500000
    private var vipLevel = 100
    private var rocketLevel = 1
    private var progress = 20

    private var soundPool: SoundPool? = null
    private var rocketSoundId = 0
    private var soundReady = false
    private var patiAnim: ObjectAnimator? = null

    private lateinit var tvCoins: TextView
    private lateinit var tvRocket: TextView
    private lateinit var rocketBox: LinearLayout
    private lateinit var tvPati: TextView

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

        setupSound()
        buildRoomUi()
        listenGlobalWins()
    }

    private fun box(bg: String, stroke: String, widthPx: Int, radiusPx: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(bg))
            if (widthPx > 0) setStroke(widthPx, Color.parseColor(stroke))
            cornerRadius = radiusPx
        }
    }

    private fun setupSound() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attrs)
            .build()
        val resId = resources.getIdentifier("rocket_launch", "raw", packageName)
        if (resId != 0) {
            rocketSoundId = soundPool?.load(this, resId, 1) ?: 0
            soundPool?.setOnLoadCompleteListener { _, _, status ->
                if (status == 0) soundReady = true
            }
        }
    }

    private fun playLaunchSound() {
        if (soundReady && rocketSoundId != 0) {
            soundPool?.play(rocketSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        } else {
            try {
                ToneGenerator(AudioManager.STREAM_MUSIC, 80).startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
            } catch (_: Exception) {
            }
        }
    }

    private fun buildRoomUi() {
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
            setPadding(14, 10, 14, 10)
            background = box("#1A103C", "#FFD700", 2, 18f)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 }
        }

        topBar.addView(TextView(this).apply {
            text = "👑 $roomName ($roomId)\n$userName • Lv.$vipLevel"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFD700"))
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })

        tvCoins = TextView(this).apply {
            text = "🪙 $userCoins"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#00FFAA"))
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { rightMargin = 8 }
        }
        topBar.addView(tvCoins)

        topBar.addView(Button(this).apply {
            text = "Exit"
            textSize = 10f
            setTextColor(Color.WHITE)
            background = box("#3A1638", "#FF5252", 2, 12f)
            setOnClickListener { finish() }
        })
        col.addView(topBar)

        rocketBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(18, 24, 18, 24)
            background = box("#24124F", "#FFD700", 3, 24f)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 }
        }

        tvRocket = TextView(this).apply {
            text = "🚀 AURORA VIPER  •  Lv.$rocketLevel  •  $progress%"
            textSize = 15f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFD700"))
        }
        rocketBox.addView(tvRocket)

        rocketBox.addView(Button(this).apply {
            text = "🚀 Launch Rocket (🪙 10000)"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A0B2E"))
            background = box("#FFD700", "#FFFFFF", 2, 14f)
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { topMargin = 12 }
            setOnClickListener { launchRocket() }
        })
        col.addView(rocketBox)

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 }
        }

        row1.addView(Button(this).apply {
            text = "🎁 Gifts"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFD700"))
            background = box("#1A103C", "#FFD700", 2, 14f)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = 6 }
            setOnClickListener { showGiftDialog() }
        })

        row1.addView(Button(this).apply {
            text = "🍀 Lucky Gift"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#00FFAA"))
            background = box("#1A103C", "#00FFAA", 2, 14f)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { left
                
