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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var tvCoins: TextView
    private lateinit var etUserName: EditText
    private lateinit var etRoomId: EditText
    private lateinit var dbRef: DatabaseReference
    private var coins: Int = 5500

    private val FIREBASE_DB_URL = "https://gupshuppro-9ab9e-default-rtdb.firebaseio.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestAudioPermission()

        prefs = getSharedPreferences("GupShupPrefs", MODE_PRIVATE)
        coins = prefs.getInt("user_coins", 5500)

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
            startActivity(Intent(this, GreedyGameActivity::class.java))
        }

        btnCreateRoom.setOnClickListener {
            val name = etUserName.text.toString().trim().ifEmpty { "Adil_Host" }
            val roomId = etRoomId.text.toString().trim().ifEmpty { "1001" }
            prefs.edit().putString("user_name", name).apply()
            val intent = Intent(this, LiveRoomActivity::class.java)
            intent.putExtra("USER_NAME", name)
            intent.putExtra("ROOM_ID", roomId)
            intent.putExtra("IS_HOST", true)
            startActivity(intent)
        }

        btnJoinRoom.setOnClickListener {
            val name = etUserName.text.toString().trim().ifEmpty { "Guest_User" }
            val roomId = etRoomId.text.toString().trim().ifEmpty { "1001" }
            prefs.edit().putString("user_name", name).apply()
            val intent = Intent(this, LiveRoomActivity::class.java)
            intent.putExtra("USER_NAME", name)
            intent.putExtra("ROOM_ID", roomId)
            intent.putExtra("IS_HOST", false)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        coins = prefs.getInt("user_coins", 5500)
        updateCoinUI()
    }

    private fun getCurrentUserKey(): String {
        val rawName = if (::etUserName.isInitialized) etUserName.text.toString().trim() else ""
        val name = rawName.ifEmpty { prefs.getString("user_name", "Adil_Host") ?: "Adil_Host" }
        return name.replace(Regex("[^a-zA-Z0-9_]"), "_")
    }

    private fun syncCoinsFromFirebase(userKey: String) {
        dbRef.child("users").child(userKey).child("coins")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val cloudCoins = snapshot.getValue(Int::class.java)
                    if (cloudCoins != null) {
                        coins = cloudCoins
                        prefs.edit().putInt("user_coins", coins).apply()
                        updateCoinUI()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
    }
}
