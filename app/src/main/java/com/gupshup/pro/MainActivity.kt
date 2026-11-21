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
import android.widget.ScrollView
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
    private lateinit var layoutSuperAdmin: LinearLayout
    private lateinit var liveRoomsContainer: LinearLayout

    private lateinit var tabParty: View
    private lateinit var tabGames: View
    private lateinit var tabMe: ScrollView
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

        val dbRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).reference

        tvCoinsTop = findViewById(R.id.tvCoinsTop)
        liveRoomsContainer = findViewById(R.id.liveRoomsContainer)
        tabParty = findViewById(R.id.tabParty)
        tabGames = findViewById(R.id.tabGames)
        tabMe = findViewById(R.id.tabMe)
        navParty = findViewById(R.id.navParty)
        navGames = findViewById(R.id.navGames)
        navMe = findViewById(R.id.navMe)

        buildBulletproofMeTab(dbRef, prefs)
        refreshProfileUI()

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
                    layoutSuperAdmin.visibility = if (isSuperAdmin) View.VISIBLE else View.GONE
                    refreshProfileUI()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        findViewById<TextView>(R.id.tvTopMe).setOnClickListener { selectTab(2) }
        navParty.setOnClickListener { selectTab(0) }
        navGames.setOnClickListener { selectTab(1) }
        navMe.setOnClickListener { selectTab(2) }

        findViewById<TextView>(R.id.btnTopSearch).setOnClickListener {
            val etRoom = findViewById<EditText>(R.id.etRoomIdInput)
            etRoom.requestFocus()
            Toast.makeText(this, "🔍 Type 6-digit Room ID below to join!", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnCreateRoom).setOnClickListener { showCreateRoomDialog(dbRef) }

        findViewById<Button>(R.id.btnJoinRoom).setOnClickListener {
            val roomId = findViewById<EditText>(R.id.etRoomIdInput).text.toString().trim()
            if (roomId.length < 4) {
                Toast.makeText(this, "Enter valid Room ID!", Toast.LENGTH_SHORT).show()
            } else {
                openRoom(roomId, false)
            }
        }

        findViewById<View>(R.id.bannerWeeklyStar).setOnClickListener {
            showEventPopup("✨ WEEKLY STAR CARNIVAL ✨", "🏆 #1 Royal Star: 5,00,000 Coins + Dragon Frame\n🥈 #2 Party Queen: 2,50,000 Coins + Crown\n🥉 #3 Rising Host: 1,00,000 Coins")
        }

        findViewById<View>(R.id.cardRanking).setOnClickListener {
            showEventPopup("🏆 TOP ROYAL RANKING", "1. 👑 $userName (ID: $shortUid) — 🪙 $currentCoins\n2. 🇮🇳 Mahi ✨ — 🪙 8,50,000\n3. 🇮🇳 THALAPATI — 🪙 6,20,000")
        }

        findViewById<View>(R.id.cardFamily).setOnClickListener {
            showEventPopup("🏰 ROYAL FAMILY CLAN", "Join or create your Royal Family!\n• Family Badge on profile\n• Weekly 10,00,000 Coin Family Treasury")
        }

        findViewById<View>(R.id.cardCp).setOnClickListener {
            showEventPopup("💑 ROYAL CP (COUPLE SPACE)", "Send a Royal Diamond Ring inside any Voice Room to unlock Golden Heart CP Frame!")
        }

        findViewById<View>(R.id.cardDailyBonus).setOnClickListener { claimDaily100kCoins(dbRef) }
        findViewById<Button>(R.id.btnClaimDailyGames).setOnClickListener { claimDaily100kCoins(dbRef) }

        findViewById<Button>(R.id.btnOpenGreedy).setOnClickListener {
            startActivity(Intent(this, GreedyGameActivity::class.java))
        }

        loadHaza2ColumnRooms(dbRef)
    }

    private fun buildBulletproofMeTab(dbRef: DatabaseReference, prefs: android.content.SharedPreferences) {
        tabMe.removeAllViews()
        tabMe.setBackgroundColor(Color.parseColor("#F5F7FA"))

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 36, 36, 48)
        }

        val profileCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#23123D"))
            setPadding(44, 48, 44, 48)
        }

        tvUserAvatarMe = TextView(this).apply {
            text = userAvatar
            textSize = 52f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#3D206B"))
            layoutParams = LinearLayout.LayoutParams(190, 190)
        }

        tvUserNameMe = TextView(this).apply {
            text = userName
            setTextColor(Color.WHITE)
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 22, 0, 8)
        }

        tvUserUidMe = TextView(this).apply {
            text = "ID: $shortUid • ROYAL VIP"
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        tvUserBioMe = TextView(this).apply {
            text = userBio
            setTextColor(Color.parseColor("#D4C7EC"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 24)
        }

        tvCoinsMe = TextView(this).apply {
            text = "🪙 $currentCoins"
            setTextColor(Color.parseColor("#1A0B2E"))
            setBackgroundColor(Color.parseColor("#FFD700"))
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setPadding(42, 16, 42, 16)
        }

        val btnEdit = Button(this).apply {
            text = "✏️ Edit VIP Profile (Name / Avatar / Bio)"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#8B5CF6"))
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 120).apply { topMargin = 32 }
            setOnClickListener { showEditProfileDialog(dbRef) }
        }

        profileCard.addView(tvUserAvatarMe)
        profileCard.addView(tvUserNameMe)
        profileCard.addView(tvUserUidMe)
        profileCard.addView(tvUserBioMe)
        profileCard.addView(tvCoinsMe)
        profileCard.addView(btnEdit)
        container.addView(profileCard)

        layoutSuperAdmin = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FFF8E1"))
            setPadding(36, 36, 36, 36)
            visibility = if (isSuperAdmin) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 32 }
        }

        val tvAdminTitle = TextView(this).apply {
            text = "🛡️ SUPER ADMIN COIN CONTROL"
            setTextColor(Color.parseColor("#B27800"))
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
        }
        val etTargetUid = EditText(this).apply {
            hint = "Enter User ID (e.g. 100001)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val etSendCoins = EditText(this).apply {
            hint = "Coins amount (e.g. 500000 or -50000)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        val btnSend = Button(this).apply {
            text = "⚡ SEND / UPDATE USER COINS"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#FF8F00"))
            setTypeface(null, Typeface.BOLD)
            setOnClickListener {
                val targetUid = etTargetUid.text.toString().trim()
                val amount = etSendCoins.text.toString().trim().toIntOrNull()
                if (targetUid.isEmpty() || amount == null) {
                    Toast.makeText(this@MainActivity, "Enter valid User ID & Coin amount!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val targetRef = dbRef.child("users").child(targetUid).child("coins")
                targetRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snap: DataSnapshot) {
                        val oldCoins = snap.getValue(Int::class.java) ?: 0
                        val updated = (oldCoins + amount).coerceAtLeast(0)
                        targetRef.setValue(updated)
                        Toast.makeText(this@MainActivity, "✅ Updated ID $targetUid to 🪙 $updated!", Toast.LENGTH_SHORT).show()
                        etSendCoins.setText("")
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }
        layoutSuperAdmin.addView(tvAdminTitle)
        layoutSuperAdmin.addView(etTargetUid)
        layoutSuperAdmin.addView(etSendCoins)
        layoutSuperAdmin.addView(btnSend)
        container.addView(layoutSuperAdmin)

        val btnLogout = Button(this).apply {
            text = "🔄 Switch Google Account / Logout"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#E53935"))
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 124).apply { topMargin = 36 }
            setOnClickListener {
                prefs.edit().clear().apply()
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        container.addView(btnLogout)
        tabMe.addView(container)
    }    private fun claimDaily100kCoins(dbRef: DatabaseReference) {
        val prefs = getSharedPreferences("GupShupPrefs", MODE_PRIVATE)
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        if (prefs.getString("last_daily_claim", "") == today) {
            Toast.makeText(this, "🎁 Today's 1,00,000 Coins already claimed! Come back tomorrow ✨", Toast.LENGTH_LONG).show()
            return
        }
        currentCoins += 100000
        prefs.edit().putString("last_daily_claim", today).putInt("user_coins", currentCoins).apply()
        dbRef.child("users").child(shortUid).child("coins").setValue(currentCoins)
        refreshProfileUI()
        AlertDialog.Builder(this)
            .setTitle("🎉 MEGA DAILY REWARD!")
            .setMessage("You received 🪙 1,00,000 FREE Gold Coins!\n\nNew Balance: 🪙 $currentCoins")
            .setPositiveButton("AWESOME! 👑", null)
            .show()
    }

    private fun showEventPopup(title: String, message: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("GOT IT ✨", null).show()
    }

    private fun loadHaza2ColumnRooms(dbRef: DatabaseReference) {
        dbRef.child("rooms").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                liveRoomsContainer.removeAllViews()
                data class RoomItem(val roomId: String, val title: String, val host: String, val avatar: String, val gender: String, val listeners: Int, val isNew: Boolean, val bgColor: String)
                val roomList = mutableListOf<RoomItem>()
                for (roomSnap in snapshot.children) {
                    val rId = roomSnap.key ?: continue
                    val title = roomSnap.child("title").getValue(String::class.java) ?: "Royal Voice Party"
                    val host = roomSnap.child("host").getValue(String::class.java) ?: "VIP Host"
                    roomList.add(RoomItem(rId, title, host, "👑", "♀", Random.nextInt(5, 28), true, "#2E1A47"))
                }
                roomList.add(RoomItem("100101", "Don't trust anyone...", "Mahi ✨", "👩‍🎤", "♀", 17, false, "#3B1E54"))
                roomList.add(RoomItem("100102", "GUPSHUP OFFICIAL RO...", "THALAPATI", "🦁", "♂", 24, false, "#163020"))
                roomList.add(RoomItem("100103", "✨ Serenity 🌿🤍", "Mark 🚩", "😎", "♂", 12, false, "#1E2D3D"))
                roomList.add(RoomItem("100104", "💃 Ghutna Main Teri...", "Ira 🦋", "👸", "♀", 19, true, "#4A1C40"))

                var i = 0
                while (i < roomList.size) {
                    val rowLayout = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 24 }
                    }
                    rowLayout.addView(createHazaRoomCard(roomList[i].roomId, roomList[i].title, roomList[i].host, roomList[i].avatar, roomList[i].gender, roomList[i].listeners, roomList[i].isNew, roomList[i].bgColor, true))
                    if (i + 1 < roomList.size) {
                        rowLayout.addView(createHazaRoomCard(roomList[i + 1].roomId, roomList[i + 1].title, roomList[i + 1].host, roomList[i + 1].avatar, roomList[i + 1].gender, roomList[i + 1].listeners, roomList[i + 1].isNew, roomList[i + 1].bgColor, false))
                    } else {
                        rowLayout.addView(View(this@MainActivity).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
                    }
                    liveRoomsContainer.addView(rowLayout)
                    i += 2
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun createHazaRoomCard(roomId: String, title: String, host: String, avatar: String, gender: String, listeners: Int, isNew: Boolean, bgColorHex: String, isLeft: Boolean): LinearLayout {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (isLeft) rightMargin = 14 else leftMargin = 14
            }
            setOnClickListener { openRoom(roomId, false) }
        }
        val coverBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(bgColorHex))
            setPadding(20, 18, 20, 18)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 380)
        }
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
        val spacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) }
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

        val bigAvatar = TextView(this).apply {
            text = avatar
            textSize = 54f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        coverBox.addView(bigAvatar)

        val tvTitle = TextView(this).apply {
            text = "$gender $title"
            setTextColor(Color.WHITE)
            textSize = 13f
            maxLines = 1
            setTypeface(null, Typeface.BOLD)
        }
        coverBox.addView(tvTitle)

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
        val etTitle = EditText(this).apply { hint = "Room Name"; setText("$userName's Party") }
        val etTopic = EditText(this).apply { hint = "Topic"; setText("Music & Chill 🎶") }
        layout.addView(etTitle)
        layout.addView(etTopic)
        AlertDialog.Builder(this)
            .setTitle("🎙️ Create Royal Voice Room")
            .setView(layout)
            .setPositiveButton("Go Live") { _, _ ->
                val newRoomId = Random.nextInt(100000, 999999).toString()
                val title = etTitle.text.toString().trim().ifEmpty { "$userName's Party" }
                val topic = etTopic.text.toString().trim().ifEmpty { "Music & Chill 🎶" }
                dbRef.child("rooms").child(newRoomId).setValue(mapOf("title" to title, "host" to userName, "hostUid" to shortUid, "topic" to topic))
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
        val etName = EditText(this).apply { hint = "Display Name"; setText(userName) }
        val etBio = EditText(this).apply { hint = "Profile Bio"; setText(userBio) }
        val etAvatar = EditText(this).apply { hint = "VIP Avatar Emoji"; setText(userAvatar) }
        val rgGender = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        val rbMale = RadioButton(this).apply { text = "♂️ Male"; id = View.generateViewId() }
        val rbFemale = RadioButton(this).apply { text = "♀️ Female"; id = View.generateViewId() }
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
                userName = etName.text.toString().trim().ifEmpty { userName }
                userBio = etBio.text.toString().trim().ifEmpty { userBio }
                userAvatar = etAvatar.text.toString().trim().ifEmpty { "👑" }
                userGender = if (rbFemale.isChecked) "Female" else "Male"
                getSharedPreferences("GupShupPrefs", MODE_PRIVATE).edit()
                    .putString("user_name", userName).putString("user_bio", userBio)
                    .putString("user_avatar", userAvatar).putString("user_gender", userGender).apply()
                dbRef.child("users").child(shortUid).updateChildren(mapOf("name" to userName, "bio" to userBio, "avatar" to userAvatar, "gender" to userGender))
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
        navParty.setTextColor(if (index == 0) Color.parseColor("#00C49A") else Color.parseColor("#8E96AA"))
        navGames.setTextColor(if (index == 1) Color.parseColor("#00C49A") else Color.parseColor("#8E96AA"))
        navMe.setTextColor(if (index == 2) Color.parseColor("#00C49A") else Color.parseColor("#8E96AA"))
    }

    private fun openRoom(roomId: String, isHost: Boolean) {
        val intent = Intent(this, LiveRoomActivity::class.java)
        intent.putExtra("ROOM_ID", roomId)
        intent.putExtra("IS_HOST", isHost)
        startActivity(intent)
    }

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
    }
}

