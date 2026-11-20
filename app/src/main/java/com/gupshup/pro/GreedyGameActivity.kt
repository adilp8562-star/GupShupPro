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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class GreedyGameActivity : AppCompatActivity() {

    private val FIREBASE_DB_URL = "https://gupshuppro-9ab9e-default-rtdb.firebaseio.com"
    private lateinit var userCoinsRef: DatabaseReference

    private var coins = 50000
    private var selectedChip = 100
    private var isBettingOpen = true
    private var isSuperAdmin = false
    private var shortUid = "100001"
    private var nextWinnerIndex = 0

    private lateinit var boxes: List<Button>
    private val bets = IntArray(8) { 0 }
    private val names = listOf("🍅 Tomato", "🌽 Corn", "🥕 Carrot", "🥬 Cabbage", "🍗 Chicken", "🍤 Shrimp", "🐟 Fish", "🥩 Steak")
    private val multipliers = listOf(5, 10, 5, 15, 5, 25, 5, 45)
    private val historyList = mutableListOf("🍅", "🍗", "🥩", "🍤", "🌽", "🐟", "🥕")

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
        coins = prefs.getInt("user_coins", 50000)
        shortUid = prefs.getString("user_uid", "100001") ?: "100001"
        isSuperAdmin = prefs.getBoolean("is_super_admin", false)

        val dbRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).reference
        userCoinsRef = dbRef.child("users").child(shortUid).child("coins")

        userCoinsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val liveCoins = snapshot.getValue(Int::class.java)
                if (liveCoins != null) {
                    coins = liveCoins
                    prefs.edit().putInt("user_coins", coins).apply()
                    updateUI()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

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

        val chips = listOf(chip100 to 100, chip500 to 500, chip1000 to 1000, chip5000 to 5000)
        chips.forEach { (btn, value) ->
            btn?.setOnClickListener {
                selectedChip = value
                chips.forEach { (b, _) ->
                    b?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2E1859"))
                }
                btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFD700"))
                Toast.makeText(this, "Selected Bet: $value Coins", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun placeBet(index: Int) {
        if (!isBettingOpen) {
            Toast.makeText(this, "Wait for next round!", Toast.LENGTH_SHORT).show()
            return
        }
        if (coins < selectedChip) {
            Toast.makeText(this, "Not enough coins!", Toast.LENGTH_SHORT).show()
            return
        }
        coins -= selectedChip
        bets[index] += selectedChip
        syncCoins()
        updateUI()
    }

    private fun startNewRound() {
        isBettingOpen = true
        for (i in 0..7) {
            bets[i] = 0
            boxes[i].backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2A1552"))
        }

        // Weighted Royal Wheel Selection (Pre-determined so Super Admin sees Secret Hint)
        val roll = Random.nextInt(100)
        nextWinnerIndex = when {
            roll < 22 -> 0 // 5x
            roll < 44 -> 2 // 5x
            roll < 64 -> 4 // 5x
            roll < 78 -> 6 // 5x
            roll < 88 -> 1 // 10x
            roll < 94 -> 3 // 15x
            roll < 98 -> 5 // 25x
            else -> 7      // 45x Jackpot
        }

        roundTimer?.cancel()
        roundTimer = object : CountDownTimer(12000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = millisUntilFinished / 1000
                tvTimerStatus.text = if (isSuperAdmin) {
                    "⏳ ${sec}s • 👑 Secret Hint: ${names[nextWinnerIndex]} (${multipliers[nextWinnerIndex]}x)"
                } else {
                    "⏳ Place Your Bets: ${sec}s"
                }
                tvCenterDisplay.text = "🎡\nBETTING"
            }

            override fun onFinish() {
                spinWheel()
            }
        }.start()
    }

    private fun spinWheel() {
        isBettingOpen = false
        tvTimerStatus.text = "🎰 Spinning Royal Wheel..."

        var step = 0
        val totalSteps = 16 + nextWinnerIndex
        val spinRunnable = object : Runnable {
            override fun run() {
                val currentIdx = step % 8
                for (i in 0..7) {
                    boxes[i].backgroundTintList = ColorStateList.valueOf(
                        if (i == currentIdx) Color.parseColor("#FFD700") else Color.parseColor("#2A1552")
                    )
                }
                tvCenterDisplay.text = names[currentIdx]

                if (step < totalSteps) {
                    step++
                    handler.postDelayed(this, (70 + step * 8).toLong())
                } else {
                    announceWinner(nextWinnerIndex)
                }
            }
        }
        handler.post(spinRunnable)
    }

    private fun announceWinner(winIdx: Int) {
        val winName = names[winIdx]
        val mult = multipliers[winIdx]
        val betOnWinner = bets[winIdx]
        val winAmount = betOnWinner * mult

        historyList.add(0, winName.substringBefore(" "))
        if (historyList.size > 10) historyList.removeAt(historyList.size - 1)

        if (winAmount > 0) {
            coins += winAmount
            syncCoins()
            tvCenterDisplay.text = "🎉 WIN!\n+$winAmount"
            Toast.makeText(this, "🎉 $winName (${mult}x) Won! +$winAmount Coins!", Toast.LENGTH_LONG).show()
        } else {
            tvCenterDisplay.text = "$winName\n${mult}x"
        }

        updateUI()
        handler.postDelayed({ startNewRound() }, 3500)
    }

    private fun syncCoins() {
        getSharedPreferences("GupShupPrefs", Context.MODE_PRIVATE)
            .edit().putInt("user_coins", coins).apply()
        userCoinsRef.setValue(coins)
    }

    private fun updateUI() {
        tvCoins.text = "🪙 $coins"
        tvHistory.text = "History: ${historyList.joinToString(" ")}"
        for (i in 0..7) {
            val baseLabel = "${names[i]}\n${multipliers[i]}x"
            boxes[i].text = if (bets[i] > 0) "$baseLabel\n[Bet: ${bets[i]}]" else baseLabel
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        roundTimer?.cancel()
        handler.removeCallbacksAndMessages(null)
    }
}
