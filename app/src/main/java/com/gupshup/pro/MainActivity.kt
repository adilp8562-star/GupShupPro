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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var tvCoins: TextView
    private lateinit var etUserName: EditText
    private lateinit var etRoomId: EditText
    private lateinit var dbRef: DatabaseReference
    private var coins: Int = 5000

    private val FIREBASE_DB_URL = "https://gupshuppro-9ab9e-default-rtdb.firebaseio.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestAudioPermission()

        prefs = getSharedPreferences("GupShupPrefs", MODE_PRIVATE)
        coins = prefs.getInt("user_coins", 5000)

        // Initialize Firebase Realtime Database
        dbRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).reference

        tvCoins = findViewById(R.id.tvCoins)
        val btnDailyBonus = findViewById<Button>(R.id.btnDailyBonus)
        val btnGreedyGame = findViewById<Button>(R.id.btnGreedyGame)
        etUserName = findViewById(R.id.etUserName)
        etRoomId = findViewById(R.id.etRoomId)
        val btnCreateRoom = findViewById<Button>(R.id.btnCreateRoom)
        val btnJoinRoom = findViewById<Button>(R.id.btnJoinRoom)

        val savedName = prefs.getString("user_name", "") ?: ""
        if (savedName.isNotEmpty()) {
            etUserName.setText(savedName)
        }

        updateCoinUI()
        syncCoinsFromFirebase(getCurrentUserKey())

        btnDailyBonus.setOnClickListener {
            coins += 1000
            saveCoins()
            Toast.makeText(this, "🎉 +1000 Free Gold Coins मिले! (Live Synced)", Toast.LENGTH_SHORT).show()
        }

        btnGreedyGame.setOnClickListener {
            openGreedyMiniGame()
        }

        btnCreateRoom.setOnClickListener {
            val name = etUserName.text.toString().trim().ifEmpty { "Adil_Host" }
            val roomId = etRoomId.text.toString().trim().ifEmpty { "1001" }
            prefs.edit().putString("user_name", name).apply()

            // Create / Reset Live Room entry in Firebase
            val roomData = mapOf(
                "roomId" to roomId,
                "hostName" to name,
                "rocketCoins" to 0,
                "rocketTarget" to 10000,
                "treasureUnlocked" to false,
                "status" to "LIVE"
            )
            dbRef.child("rooms").child(roomId).updateChildren(roomData)

            startLiveRoom(name, roomId, true)
        }

        btnJoinRoom.setOnClickListener {
            val name = etUserName.text.toString().trim().ifEmpty { "Guest_${Random.nextInt(100, 999)}" }
            val roomId = etRoomId.text.toString().trim().ifEmpty { "1001" }
            prefs.edit().putString("user_name", name).apply()

            startLiveRoom(name, roomId, false)
        }
    }

    override fun onResume() {
        super.onResume()
        coins = prefs.getInt("user_coins", coins)
        updateCoinUI()
        syncCoinsFromFirebase(getCurrentUserKey())
    }

    private fun getCurrentUserKey(): String {
        val rawName = etUserName.text.toString().trim()
            .ifEmpty { prefs.getString("user_name", "Adil_Host") ?: "Adil_Host" }
        return rawName.replace(Regex("[^a-zA-Z0-9_]"), "_")
    }

    private fun syncCoinsFromFirebase(userKey: String) {
        dbRef.child("users").child(userKey).child("coins")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val cloudCoins = snapshot.getValue(Int::class.java)
                    if (cloudCoins != null) {
                        coins = cloudCoins
                        prefs.edit().putInt("user_coins", coins).apply()
                        updateCoinUI()
                    } else {
                        dbRef.child("users").child(userKey).child("coins").setValue(coins)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Fallback to local coins if network is slow
                }
            })
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
                    Toast.makeText(this, "🎉 JACKPOT! आपने $prize Coins जीते!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "😢 इस बार ${options[winIndex]} आया! फिर से ट्राय करें।", Toast.LENGTH_SHORT).show()
                }
                saveCoins()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startLiveRoom(userName: String, roomId: String, isHost: Boolean) {
        saveCoins()
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
        dbRef.child("users").child(getCurrentUserKey()).child("coins").setValue(coins)
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
