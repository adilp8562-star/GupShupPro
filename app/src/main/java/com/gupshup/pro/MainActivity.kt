package com.gupshup.pro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val FIREBASE_DB_URL = "https://gupshuppro-9ab9e-default-rtdb.firebaseio.com"
    private lateinit var tvCoins: TextView
    private var currentCoins = 50000
    private var shortUid = "100001"
    private var userName = "Royal Host"
    private var isSuperAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestAudioPermission()

        val prefs = getSharedPreferences("GupShupPrefs", MODE_PRIVATE)
        shortUid = prefs.getString("user_uid", "100001") ?: "100001"
        userName = prefs.getString("user_name", "Royal Host") ?: "Royal Host"
        currentCoins = prefs.getInt("user_coins", 50000)
        isSuperAdmin = prefs.getBoolean("is_super_admin", false)

        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val tvUserUid = findViewById<TextView>(R.id.tvUserUid)
        tvCoins = findViewById(R.id.tvCoins)
        val layoutSuperAdmin = findViewById<LinearLayout>(R.id.layoutSuperAdmin)

        tvUserName.text = if (isSuperAdmin) "👑 $userName" else userName
        tvUserUid.text = if (isSuperAdmin) "ID: $shortUid • SUPER ADMIN" else "ID: $shortUid • Royal VIP"
        tvCoins.text = "🪙 $currentCoins"

        if (isSuperAdmin) {
            layoutSuperAdmin.visibility = View.VISIBLE
        }

        if (prefs.getBoolean("show_welcome_popup", false)) {
            prefs.edit().putBoolean("show_welcome_popup", false).apply()
            AlertDialog.Builder(this)
                .setTitle("🎉 Welcome to GupShup Live!")
                .setMessage("Congratulations! Your Permanent ID ($shortUid) is ready.\n\n• 🪙 50,000 Free Gold Coins added\n• 👑 3-Day Golden VIP Frame unlocked!")
                .setPositiveButton("Awesome!", null)
                .show()
        }

        val dbRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).reference
        dbRef.child("users").child(shortUid).child("coins")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val liveCoins = snapshot.getValue(Int::class.java)
                    if (liveCoins != null) {
                        currentCoins = liveCoins
                        tvCoins.text = "🪙 $currentCoins"
                        prefs.edit().putInt("user_coins", currentCoins).apply()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Super Admin: Send / Deduct Coins to any 6-Digit User ID
        findViewById<Button>(R.id.btnAdminSendCoins).setOnClickListener {
            val targetUid = findViewById<EditText>(R.id.etTargetUid).text.toString().trim()
            val amountStr = findViewById<EditText>(R.id.etAdminCoins).text.toString().trim()
            val amount = amountStr.toIntOrNull()

            if (targetUid.length < 6 || amount == null || amount == 0) {
                Toast.makeText(this, "सही 6-Digit User ID और Coins लिखें", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val targetRef = dbRef.child("users").child(targetUid)
            targetRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    if (!snap.exists()) {
                        Toast.makeText(this@MainActivity, "User ID $targetUid नहीं मिला!", Toast.LENGTH_SHORT).show()
                        return
                    }
                    val oldCoins = snap.child("coins").getValue(Int::class.java) ?: 0
                    val updated = (oldCoins + amount).coerceAtLeast(0)
                    targetRef.child("coins").setValue(updated)
                    Toast.makeText(this@MainActivity, "✅ ID $targetUid के Coins अब $updated हो गए!", Toast.LENGTH_LONG).show()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }

        // Daily Bonus (+2,000 Coins once per day)
        findViewById<Button>(R.id.btnDailyBonus).setOnClickListener {
            val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val lastClaim = prefs.getString("last_daily_bonus", "")
            if (lastClaim == today) {
                Toast.makeText(this, "आज का Daily Bonus पहले ही ले चुके हो!", Toast.LENGTH_SHORT).show()
            } else {
                val updated = currentCoins + 2000
                prefs.edit().putString("last_daily_bonus", today).putInt("user_coins", updated).apply()
                dbRef.child("users").child(shortUid).child("coins").setValue(updated)
                Toast.makeText(this, "🎁 +2,000 Free Gold Coins मिल गए!", Toast.LENGTH_SHORT).show()
            }
        }

        // Open Royal Greedy Game
        findViewById<Button>(R.id.btnGreedyGame).setOnClickListener {
            startActivity(Intent(this, GreedyGameActivity::class.java))
        }

        // Start Host Room
        findViewById<Button>(R.id.btnCreateRoom).setOnClickListener {
            val roomId = findViewById<EditText>(R.id.etRoomId).text.toString().trim().ifEmpty { "8888" }
            val intent = Intent(this, LiveRoomActivity::class.java)
            intent.putExtra("USER_NAME", userName)
            intent.putExtra("USER_UID", shortUid)
            intent.putExtra("ROOM_ID", roomId)
            intent.putExtra("IS_HOST", true)
            startActivity(intent)
        }

        // Join Party Room
        findViewById<Button>(R.id.btnJoinRoom).setOnClickListener {
            val roomId = findViewById<EditText>(R.id.etRoomId).text.toString().trim().ifEmpty { "8888" }
            val intent = Intent(this, LiveRoomActivity::class.java)
            intent.putExtra("USER_NAME", userName)
            intent.putExtra("USER_UID", shortUid)
            intent.putExtra("ROOM_ID", roomId)
            intent.putExtra("IS_HOST", false)
            startActivity(intent)
        }

        // Logout / Switch Google Account
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener {
                prefs.edit().clear().apply()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
    }
}
