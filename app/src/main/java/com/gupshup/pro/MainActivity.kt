package com.gupshup.pro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
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
import com.google.firebase.database.DatabaseReference
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

    private var currentCoins = 200000
    private var shortUid = "100001"
    private var userName = "Royal Host"
    private var userBio = "Welcome to my Royal Voice Party! 👑✨"
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
        userBio = prefs.getString("user_bio", "Welcome to my Royal Voice Party! 👑✨") ?: "Welcome to my Royal Voice Party! 👑✨"
        userGender = prefs.getString("user_gender", "Male") ?: "Male"
        userAvatar = prefs.getString("user_avatar", "👑") ?: "👑"
        currentCoins = prefs.getInt("user_coins", 200000)
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

        refreshProfileUI()

        val dbRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).reference

        // Live sync user coins & profile from Firebase
        dbRef.child("users").child(shortUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val fbCoins = snapshot.child("coins").getValue(Int::class.java)
                    if (fbCoins != null) {
                        currentCoins = fbCoins
                        prefs.edit().putInt("user_coins", currentCoins).apply()
                    }
                    userName = snapshot.child("name").getValue(String::class.java) ?: userName
                    userBio = snapshot.child("bio").getValue(String::class.java) ?: userBio
                    userAvatar = snapshot.child("avatar").getValue(String::class.java) ?: userAvatar
                    userGender = snapshot.child("gender").getValue(String::class.java) ?: userGender
                    isSuperAdmin = snapshot.child("isSuperAdmin").getValue(Boolean::class.java) ?: isSuperAdmin

                    findViewById<View>(R.id.layoutSuperAdmin).visibility =
                        if (isSuperAdmin) View.VISIBLE else View.GONE

                    refreshProfileUI()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // Top Bar & Bottom Navigation
        findViewById<TextView>(R.id.tvTopMe).setOnClickListener { selectTab(2) }
        navParty.setOnClickListener { selectTab(0) }
        navGames.setOnClickListener { selectTab(1) }
        navMe.setOnClickListener { selectTab(2) }

        findViewById<TextView>(R.id.btnTopSearch).setOnClickListener {
            val etRoom = findViewById<EditText>(R.id.etRoomIdInput)
            etRoom.requestFocus()
            Toast.makeText(this, "🔍 Type 6-digit Room ID below to join!", Toast.LENGTH_SHORT).show()
        }

        // Create Room (+House icon)
        findViewById<View>(R.id.btnCreateRoom).setOnClickListener {
            showCreateRoomDialog(dbRef)
        }

        // Join Room by 6-digit ID
        findViewById<Button>(R.id.btnJoinRoom).setOnClickListener {
            val roomId = findViewById<EditText>(R.id.etRoomIdInput).text.toString().trim()
            if (roomId.length < 4) {
                Toast.makeText(this, "Enter valid Room ID!", Toast.LENGTH_SHORT).show()
            } else {
                openRoom(roomId, false)
            }
        }

        // Mega Events Clicks
        findViewById<View>(R.id.bannerWeeklyStar).setOnClickListener {
            showEventPopup(
                "✨ WEEKLY STAR CARNIVAL ✨",
                "🏆 #1 Royal Star: 5,00,000 Coins + Dragon Frame\n🥈 #2 Party Queen: 2,50,000 Coins + Crown\n🥉 #3 Rising Host: 1,00,000 Coins\n\nSend luxury gifts in any room to climb the Weekly Star leaderboard!"
            )
        }

        findViewById<View>(R.id.cardRanking).setOnClickListener {
            showEventPopup(
                "🏆 TOP ROYAL RANKING",
                "1. 👑 $userName (ID: $shortUid) — 🪙 $currentCoins\n2. 🇮🇳 Mahi ✨ — 🪙 8,50,000\n3. 🇮🇳 THALAPATI — 🪙 6,20,000\n4. 🇮🇳 Ira 🦋 — 🪙 4,90,000"
            )
        }

        findViewById<View>(R.id.cardFamily).setOnClickListener {
            showEventPopup(
                "🏰 ROYAL FAMILY CLAN",
                "Join or create your Royal Family!\n• Family Badge on profile\n• Weekly 10,00,000 Coin Family Treasury\n• Exclusive Family PK Battles!"
            )
        }

        findViewById<View>(R.id.cardCp).setOnClickListener {
            showEventPopup(
                "💑 ROYAL CP (COUPLE SPACE)",
                "Send a Royal Diamond Ring inside any Voice Room to unlock:\n💖 Golden Heart CP Frame\n✨ Special Couple Entry Effect!"
            )
        }

        // Daily 100,000 Free Coins Event
        findViewById<View>(R.id.cardDailyBonus).setOnClickListener {
            claimDaily100kCoins(dbRef)
        }
        findViewById<Button>(R.id.btnClaimDailyGames).setOnClickListener {
            claimDaily100kCoins(dbRef)
        }

        // Open Greedy Game
        findViewById<Button>(R.id.btnOpenGreedy).setOnClickListener {
            startActivity(Intent(this, GreedyGameActivity::class.java))
        }

        // Edit Profile
        findViewById<Button>(R.id.btnEditProfile).setOnClickListener {
            showEditProfileDialog(dbRef)
        }

        // Super Admin Send Coins
        findViewById<Button>(R.id.btnAdminSendCoins).setOnClickListener {
            val targetUid = findViewById<EditText>(R.id.etTargetUid).text.toString().trim()
            val amountStr = findViewById<EditText>(R.id.etSendCoins).text.toString().trim()
            val amount = amountStr.toIntOrNull()
            if (targetUid.isEmpty() || amount == null) {
                Toast.makeText(this, "Enter valid User ID & Coin amount!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val targetRef = dbRef.child("users").child(targetUid).child("coins")
            targetRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    val oldCoins = snap.getValue(Int::class.java) ?: 0
                    val updated = (oldCoins + amount).coerceAtLeast(0)
                    targetRef.setValue(updated)
                    Toast.makeText(this@MainActivity, "✅ Updated ID $targetUid to 🪙 $updated!", Toast.LENGTH_SHORT).show()
                    findViewById<EditText>(R.id.etSendCoins).setText("")
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }

        // Logout / Switch Account
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            prefs.edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Load 2-Column Haza Style Live Rooms
        loadHaza2ColumnRooms(dbRef)
    }

    private fun claimDaily100kCoins(dbRef: DatabaseReference) {
        val prefs = getSharedPreferences("GupShupPrefs", MODE_PRIVATE)
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val lastClaimed = prefs.getString("last_daily_claim", "")

        if (lastClaimed == today) {
            Toast.makeText(this, "🎁 Today's 1,00,000 Coins already claimed! Come back tomorrow ✨", Toast.LENGTH_LONG).show()
            return
        }

        currentCoins += 100000
        prefs.edit()
            .putString("last_daily_claim", today)
            .putInt("user_coins", currentCoins)
            .apply()

        dbRef.child("users").child(shortUid).child("coins").setValue(currentCoins)
        refreshProfileUI()

        AlertDialog.Builder(this)
            .setTitle("🎉 MEGA DAILY REWARD!")
            .setMessage("Congratulations! You received 🪙 1,00,000 FREE Gold Coins!\n\nNew Balance: 🪙 $currentCoins")
            .setPositiveButton("AWESOME! 👑", null)
            .show()
    }

    private fun showEventPopup(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("GOT IT ✨", null)
            .show()
    }

    private fun loadHaza2ColumnRooms(dbRef: DatabaseReference) {
        dbRef.child("rooms").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                liveRoomsContainer.removeAllViews()

                data class RoomItem(
                    val roomId: String,
                    val title: String,
                    val host: String,
                    val avatar: String,
                    val gender: String,
                    val listeners: Int,
                    val isNew: Boolean,
                    val bgColor: String
                )

                val roomList = mutableListOf<RoomItem>()

                // First add real live rooms from Firebase
                for (roomSnap in snapshot.children) {
                    val rId = roomSnap.key ?: continue
                    val title = roomSnap.child("title").getValue(String::class.java) ?: "Royal Voice Party"
                    val host = roomSnap.child("host").getValue(String::class.java) ?: "VIP Host"
                    roomList.add(
                        RoomItem(rId, title, host, "👑", "♀", Random.nextInt(5, 28), true, "#2E1A47")
                    )
                }

                // Always keep Haza-style featured rooms so Home screen looks rich & active
                roomList.add(RoomItem("100101", "Don't trust anyone...", "Mahi ✨", "👩‍🎤", "♀", 17, false, "#3B1E54"))
                roomList.add(RoomItem("100102", "GUPSHUP OFFICIAL RO...", "THALAPATI", "🦁", "♂", 24, false, "#163020"))
                roomList.add(RoomItem("100103", "✨ Serenity 🌿🤍", "Mark 🚩", "😎", "♂", 12, false, "#1E2D3D"))
                roomList.add(RoomItem("100104", "💃 Ghutna Main Teri...", "Ira 🦋", "👸", "♀", 19, true, "#4A1C40"))

                // Render in 2-Column Grid Rows
                var i = 0
                while (i < roomList.size) {
                    val rowLayout = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = 24 }
                    }

                    val leftCard = createHazaRoomCard(roomList[i].roomId, roomList[i].title, roomList[i].host, roomList[i].avatar, roomList[i].gender, roomList[i].listeners, roomList[i].isNew, roomList[i].bgColor, true)
                    rowLayout.addView(leftCard)

                    if (i + 1 < roomList.size) {
                        val rightCard = createHazaRoomCard(roomList[i + 1].roomId, roomList[i + 1].title, roomList[i + 1].host, roomList[i + 1].avatar, roomList[i + 1].gender, roomList[i + 1].listeners, roomList[i + 1].isNew, roomList[i + 1].bgColor, false)
                        rowLayout.addView(rightCard)
                    } else {
                        val spacer = View(this@MainActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                        }
                        rowLayout.addView(spacer)
                    }

                    liveRoomsContainer.addView(rowLayout)
                    i += 2
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun createHazaRoomCard(
        roomId: String,
        title: String,
        host: String,
        avatar: String,
        gender: String,
        listeners: Int,
        isNew: Boolean,
        bgColorHex: String,
        isLeft: Boolean
    ): LinearLayout {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (isLeft) rightMargin = 14 else leftMargin = 14
            }
            setOnClickListener { openRoom(roomId, false) }
        }

        // Main Cover Box
        val coverBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(bgColorHex))
            setPadding(20, 18, 20, 18)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 380)
        }

        // Top Badges Row (New + Wave Count)
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val badgeNew = TextView(this).apply {
            text = if (isNew) " New " else " 🔥 HOT "
            setTextColor(Color.WHITE)
            setBackgroundColor(if (isNew) Color.parseColor("#00C49A") else Color.parseColor("#FF5722"))
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
        }

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }

        val waveCount = TextView(this).apply {
            text = "📶 $listeners"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#55000000"))
            setPadding(12, 4, 12, 4)
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
        }

        topRow.addView(badgeNew)
        topRow.addView(spacer)
        topRow.addView(waveCount)
        coverBox.addView(topRow)

        // Center Big Avatar
        val bigAvatar = TextView(this).apply {
            text = avatar
            textSize = 54f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        coverBox.addView(bigAvatar)

        // Bottom Room Title inside card
        val tvTitle = TextView(this).apply {
            text = "$gender $title"
            setTextColor(Color.WHITE)
            textSize = 13f
            maxLines = 1
            setTypeface(null, Typeface.BOLD)
        }
        coverBox.addView(tvTitle)

        // Below Card: Flag + Host Name
        val tvHost = TextView(this).apply {
            text = "🇮🇳 $host"
            setTextColor(Color.parseColor("#2B303A"))
            textSize = 13f
            maxLines = 1
            setTypeface(null, Typeface.BOLD)
            setPadding(4, 10, 4, 0)
        }

        wrapper.addView(coverBox)
        wrapper.addView(tvHost)
        return wrapper
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

    private fun showCreateRoomDialog(dbRef: DatabaseReference) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 12)
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
            .setTitle("🎙️ Create Royal Voice Room")
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

    private fun showEditProfileDialog(dbRef: DatabaseReference) {
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

        val etAvatar = EditText(this).ap
