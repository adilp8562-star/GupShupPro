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

    private lateinit var tabParty: View
    private lateinit var tabGames: View
    private lateinit var tabMe: View
    private lateinit var navParty: TextView
    private lateinit var navGames: TextView
    private lateinit var navMe: TextView

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

        tabParty = findViewById(R.id.tabParty)
        tabGames = findViewById(R.id.tabGames)
        tabMe = findViewById(R.id.tabMe)
        navParty = findViewById(R.id.navParty)
        navGames = findViewById(R.id.navGames)
        navMe = findViewById(R.id.navMe)

        val layoutSuperAdmin = findViewById<LinearLayout>(R.id.layoutSuperAdmin)
        if (isSuperAdmin) {
            layoutSuperAdmin.visibility = View.VISIBLE
        }

        refreshProfileUI()

        val dbRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).reference

        // Live sync user profile & coins from Firebase
        dbRef.child("users").child(shortUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val c = snapshot.child("coins").getValue(Int::class.java)
                if (c != null) {
                    currentCoins = c
                    prefs.edit().putInt("user_coins", currentCoins).apply()
                }
                snapshot.child("name").getValue(String::class.java)?.let {
                    if (it.isNotEmpty()) {
                        userName = it
                        prefs.edit().putString("user_name", userName).apply()
                    }
                }
                snapshot.child("bio").getValue(String::class.java)?.let {
                    if (it.isNotEmpty()) {
                        userBio = it
                        prefs.edit().putString("user_bio", userBio).apply()
                    }
                }
                snapshot.child("gender").getValue(String::class.java)?.let {
                    if (it.isNotEmpty()) {
                        userGender = it
                        prefs.edit().putString("user_gender", userGender).apply()
                    }
                }
                snapshot.child("avatar").getValue(String::class.java)?.let {
                    if (it.isNotEmpty()) {
                        userAvatar = it
                        prefs.edit().putString("user_avatar", userAvatar).apply()
                    }
                }
                refreshProfileUI()
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // Live Rooms Feed (Haza Home Lobby)
        dbRef.child("rooms").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                liveRoomsContainer.removeAllViews()
                var count = 0
                for (roomSnap in snapshot.children) {
                    val rId = roomSnap.key ?: continue
                    val rTitle = roomSnap.child("title").getValue(String::class.java) ?: "Royal Party Room"
                    val rHost = roomSnap.child("host").getValue(String::class.java) ?: "VIP Host"
                    val rTopic = roomSnap.child("topic").getValue(String::class.java) ?: "Music & Chill 🎶"
                    count++

                    val card = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundColor(Color.parseColor("#1E1736"))
                        setPadding(32, 28, 32, 28)
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.setMargins(0, 0, 0, 22)
                        layoutParams = params
                    }

                    val tvTitle = TextView(this@MainActivity).apply {
                        text = "🎙️ $rTitle"
                        textSize = 17f
                        setTextColor(Color.WHITE)
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    }

                    val tvSub = TextView(this@MainActivity).apply {
                        text = "👑 $rHost  •  $rTopic  •  ID: $rId"
                        textSize = 12f
                        setTextColor(Color.parseColor("#FFD700"))
                        setPadding(0, 8, 0, 0)
                    }

                    card.addView(tvTitle)
                    card.addView(tvSub)
                    card.setOnClickListener { openRoom(rId, false) }
                    liveRoomsContainer.addView(card)
                }

                if (count == 0) {
                    val emptyTv = TextView(this@MainActivity).apply {
                        text = "✨ No live rooms right now.\nTap '+ Create Room' above to start your own Haza Voice Party!"
                        textSize = 14f
                        setTextColor(Color.parseColor("#B8B0D0"))
                        setPadding(24, 48, 24, 48)
                    }
                    liveRoomsContainer.addView(emptyTv)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // Bottom Navigation
        navParty.setOnClickListener { selectTab(0) }
        navGames.setOnClickListener { selectTab(1) }
        navMe.setOnClickListener { selectTab(2) }
        selectTab(0)

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
                Toast.makeText(this, "Today's 10,000 Coins already claimed! Come back tomorrow.", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@MainActivity, "✅ Updated UID $targetId balance to $updated Coins!", Toast.LENGTH_SHORT).show()
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
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        val etTitle = EditText(this).apply {
            hint = "Room Name (e.g. Midnight Chill & Music)"
            setText("$userName's Party")
        }
        val etTopic = EditText(this).apply {
            hint = "Topic (e.g. Singing / Friendship / Games)"
            setText("Music & Chill 🎶")
        }
        layout.addView(etTitle)
        layout.addView(etTopic)

        AlertDialog.Builder(this)
            .setTitle("🎙️ Create Haza Voice Room")
            .setView(layout)
            .setPositiveButton("Go Live") { _, _ ->
                val newRoomId = Random.nextInt(100000, 999999).toString()
                val title = etTitle.text.toString().trim().ifEmpty { "$userName's Party" }
                val topic = etTopic.text.toString().trim().ifEmpty { "Music & Chill 🎶" }
                val roomData = mapOf(
                    "title" to title,
                    "host" to userName,
                    "hostUid" to shortUid,
                    "topic" to topic
                )
                dbRef.child("rooms").child(newRoomId).setValue(roomData)
                openRoom(newRoomId, true)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditProfileDialog(dbRef: com.google.firebase.database.DatabaseReference) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 12)
        }
        val etName = EditText(this).apply {
            hint = "Display Name"
            setText(userName)
        }
        val etBio = EditText(this).apply {
            hint = "Profile Bio"
            setText(userBio)
        }
        val etAvatar = EditText(this).apply {
            hint = "VIP Avatar Emoji (e.g. 👑, 🦁, 🔥, 💎, 👸)"
            setText(userAvatar)
        }
        val rgGender = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
        }
        val rbMale = RadioButton(this).apply {
            text = "♂️ Male"
            id = View.generateViewId()
        }
        val rbFemale = RadioButton(this).apply {
            text = "♀️ Female"
            id = View.generateViewId()
        }
        rgGender.addView(rbMale)
        rgGender.addView(rbFemale)
        if (userGender == "Female") rbFemale.isChecked = true else rbMale.isChecked = true

        layout.addView(etName)
        layout.addView(etBio)
        layout.addView(etAvatar)
        layout.addView(rgGender)

        AlertDialog.Builder(this)
            .setTitle("✏️ Edit VIP Profile")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newName = etName.text.toString().trim().ifEmpty { userName }
                val newBio = etBio.text.toString().trim().ifEmpty { userBio }
                val newAvatar = etAvatar.text.toString().trim().ifEmpty { "👑" }
                val newGender = if (rbFemale.isChecked) "Female" else "Male"

                userName = newName
                userBio = newBio
                userAvatar = newAvatar
                userGender = newGender

                val prefs = getSharedPreferences("GupShupPrefs", MODE_PRIVATE)
                prefs.edit()
                    .putString("user_name", userName)
                    .putString("user_bio", userBio)
                    .putString("user_avatar", userAvatar)
                    .putString("user_gender", userGender)
                    .apply()

                val updates = mapOf(
                    "name" to userName,
                    "bio" to userBio,
                    "avatar" to userAvatar,
                    "gender" to userGender
                )
                dbRef.child("users").child(shortUid).updateChildren(updates)
                refreshProfileUI()
                Toast.makeText(this, "✅ Profile Updated!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun selectTab(index: Int) {
        tabParty.visibility = if (index == 0) View.VISIBLE else View.GONE
        tabGames.visibility = if (index == 1) View.VISIBLE else View.GONE
        tabMe.visibility = if (index == 2) View.VISIBLE else View.GONE

        navParty.setTextColor(if (index == 0) Color.parseColor("#FFD700") else Color.parseColor("#8E86AA"))
        navGames.setTextColor(if (index == 1) Color.parseColor("#FFD700") else Color.parseColor("#8E86AA"))
        navMe.setTextColor(if (index == 2) Color.parseColor("#FFD700") else Color.parseColor("#8E86AA"))
    }

    private fun openRoom(roomId: String, isHost: Boolean) {
        val intent = Intent(this, LiveRoomActivity::class.java)
        intent.putExtra("ROOM_ID", roomId)
        intent.putExtra("IS_HOST", isHost)
        startActivity(intent)
    }

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
    }
}
