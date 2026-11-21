package com.gupshup.pro

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
    private var currentCoins = 5000
    private var currentXp = 120

    private lateinit var tvProfileCoins: TextView
    private lateinit var roomsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("gupshup_prefs", Context.MODE_PRIVATE)
        dbRef = FirebaseDatabase.getInstance().reference

        shortUid = prefs.getString("short_uid", null) ?: Random.nextInt(100000, 999999).toString().also {
            prefs.edit().putString("short_uid", it).apply()
        }
        userName = prefs.getString("user_name", "Its Meroyal") ?: "Its Meroyal"
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
            setBackgroundColor(Color.parseColor("#1D1636"))
        }

        header.addView(TextView(this).apply {
            text = "👑 GupShupPro  |  $userName (ID: $shortUid)"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFD700"))
        })

        tvProfileCoins = TextView(this).apply {
            text = "🪙 $currentCoins Coins  •  ⚡ $currentXp XP"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 14, 0, 14)
        }
        header.addView(tvProfileCoins)

        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        btnRow.addView(Button(this).apply {
            text = "+ Create Room"
            setOnClickListener {
                val rId = Random.nextInt(100200, 999999).toString()
                dbRef.child("rooms").child(rId).setValue(mapOf("id" to rId, "name" to "$userName's Royal Room", "host" to userName, "cat" to "Party"))
                openLiveRoom(rId, "$userName's Royal Room", userName, "Party")
            }
        })
        btnRow.addView(Button(this).apply {
            text = "🎁 +500"
            setOnClickListener {
                currentCoins += 500
                currentXp += 25
                saveStats()
                Toast.makeText(this@MainActivity, "+500 Coins!", Toast.LENGTH_SHORT).show()
            }
        })
        btnRow.addView(Button(this).apply {
            text = "🎰 Spin"
            setOnClickListener {
                if (currentCoins >= 100) {
                    currentCoins = currentCoins - 100 + listOf(0, 200, 500).random()
                    saveStats()
                }
            }
        })
        header.addView(btnRow)
        root.addView(header)

        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        roomsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 28)
        }
        scroll.addView(roomsContainer)
        root.addView(scroll)
        setContentView(root)
    }

    private fun addRoomCard(id: String, name: String, host: String, cat: String) {
        val card = TextView(this).apply {
            text = "🔥 $name\n🎙️ $host  •  $cat"
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(28, 26, 28, 26)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#241744"))
                cornerRadius = 24f
            }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 18 }
            setOnClickListener { openLiveRoom(id, name, host, cat) }
        }
        roomsContainer.addView(card)
    }

    private fun openLiveRoom(roomId: String, roomName: String, hostName: String, category: String) {
        try {
            startActivity(Intent(this, LiveRoomActivity::class.java).apply {
                putExtra("ROOM_ID", roomId)
                putExtra("ROOM_NAME", roomName)
                putExtra("HOST_NAME", hostName)
                putExtra("CATEGORY", category)
                putExtra("USER_ID", shortUid)
                putExtra("USER_NAME", userName)
                putExtra("USER_COINS", currentCoins)
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Opening: $roomName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveStats() {
        prefs.edit().putInt("user_coins", currentCoins).putInt("user_xp", currentXp).apply()
        tvProfileCoins.text = "🪙 $currentCoins Coins  •  ⚡ $currentXp XP"
        dbRef.child("users").child(shortUid).updateChildren(mapOf("uid" to shortUid, "name" to userName, "coins" to currentCoins, "xp" to currentXp))
    }

    private fun loadRoomsData() {
        roomsContainer.removeAllViews()
        addRoomCard("100101", "🎶 Late Night Bollywood & Lofi", "DJ Kabir", "Music")
        addRoomCard("100102", "👑 Royal Shayari & Friendship", "Ayisha Queen", "Royal")
        addRoomCard("100103", "💘 Dil Se Dil Tak 1-on-1", "Riya Live", "Dating")
        addRoomCard("100104", "🧧 Lucky Bag & Ring Toss", "Its Meroyal", "Games")

        dbRef.child("rooms").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                roomsContainer.removeAllViews()
                for (child in snapshot.children) {
                    val id = child.child("id").getValue(String::class.java) ?: child.key ?: continue
                    val name = child.child("name").getValue(String::class.java) ?: continue
                    val host = child.child("host").getValue(String::class.java) ?: "Host"
                    val cat = child.child("cat").getValue(String::class.java) ?: "Party"
                    addRoomCard(id, name, host, cat)
                }
                addRoomCard("100101", "🎶 Late Night Bollywood & Lofi", "DJ Kabir", "Music")
                addRoomCard("100102", "👑 Royal Shayari & Friendship", "Ayisha Queen", "Royal")
                addRoomCard("100103", "💘 Dil Se Dil Tak 1-on-1", "Riya Live", "Dating")
                addRoomCard("100104", "🧧 Lucky Bag & Ring Toss", "Its Meroyal", "Games")
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
