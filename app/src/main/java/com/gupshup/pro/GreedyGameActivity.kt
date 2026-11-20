package com.gupshup.pro

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class GreedyGameActivity : AppCompatActivity() {

    private var coins = 5500
    private var selectedChip = 100
    private var isBettingOpen = true

    private lateinit var boxes: List<Button>
    private val bets = IntArray(8) { 0 }
    private val names = listOf("🍔", "🍅", "🍕", "🌽", "🍗", "🥕", "🌭", "👑")
    private val multipliers = listOf(5, 10, 5, 15, 5, 25, 5, 45)
    private val historyList = mutableListOf("🍔", "🍕", "🍅", "🍗", "👑", "🥕")

    private lateinit var tvCoins: TextView
    private lateinit var tvTimerStatus: TextView
    private lateinit var tvCenterDisplay: TextView
    private lateinit var tvHistory: TextView

    private var roundTimer: CountDownTimer? = null
    private val handler = Handler(Looper.getMainLooper())

    package com.gupshup.pro

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class GreedyGameActivity : AppCompatActivity() {

    private var coins = 5500
    private var selectedChip = 100
    private var isBettingOpen = true

    private lateinit var boxes: List<Button>
    private val bets = IntArray(8) { 0 }
    private val names = listOf("🍔", "🍅", "🍕", "🌽", "🍗", "🥕", "🌭", "👑")
    private val multipliers = listOf(5, 10, 5, 15, 5, 25, 5, 45)
    private val historyList = mutableListOf("🍔", "🍕", "🍅", "🍗", "👑", "🥕")

    private lateinit var tvCoins: TextView
    private lateinit var tvTimerStatus: TextView
    private lateinit var tvCenterDisplay: TextView
    private lateinit var tvHistory: TextView

    private var roundTimer: CountDownTimer? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_greedy)

        val prefs = getSharedPreferences("GupShupPrefs", Context.MODE_PRIVATE)
        coins = prefs.getInt("user_coins", 5500)

        tvCoins = findViewById(R.id.tvGreedyCoins)
        tvTimerStatus = findViewById(R.id.tvTimerStatus)
        tvCenterDisplay = findViewById(R.id.tvCenterDisplay)
        tvHistory = findViewById(R.id.tvHistory)

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        boxes = listOf(
            findViewById(R.id.box0),
            findViewById(R.id.box1),
            findViewById(R.id.box2),
            findViewById(R.id.box3),
            findViewById(R.id.box4),
            findViewById(R.id.box5),
            findViewById(R.id.box6),
            findViewById(R.id.box7)
        )

        boxes.forEachIndexed { index, button ->
            button.setOnClickListener {
                placeBet(index)
            }
        }

        setupChips()
        updateUI()
        startNewRound()
    }

    private fun setupChips() {
        val chip100 = findViewById<Button>(R.id.chip100)
        val chip500 = findViewById<Button>(R.id.chip500)
        val chip1000 = findViewById<Button>(R.id.chip1000)
        val chip5000 = findViewById<Button>(R.id.chip5000)
        val allChips = listOf(chip100, chip500, chip1000, chip5000)
        val values = listOf(100, 500, 1000, 5000)

        allChips.forEachIndexed { idx, btn ->
            btn.setOnClickListener {
                selectedChip = values[idx]
                allChips.forEach {
                    it.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#321B6B"))
                    it.setTextColor(Color.WHITE)
                }
                btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF9800"))
                btn.setTextColor(Color.BLACK)
            }
        }
    }

    private fun placeBet(index: Int) {
        if (!isBettingOpen) {
            Toast.makeText(this, "⏳ व्हील घूम रहा है! अगले राउंड का इंतज़ार करें", Toast.LENGTH_SHORT).show()
            return
        }
        if (coins < selectedChip) {
            Toast.makeText(this, "❌ पर्याप्त कॉइन्स नहीं हैं!", Toast.LENGTH_SHORT).show()
            return
        }
        coins -= selectedChip
        bets[index] += selectedChip
        saveCoins()
        updateUI()
    }

    private fun startNewRound() {
        isBettingOpen = true
        for (i in 0..7) bets[i] = 0
        resetBoxColors()
        updateUI()

        roundTimer?.cancel()
        roundTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = (millisUntilFinished / 1000) + 1
                tvTimerStatus.text = "⏳ Betting Time: ${sec}s"
                tvCenterDisplay.text = "${sec}s\nPLACE BET"
                tvCenterDisplay.setBackgroundColor(Color.parseColor("#FF6F00"))
            }

            override fun onFinish() {
                isBettingOpen = false
                tvTimerStatus.text = "🎡 Spinning Wheel..."
                tvCenterDisplay.text = "SPIN\n..."
                tvCenterDisplay.setBackgroundColor(Color.parseColor("#D50000"))
                spinWheel()
            }
        }.start()
    }

    private fun spinWheel() {
        val weightedPool = listOf(0, 0, 0, 2, 2, 2, 4, 4, 4, 6, 6, 6, 1, 1, 3, 3, 5, 7)
        val winnerIndex = weightedPool[Random.nextInt(weightedPool.size)]
        val totalSteps = 24 + winnerIndex

        var currentStep = 0
        val spinRunnable = object : Runnable {
            override fun run() {
                resetBoxColors()
                val activeBox = currentStep % 8
                boxes[activeBox].backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFD700"))
                boxes[activeBox].setTextColor(Color.BLACK)

                if (currentStep < totalSteps) {
                    currentStep++
                    val delay = if (currentStep > totalSteps - 6) 220L else 90L
                    handler.postDelayed(this, delay)
                } else {
                    finishRound(winnerIndex)
                }
            }
        }
        handler.post(spinRunnable)
    }

    private fun finishRound(winnerIndex: Int) {
        val wonAmount = bets[winnerIndex] * multipliers[winnerIndex]
        if (wonAmount > 0) {
            coins += wonAmount
            saveCoins()
            tvTimerStatus.text = "🎉 जीते! ${names[winnerIndex]} (${multipliers[winnerIndex]}x) -> +$wonAmount Coins!"
            tvCenterDisplay.text = "WIN!\n+$wonAmount"
            tvCenterDisplay.setBackgroundColor(Color.parseColor("#00C853"))
        } else {
            tvTimerStatus.text = "Result: ${names[winnerIndex]} (${multipliers[winnerIndex]}x) आया!"
            tvCenterDisplay.text = "${names[winnerIndex]}\n${multipliers[winnerIndex]}x"
            tvCenterDisplay.setBackgroundColor(Color.parseColor("#4A148C"))
        }

        historyList.add(0, names[winnerIndex])
        if (historyList.size > 6) historyList.removeAt(historyList.size - 1)
        updateUI()

        handler.postDelayed({
            if (!isFinishing) startNewRound()
        }, 4000)
    }

    private fun resetBoxColors() {
        boxes.forEachIndexed { idx, btn ->
            btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2B1659"))
            btn.setTextColor(if (idx == 7) Color.parseColor("#FFD700") else Color.WHITE)
        }
    }

    private fun updateUI() {
        tvCoins.text = "🪙 $coins"
        boxes.forEachIndexed { idx, btn ->
            btn.text = "${names[idx]} ${multipliers[idx]}x\nBet: ${bets[idx]}"
        }
        tvHistory.text = "Result: " + historyList.joinToString("  ")
    }

    private fun saveCoins() {
        val prefs = getSharedPreferences("GupShupPrefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("user_coins", coins).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        roundTimer?.cancel()
        handler.removeCallbacksAndMessages(null)
    }
}

