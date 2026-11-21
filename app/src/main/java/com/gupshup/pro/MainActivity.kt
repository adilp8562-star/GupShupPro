package com.gupshup.pro

import android.Manifest
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var dbRef: DatabaseReference

    private var shortUid: String = "884920"
    private var userName: String = "Royal Guest"
    private var userGender: String = "Male"
    private var currentCoins: Int = 5000
    private var currentXp: Int = 120
    private var selectedCategory: String = "All"

    private lateinit var tvProfileCoins: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserUid: TextView
    private lateinit var meRoomsGrid: LinearLayout
    private lateinit var popularRoomsGrid: LinearLayout
    private lateinit var contentContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("gupshup_prefs", Context.MODE_PRIVATE)
        dbRef = FirebaseDatabase.getInstance().reference

        shortUid = prefs.getString("short_uid", null) ?: Random.nextInt(100000, 999999).toString().also {
            prefs.edit().putString("short_uid", it).apply()
        }
        userName = prefs.getString("user_name", "Its Meroyal") ?: "Its Meroyal"
        userGender = prefs.getString("user_gender", "Male") ?: "Male"
        currentCoins = prefs.getInt("user_coins", 5000)
        currentXp = prefs.getInt("user_xp", 120)

        buildMainUi()
        loadRoomsData()
        requestAudioPermission()
    }

    private fun buildMainUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D0A1A"))
        }

        // Top Header Bar
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 44, 36, 28)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#241744"), Color.parseColor("#120D24"))
            )
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val appTitle = TextView(this).apply {
            text = "👑 GupShupPro"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFD700"))
        }

        tvUserName = TextView(this).apply {
            text = "$userName  •  VIP 1"
            textSize = 14f
            setTextColor(Color.WHITE)
        }

        tvUserUid = TextView(this).apply {
            text = "ID: $shortUid 📋"
            textSize = 12f
            setTextColor(Color.parseColor("#B8B0D8"))
            setOnClickListener { copyUidToClipboard() }
        }

        titleCol.addView(appTitle)
        titleCol.addView(tvUserName)
        titleCol.addView(tvUserUid)

        val btnCreate = TextView(this).apply {
            text = "+ Create Room"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0D0A1A"))
            setPadding(32, 18, 32, 18)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFD700"))
                cornerRadius = 40f
            }
            setOnClickListener { showCreateRoomDialog() }
        }

        topRow.addView(titleCol)
        topRow.addView(btnCreate)
        header.addView(topRow)

        // Coins & Daily Bonus Row
        val walletRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 18, 24, 18)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 20
            layoutParams = lp
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1D1636"))
                cornerRadius = 28f
            }
        }

        tvProfileCoins = TextView(this).apply {
            text = "🪙 $currentCoins Coins  -  ⚡$currentXp XP"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFE57F"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnBonus = TextView(this).apply {
            text = "🎁 Daily +500"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(24, 12, 24, 12)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#7C4DFF"))
                cornerRadius = 24f
            }
            setOnClickListener {
                currentCoins += 500
                currentXp += 25
                saveLocalStats()
                Toast.makeText(this@MainActivity, "🎉 +500 Daily Coins Claimed!", Toast.LENGTH_SHORT).show()
            }
        }

        walletRow.addView(tvProfileCoins)
        walletRow.addView(btnBonus)
        header.addView(walletRow)
        root.addView(header)

        // Category Tabs
        val catScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(24, 16, 24, 16)
        }
        val catRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val categories = listOf("All", "Party", "Music", "Royal", "Dating", "Games")
        for (cat in categories) {
            val tab = TextView(this).apply {
                text = cat
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setPadding(32, 14, 32, 14)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.rightMargin = 16
                layoutParams = lp
                setTextColor(if (cat == selectedCategory) Color.parseColor("#0D0A1A") else Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(if (cat == selectedCategory) Color.parseColor("#FFD700") else Color.parseColor("#1D1636"))
                    cornerRadius = 32f
                }
                setOnClickListener {
                    selectedCategory = cat
                    buildMainUi()
                    loadRoomsData()
                }
            }
            catRow.addView(tab)
        }
        catScroll.addView(catRow)
        root.addView(catScroll)

        // Scrollable Rooms Area
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 12, 28, 36)
        }

        val lblHot = TextView(this).apply {
            text = "🔥 Hot Voice Rooms"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 8, 0, 16)
        }
        meRoomsGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val lblPopular = TextView(this).apply {
            text = "✨ Popular Party Clubs"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 24, 0, 16)
        }
        popularRoomsGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        contentContainer.addView(lblHot)
        contentContainer.addView(meRoomsGrid)
        contentContainer.addView(lblPopular)
        contentContainer.addView
            private fun showCreateRoomDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        val etName = EditText(this).apply { hint = "Enter Room Title (e.g. Royal Party)" }
        val etCat = EditText(this).apply {
            hint = "Category (Party / Music / Royal / Dating / Games)"
            setText("Party")
        }
        box.addView(etName)
        box.addView(etCat)

        AlertDialog.Builder(this)
            .setTitle("🎙️ Create Voice Room")
            .setView(box)
            .setPositiveButton("Go Live") { _, _ ->
                val rName = etName.text.toString().trim().ifEmpty { "$userName's Royal Room" }
                val rCat = etCat.text.toString().trim().ifEmpty { "Party" }
                val rId = Random.nextInt(100200, 999999).toString()

                val roomMap = mapOf(
                    "id" to rId,
                    "name" to rName,
                    "host" to userName,
                    "cat" to rCat,
                    "count" to "1"
                )
                dbRef.child("rooms").child(rId).setValue(roomMap)
                openLiveRoom(rId, rName, userName, rCat)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun playGreedyMiniGame() {
        if (currentCoins < 100) {
            Toast.makeText(this, "Need at least 100 Coins to play!", Toast.LENGTH_SHORT).show()
            return
        }
        currentCoins -= 100
        val win = listOf(0, 50, 200, 500, 1000).random()
        currentCoins += win
        currentXp += 15
        saveLocalStats()

        val msg = if (win >= 200) "🎉 JACKPOT! You won +$win Coins!" else "🎲 Round Over! Won $win Coins"
        AlertDialog.Builder(this)
            .setTitle("🎰 30s Greedy Gold Spin")
            .setMessage("$msg\n\nCurrent Balance: 🪙 $currentCoins Coins")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showEditProfileDialog() {
        val etName = EditText(this).apply {
            setText(userName)
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("✏️ Edit Your Royal Name")
            .setView(etName)
            .setPositiveButton("Save") { _, _ ->
                val newName = etName.text.toString().trim()
                if (newName.isNotEmpty()) {
                    userName = newName
                    prefs.edit().putString("user_name", userName).apply()
                    tvUserName.text = "$userName  •  VIP 1"
                    syncFirebaseUser()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renderHazaTwoColumnGrid(container: LinearLayout, rooms: List>) {
        container.removeAllViews()
        var i = 0
        while (i < rooms.size) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = 20
                layoutParams = lp
            }
            row.addView(createRoomCard(rooms[i]))
            if (i + 1 < rooms.size) {
                row.addView(createRoomCard(rooms[i + 1]))
            } else {
                val spacer = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 10, 1f)
                }
                row.addView(spacer)
            }
            container.addView(row)
            i += 2
        }
    }

    private fun createRoomCard(room: Map<String, String>): LinearLayout {
        val rId = room["id"] ?: "100101"
        val rName = room["name"] ?: "Voice Party Room"
        val rHost = room["host"] ?: "Host"
        val rCat = room["cat"] ?: "Party"
        val rCount = room["count"] ?: "12"

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(8, 0, 8, 0)
            layoutParams = lp
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#2A1E52"), Color.parseColor("#181230"))
            ).apply {
                cornerRadius = 28f
                setStroke(2, Color.parseColor("#4A3880"))
            }

            val badge = TextView(this@MainActivity).apply {
                text = "🔥 $rCat  •  👥 $rCount"
                textSize = 11f
                setTextColor(Color.parseColor("#FFD700"))
            }

            val title = TextView(this@MainActivity).apply {
                text = rName
                textSize = 14f
                maxLines = 2
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setPadding(0, 12, 0, 12)
            }

            val host = TextView(this@MainActivity).apply {
                text = "🎙️ $rHost"
                textSize = 12f
                setTextColor(Color.parseColor("#B8B0D8"))
            }

            addView(badge)
            addView(title)
            addView(host)

            setOnClickListener {
                openLiveRoom(rId, rName, rHost, rCat)
            }
        }
    }

    private fun copyUidToClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("UID", shortUid))
        Toast.makeText(this, "Copied ID: $shortUid 📋", Toast.LENGTH_SHORT).show()
    }

    private fun openLiveRoom(roomId: String, roomName: String, hostName: String, category: String) {
        try {
            val intent = Intent(this, LiveRoomActivity::class.java).apply {
                putExtra("ROOM_ID", roomId)
                putExtra("ROOM_NAME", roomName)
                putExtra("HOST_NAME", hostName)
                putExtra("CATEGORY", category)
                putExtra("USER_ID", shortUid)
                putExtra("USER_NAME", userName)
                putExtra("USER_COINS", currentCoins)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Opening Room: $roomName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveLocalStats() {
        prefs.edit().putInt("user_coins", currentCoins).putInt("user_xp", currentXp).apply()
        tvProfileCoins.text = "🪙 $currentCoins Coins  -  ⚡$currentXp XP"
        syncFirebaseUser()
    }

    private fun syncFirebaseUser() {
        val data = mapOf("uid" to shortUid, "name" to userName, "coins" to currentCoins, "xp" to currentXp, "gender" to userGender)
        dbRef.child("users").child(shortUid).updateChildren(data)
    }

    private fun loadRoomsData() {
        val defaultRooms = listOf(
            mapOf("id" to "100101", "name" to "🎶 Late Night Bollywood & Lofi", "host" to "DJ Kabir", "cat" to "Music", "count" to "48"),
            mapOf("id" to "100102", "name" to "👑 Royal Shayari & Friendship", "host" to "Ayisha Queen", "cat" to "Royal", "count" to "64"),
            mapOf("id" to "100103", "name" to "💘 Dil Se Dil Tak 1-on-1 & Chat", "host" to "Riya Live", "cat" to "Dating", "count" to "39"),
            mapOf("id" to "100104", "name" to "🧧 Lucky Bag & Ring Toss Party", "host" to "Its Meroyal", "cat" to "Games", "count" to "92"),
            mapOf("id" to "100105", "name" to "🔥 Non-Stop Punjabi Beats", "host" to "Aryan", "cat" to "Music", "count" to "51"),
            mapOf("id" to "100106", "name" to "🎉 Weekend Mega Voice Club", "host" to "Zoya", "cat" to "Party", "count" to "77")
        )
        val filtered = if (selectedCategory == "All") defaultRooms else defaultRooms.filter { it["cat"].equals(selectedCategory, true) }
        renderHazaTwoColumnGrid(meRoomsGrid, defaultRooms.take(4))
        renderHazaTwoColumnGrid(popularRoomsGrid, filtered.ifEmpty { defaultRooms })

        dbRef.child("rooms").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(
                
