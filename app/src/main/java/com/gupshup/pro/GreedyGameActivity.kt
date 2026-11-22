package com.gupshup.pro

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random

class GreedyGameActivity : AppCompatActivity() {

    private val dbUrl = "https://gupshuppro-9ab9e-default-rtdb.firebaseio.com"
    private lateinit var dbRef: DatabaseReference

    private var userName = "Its Meroyal"
    private var userCoins = 500000
    private var selectedChip = 1000L
    private var timer: CountDownTimer? = null

    private val slots = listOf(
        "🍎 Apple", "🍌 Banana", "🍇 Grapes", "🍓 Berry",
        "🍉 Melon", "🍍 Pine", "👑 Crown", "💎 Gem"
    )
    private val slotBets = LongArray(8) { 0L }
    private val slotButtons = mutableListOf<Button>()
    private val chipButtons = mutableListOf<Button>()

    private lateinit var tvTimer: TextView
    private lateinit var tvCoins: TextView
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("GupShupPrefs", Context.MODE_PRIVATE)
        userName = intent.getStringExtra("USER_NAME") ?: prefs.getString("user_name", "Its Meroyal") ?: "Its Meroyal"
        userCoins = intent.getIntExtra("USER_COINS", prefs.getInt("user_coins", 500000))

        dbRef = FirebaseDatabase.getInstance(dbUrl).reference

        buildUi()
        startRoundTimer()
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
            setPadding(18, 28, 18, 28)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14, 12, 14, 12)
            background = box("#1A103C", "#FFD700", 2, 18f)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 }
        }

        tvTimer = TextView(this).apply {
            text = "⏱️ 30s"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFD700"))
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        topBar.addView(tvTimer)

        tvCoins = TextView(this).apply {
            text = "🪙 $userCoins"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#00FFAA"))
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { rightMargin = 8 }
        }
        topBar.addView(tvCoins)

        topBar.addView(Button(this).apply {
            text = "Exit"
            textSize = 11f
            setTextColor(Color.WHITE)
            background = box("#3A1638", "#FF5252", 2, 12f)
            setOnClickListener { finish() }
        })
        col.addView(topBar)

        tvStatus = TextView(this).apply {
            text = "🎰 GIRDY 30s • Place bet & win big!"
            textSize = 13f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(12, 10, 12, 10)
            background = box("#24124F", "#8A5CF6", 2, 14f)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 }
        }
        col.addView(tvStatus)

        slots.chunked(2).forEachIndexed { rowIdx, pair ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8 }
            }
            pair.forEachIndexed { colIdx, label ->
                val idx = rowIdx * 2 + colIdx
                val btn = Button(this).apply {
                    text = "$label\n0"
                    textSize = 12f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    background = box("#1A103C", "#FFD700", 2, 14f)
                    layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                        if (colIdx == 0) rightMargin = 6 else leftMargin = 6
                    }
                    setOnClickListener { placeBet(idx) }
                }
                slotButtons.add(btn)
                row.addView(btn)
            }
            col.addView(row)
        }

        val chips = listOf(100L, 500L, 1000L, 5000L)
        val chipRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = 8; bottomMargin = 12 }
        }

        chips.forEachIndexed { i, amt ->
            val cBtn = Button(this).apply {
                text = "🪙 $amt"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(if (amt == selectedChip) Color.parseColor("#1A0B2E") else Color.parseColor("#FFD700"))
                background = if (amt == selectedChip) box("#FFD700", "#FFFFFF", 2, 12f) else box("#1A103C", "#FFD700", 2, 12f)
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (i < chips.lastIndex) rightMargin = 4
                }
                setOnClickListener {
                    selectedChip = amt
                    refreshChipUi()
                }
            }
            chipButtons.add(cBtn)
            chipRow.addView(cBtn)
        }
        col.addView(chipRow)

        col.addView(Button(this).apply {
            text = "🎲 Spin Now"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A0B2E"))
            background = box("#00FFAA", "#FFFFFF", 2, 16f)
            setOnClickListener { settleRound(Random.nextInt(slots.size)) }
        })

        scroll.addView(col)
        root.addView(scroll)
        setContentView(root)
    }

    private fun refreshChipUi() {
        val chips = listOf(100L, 500L, 1000L, 5000L)
        chipButtons.forEachIndexed { idx, btn ->
            val isSel = chips[idx] == selectedChip
            btn.setTextColor(if (isSel) Color.parseColor("#1A0B2E") else Color.parseColor("#FFD700"))
            btn.background = if (isSel) box("#FFD700", "#FFFFFF", 2, 12f) else box("#1A103C", "#FFD700", 2, 12f)
        }
    }

    private fun placeBet(idx: Int) {
        if (userCoins < selectedChip) {
            Toast.makeText(this, "Not enough coins", Toast.LENGTH_SHORT).show()
            return
        }
        userCoins -= selectedChip.toInt()
        slotBets[idx] += selectedChip
        tvCoins.text = "🪙 $userCoins"
        slotButtons[idx].text = "${slots[idx]}\n🪙 ${slotBets[idx]}"
        getSharedPreferences("GupShupPrefs", Context.MODE_PRIVATE).edit().putInt("user_coins", userCoins).apply()
    }

    private fun startRoundTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(30000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = (millisUntilFinished / 1000L).toInt()
                tvTimer.text = "⏱️ ${sec}s"
            }

            override fun onFinish() {
                settleRound(Random.nextInt(slots.size))
                startRoundTimer()
            }
        }.start()
    }

    private fun settleRound(winIdx: Int) {
        val winSlot = slots[winIdx]
        val won = slotBets[winIdx] * 5L
        if (won > 0L) {
            userCoins += won.toInt()
            tvCoins.text = "🪙 $userCoins"
            getSharedPreferences("GupShupPrefs", Context.MODE_PRIVATE).edit().putInt("user_coins", userCoins).apply()
            dbRef.child("globalWins").push().setValue(
                mapOf(
                    "winner" to userName,
                    "amount" to won,
                    "game" to "Girdy 30s ($winSlot)",
                    "timestamp" to System.currentTimeMillis()
                )
            )
            tvStatus.text = "🎉 Winner: $winSlot! You won 🪙 $won"
        } else {
            tvStatus.text = "🎯 Result: $winSlot • Next round started"
        }

        for (i in slotBets.indices) {
            slotBets[i] = 0L
            slotButtons[i].text = "${slots[i]}\n0"
        }
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}
