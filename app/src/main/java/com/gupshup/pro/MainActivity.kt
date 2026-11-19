package com.gupshup.pro

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var tvCoins: TextView
    private var coins: Int = 5000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestAudioPermission()

        prefs = getSharedPreferences("GupShupPrefs", MODE_PRIVATE)
        coins = prefs.getInt("user_coins", 5000)

        tvCoins = findViewById(R.id.tvCoins)
        val btnDailyBonus = findViewById<Button>(R.id.btnDailyBonus)
        val btnGreedyGame = findViewById<Button>(R.id.btnGreedyGame)
        val etUserName = findViewById<EditText>(R.id.etUserName)
        val etRoomId = findViewById<EditText>(R.id.etRoomId)
        val btnCreateRoom = findViewById<Button>(R.id.btnCreateRoom)
        val btnJoinRoom = findViewById<Button>(R.id.btnJoinRoom)

        updateCoinUI()

        btnDailyBonus.setOnClickListener {
            coins += 1000
            saveCoins()
            Toast.makeText(this, "🎉 +1000 Free Gold Coins मिले!", Toast.LENGTH_SHORT).show()
        }

        btnGreedyGame.setOnClickListener {
            openGreedyMiniGame()
        }

        btnCreateRoom.setOnClickListener {
            val name = etUserName.text.toString().trim().ifEmpty { "Adil_Host" }
            val roomId = etRoomId.text.toString().trim().ifEmpty { "1001" }
            startLiveRoom(name, roomId, true)
        }

        btnJoinRoom.setOnClickListener {
            val name = etUserName.text.toString().trim().ifEmpty { "Guest_${Random.nextInt(100, 999)}" }
            val roomId = etRoomId.text.toString().trim().ifEmpty { "1001" }
            startLiveRoom(name, roomId, false)
        }
    }

    private fun openGreedyMiniGame() {
        val options = arrayOf("🍎 Apple (2x)", "🍋 Lemon (3x)", "🍉 Watermelon (5x)", "👑 Crown Jackpot (10x)")
        val multipliers = arrayOf(2, 3, 5, 10)
        val betAmount = 500

        if (coins < betAmount) {
            Toast.makeText(this, "❌ कम से कम 500 Coins चाहिए! Daily Bonus लें।", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("🎰 Greedy Mini-Game (Bet: 500 Coins)")
            .setItems(options) { _, which ->
                coins -= betAmount
                val winIndex = Random.nextInt(0, 4)
                if (which == winIndex) {
                    val prize = betAmount * multipliers[which]
                    coins += prize
                    Toast.makeText(this, "🔥 JACKPOT! आपने $prize Coins जीते!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "😢 इस बार ${options[winIndex]} आया! फिर से ट्राय करें।", Toast.LENGTH_SHORT).show()
                }
                saveCoins()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startLiveRoom(userName: String, roomId: String, isHost: Boolean) {
        val intent = Intent(this, LiveRoomActivity::class.java).apply {
            putExtra("USER_NAME", userName)
            putExtra("ROOM_ID", roomId)
            putExtra("IS_HOST", isHost)
        }
        startActivity(intent)
    }

    private fun saveCoins() {
        prefs.edit().putInt("user_coins", coins).apply()
        updateCoinUI()
    }

    private fun updateCoinUI() {
        tvCoins.text = "🪙 $coins Coins"
    }

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
    }
}
