package com.gupshup.pro

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var dbRef: DatabaseReference
    private var shortUid = "884920"
    private var userName = "Its Meroyal"
    private var userGender = "Male"
    private var currentCoins = 5000
    private var currentXp = 120
    private var selectedCategory = "All"

    private lateinit var tvProfileCoins: TextView
    private lateinit var tvUserName: TextView
    private lateinit var meRoomsGrid: LinearLayout
    private lateinit var popularRoomsGrid: LinearLayout

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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
    }

    private fun buildMainUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D0A1A"))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 44, 36, 28)
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#241744"), Color.parseColor("#120D24")))
        }

            val title = TextView(this).apply {
            text = "👑 GupShupPro  |  $userName (ID: $shortUid)"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFD700"))
        }
        tvUserName = TextView(this).apply {
            text = "VIP 1  •  Tap ID to Copy"
            textSize = 12f
            setTextColor(Color.WHITE)
            setOnClickListener {
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("UID", shortUid))
                Toast.makeText(this@MainActivity, "Copied: $shortUid", Toast.LENGTH_SHORT).show()
            }
        }
        tvProfileCoins = TextView(this).apply {
            text = "🪙 $currentCoins Coins  -  ⚡ $currentXp XP"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFE57F"))
            setPadding(0, 12, 0, 12)
        }
        header.addView(title)
        header.addView(tvUserName)
        header.addView(tvProfileCoins)
        root.addView(header)

        val btnBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(24, 12, 24, 12) }
        val b1 = Button(this).apply {
            text = "+ Room"
            setOnClickListener {
                val rId = Random.nextInt(100200, 999999).toString()
                dbRef.child("rooms").child(rId).setValue(mapOf("id" to rId, "name" to "$userName's Room", "host" to userName, "cat" to "Party", "count" to "1"))
                openLiveRoom(rId, "$userName's Room", userName, "Party")
            }
        }
        val b2 = Button(this).apply {
            text = "🎁 +500"
            setOnClickListener { currentCoins += 500; currentXp += 25; saveLocalStats(); Toast.makeText(this@MainActivity, "+500 Coins!", Toast.LENGTH_SHORT).show() }
        }
        val b3 = Button(this).apply {
            text = "🎰 Spin"
            setOnClickListener {
                if (currentCoins >= 100) {
                    currentCoins = currentCoins - 100 + listOf(0, 200, 500).random()
                    saveLocalStats()
                }
            }
        }
        btnBar.addView(b1); btnBar.addView(b2); btnBar.addView(b3)
        root.addView(btnBar)

        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 12, 28, 28) }
        meRoomsGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        popularRoomsGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(meRoomsGrid)
        box.addView(popularRoomsGrid)
        scroll.addView(box)
        root.addView(scroll)
        setContentView(root)
    }

    private fun renderHazaTwoColumnGrid(container: LinearLayout, rooms: List>) {
        container.removeAllViews()
        for (room in rooms) {
            val rId = room["id"] ?: "100101"
            val rName = room["name"] ?: "Voice Room"
            val rHost = room["host"] ?: "Host"
            val rCat = room["cat"] ?: "Party"
            val card = TextView(this).apply {
                text = "🔥 $rName\n🎙️ $rHost  •  $rCat"
                textSize = 14f
                setTextColor(Color.WHITE)
                setPadding(28, 24, 28, 24)
                background = GradientDrawable().apply { setColor(Color.parseColor("#241744")); cornerRadius = 24f }
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 16 }
                setOnClickListener { openLiveRoom(rId, rName, rHost, rCat) }
            }
            container.addView(card)
        }
    }

    private fun openLiveRoom(roomId: String, roomName: String, hostName: String, category: String) {
        try {
            startActivity(Intent(this, LiveRoomActivity::class.java).apply {
                putExtra("ROOM_ID", roomId); putExtra("ROOM_NAME", roomName); putExtra("HOST_NAME", hostName)
                putExtra("CATEGORY", category); putExtra("USER_ID", shortUid); putExtra("USER_NAME", userName); putExtra("USER_COINS", currentCoins)
            })
        } catch (e: Exception) { Toast.makeText(this, "Opening: $roomName", Toast.LENGTH_SHORT).show() }
    }

    private fun saveLocalStats() {
        prefs.edit().putInt("user_coins", currentCoins).putInt("user_xp", currentXp).apply()
        tvProfileCoins.text = "🪙 $currentCoins Coins  -  ⚡ $currentXp XP"
        dbRef.child("users").child(shortUid).updateChildren(mapOf("uid" to shortUid, "name" to userName, "coins" to currentCoins, "xp" to currentXp))
    }

    private fun loadRoomsData() {
        val defaultRooms = listOf(
            mapOf("id" to "100101", "name" to "🎶 Late Night Bollywood & Lofi", "host" to "DJ Kabir", "cat" to "Music"),
            mapOf("id" to "100102", "name" to "👑 Royal Shayari & Friendship", "host" to "Ayisha Queen", "cat" to "Royal"),
            mapOf("id" to "100103", "name" to "💘 Dil Se Dil Tak 1-on-1", "host" to "Riya Live", "cat" to "Dating"),
            mapOf("id" to "100104", "name" to "🧧 Lucky Bag & Ring Toss", "host" to "Its Meroyal", "cat" to "Games")
        )
        renderHazaTwoColumnGrid(meRoomsGrid, defaultRooms.take(2))
        renderHazaTwoColumnGrid(popularRoomsGrid, defaultRooms)

        dbRef.child("rooms").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val liveList = val liveList = defaultRooms.take(0).toMutableList() 
                for (child in snapshot.children) {
                    val id = child.child("id").getValue(String::class.java) ?: child.key ?: continue
                    val name = child.child("name").getValue(String::class.java) ?: continue
                    val host = child.child("host").getValue(String::class.java) ?: "Host"
                    val cat = child.child("cat").getValue(String::class.java) ?: "Party"
                    liveList.add(mapOf("id" to id, "name" to name, "host" to host, "cat" to cat))
                }
                val combined = liveList + defaultRooms
                renderHazaTwoColumnGrid(meRoomsGrid, combined.take(2))
                renderHazaTwoColumnGrid(popularRoomsGrid, combined)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
