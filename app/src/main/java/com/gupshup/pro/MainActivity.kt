package com.gupshup.pro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val FIREBASE_DB_URL = "https://gupshuppro-9ab9e-default-rtdb.firebaseio.com"
    private lateinit var tvCoinsTop: TextView
    private lateinit var tvCoinsMe: TextView
    private lateinit var tvUserNameMe: TextView
    private lateinit var tvUserBioMe: TextView
    private lateinit var tvUserAvatarMe: TextView
    private lateinit var tvUserUidMe: TextView
    private lateinit var liveRoomsContainer: LinearLayout

    private var currentCoins = 50000
    private var shortUid = "100001"
    private var userName = "Royal Host"
    private var userBio = "Welcome to my Royal Voice Party! 🎙️👑"
    private var userGender = "Male"
    private var userAvatar = "👑"
    private var isSuperAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestAudioPermission()

        val prefs = getSharedPreferences("GupShupPrefs", MODE_PRIVATE)
        shortUid = prefs.getString("user_uid", "100001") ?: "100001"
        userName = prefs.getString("user_name", "Royal Host") ?: "Royal Host"
        userBio = prefs.getString("user_bio", "Welcome to my Royal Voice Party! 🎙️👑") ?: "Welcome to my Royal Voice Party! 🎙️👑"
        userGender = prefs.getString("user_gender", "Male") ?: "Male"
        userAvatar = prefs.getString("user_avatar", "👑") ?: "👑"
        currentCoins = prefs.getInt("user_coins", 50000)
        isSuperAdmin = prefs.getBoolean("is_super_admin", false)

        tvCoinsTop = findViewById(R.id.tvCoinsTop)
        tvCoinsMe = findViewById(R.id.tvCoinsMe)
        tvUserNameMe = findViewById(R.id.tvUserNameMe)
        tvUserBioMe = findViewById(R.id.tvUserBioMe)
        tvUserAvatarMe = findViewById(R.id.tvUserAvatarMe)
        tvUserUidMe = findViewById(R.id.tvUserUidMe)
        liveRoomsContainer = findViewById(R.id.liveRoomsContainer)

        val layoutSuperAdmin = findViewById<LinearLayout>(R.id.layoutSuperAdmin)
        if (isSuperAdmin) {
            layoutSuperAdmin.visibility = View.VISIBLE
        }

        refreshProfileUI()

        if (prefs.getBoolean("show_welcome_popup", false)) {
            prefs.edit().putBoolean("show_welcome_popup", false).apply()
            AlertDialog.Builder(this)
                .setTitle("🎉 Welcome to GupShup Live!")
                .setMessage("Your Permanent ID ($shortUid) is active!\n\n🪙 50,000 Free Welcome Coins added to your wallet!")
                .setPositiveButton("Let's Party!", null)
                .show()
        }

        val dbRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).reference

        // Sync User Data & Coins Live from Firebase
        dbRef.child("users").child(shortUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.child("coins").getValue(Int::class.java)?.let {
                    currentCoins = it
                    prefs.edit().putInt("user_coins", currentCoins).apply()
                }
                snapshot.child("name").getValue(String::class.java)?.let {
                    if (it.isNotEmpty()) {
                        userName = it
                        prefs.edit().putString("user_name", userName).apply()
                    }
                }
                snapshot.child("bio").getValue(String::class.java)?.let {
                    userBio = it
                    prefs.edit().putString("user_bio", userBio).apply()
                }
                snapshot.child("gender").getValue(String::class.java)?.let {
                    userGender = it
                    prefs.edit().putString("user_gender", userGender).apply()
                }
                snapshot.child("avatar").getValue(String::class.java)?.let {
                    userAvatar = it
                    prefs.edit().putString("user_avatar", userAvatar).apply()
                }
                refreshProfileUI()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Load Live Active Rooms in Party Tab (Haza Style)
        loadLiveRooms(dbRef)

        // Bottom Navigation Tabs
        val tabPartyView = findViewById<View>(R.id.tabPartyView)
        val tabGamesView = findViewById<View>(R.id.tabGamesView)
        val tabMeView = findViewById<View>(R.id.tabMeView)

        val navParty = findViewById<LinearLayout>(R.id.navParty)
        val navGames = findViewById<LinearLayout>(R.id.navGames)
        val navMe = findViewById<LinearLayout>(R.id.navMe)

        val txtNavParty = findViewById<TextView>(R.id.txtNavParty)
        val txtNavGames = findViewById<TextView>(R.id.txtNavGames)
        val txtNavMe = findViewById<TextView>(R.id.txtNavMe)

        fun selectTab(index: Int) {
            tabPartyView.visibility = if (index == 0) View.VISIBLE else View.GONE
            tabGamesView.visibility = if (index == 1) View.VISIBLE else View.GONE
            tabMeView.visibility = if (index == 2) View.VISIBLE else View.GONE

            txtNavParty.setTextColor(if (index == 0) Color.parseColor("#FFD700") else Color.parseColor("#8E84A8"))
            txtNavGames.setTextColor(if (index == 1) Color.parseColor("#FFD700") else Color.parseColor("#8E84A8"))
            txtNavMe.setTextColor(if (index == 2) Color.parseColor("#FFD700") else Color.parseColor("#8E84A8"))
        }

        navParty.setOnClickListener { selectTab(0) }
        navGames.setOnClickListener { selectTab(1) }
        navMe.setOnClickListener { selectTab(2) }

        // Create Room Button (Haza Party Header)
        findViewById<Button>(R.id.btnCreateRoom).setOnClickListener {
            showCreateRoomDialog(dbRef)
        }

        // Quick Room Join by ID
        val etRoomId = findViewById<EditText>(R.id.etRoomId)
        findViewById<Button>(R.id.btnJoinRoom).setOnClickListener {
            val roomId = etRoomId.text.toString().trim()
            if (roomId.isEmpty()) {
                Toast.makeText(this, "Enter a Room ID!", Toast.LENGTH_SHORT).show()
            } else {
                openRoom(roomId, false)
            }
        }

        // Daily Bonus (10,000 Coins - Bigger than Haza!)
        findViewById<Button>(R.id.btnDailyBonus).setOnClickListener {
            val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val lastClaim = prefs.getString("last_daily_claim", "")
            if (lastClaim == today) {
                Toast.makeText(this, "🎁 Today's 10,000 Coins already claimed! Come back tomorrow.", Toast.LENGTH_SHORT).show()
            } else {
                val newBalance = currentCoins + 10000
                dbRef.child("users").child(shortUid).child("coins").setValue(newBalance)
                prefs.edit().putString("last_daily_claim", today).apply()
                Toast.makeText(this, "🎉 Claimed +10,000 Free Daily Gold Coins!", Toast.LENGTH_LONG).show()
            }
        }

        // Open Royal Greedy Game
        findViewById<Button>(R.id.btnOpenGreedy).setOnClickListener {
            startActivity(Intent(this, GreedyGameActivity::class.java))
        }

        // Edit Profile Button (Name / Bio / Gender / Avatar)
        findViewById<Button>(R.id.btnEditProfile).setOnClickListener {
            showEditProfileDialog(dbRef)
        }

        // Super Admin Send/Deduct Coins
        val etTargetUid = findViewById<EditText>(R.id.etTargetUid)
        val etSendCoins = findViewById<EditText>(R.id.etSendCoins)
        findViewById<Button>(R.id.btnAdminSendCoins).setOnClickListener {
            val targetId = etTargetUid.text.toString().trim()
            val amountStr = etSendCoins.text.toString().trim()
            val amount = amountStr.toIntOrNull()
            if (targetId.isEmpty() || amount == null) {
                Toast.makeText(this, "Enter valid 6-digit UID & Coin amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val targetRef = dbRef.child("users").child(targetId).child("coins")
            targetRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    val oldCoins = snap.getValue(Int::class.java) ?: 0
                    val updated = (oldCoins + amount).coerceAtLeast(0)
                    targetRef.setValue(updated)
                    Toast.makeText(this@MainActivity, "✅ Updated UID $targetId balance to$updated Coins!", Toast.LENGTH_LONG).show()
                    etSendCoins.setText("")
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }

        // Logout / Switch Google Account
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            prefs.edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun refreshProfileUI() {
        tvCoinsTop.text = "🪙 $currentCoins"
        tvCoinsMe.text = "🪙 $currentCoins"
        tvUserAvatarMe.text = userAvatar
        tvUserNameMe.text = if (isSuperAdmin) "👑 $userName" else userName
        val genderBadge = if (userGender == "Female") "♀️ Female" else "♂️ Male"
        val roleText = if (isSuperAdmin) "SUPER ADMIN" else "ROYAL VIP"
        tvUserUidMe.text = "ID: $shortUid • $genderBadge • $roleText"
        tvUserBioMe.text = userBio
    }

    private fun showCreateRoomDialog(dbRef: com.google.firebase.database.DatabaseReference) {
