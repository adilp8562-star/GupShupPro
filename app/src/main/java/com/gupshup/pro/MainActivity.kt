package com.gupshup.pro

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var dbRef: DatabaseReference
    private var shortUid = "777777"
    private var userName = "Its Meroyal"
    private var currentCoins = 500000
    private var currentXp = 1000000
    private var vipLevel = 100
    private var selectedCat = "All"

    private lateinit var tvWallet: TextView
    private lateinit var tvHeaderName: TextView
    private lateinit var tvLevelBadge: TextView
    private lateinit var mainBody: LinearLayout
    private lateinit var roomsGrid: LinearLayout
    private lateinit var tabRooms: TextView
    private lateinit var tabGames: TextView
    private lateinit var tabChat: TextView
    private lateinit var tabMe: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("gupshup_prefs", Context.MODE_PRIVATE)
        dbRef = FirebaseDatabase.getInstance().reference

        userName = intent.getStringExtra("USER_NAME") ?: prefs.getString("user_name", "Its Meroyal") ?: "Its Meroyal"
        val isOwner = userName.equals("Its Meroyal", ignoreCase = true)

        shortUid = prefs.getString("short_uid", null) ?: (if (isOwner) "777777" else Random.nextInt(100000, 999999).toString()).also {
            prefs.edit().putString("short_uid", it).apply()
        }

        val savedCoins = prefs.getInt("user_coins", 500000)
        currentCoins = if (savedCoins <= 5000) 500000 else savedCoins
        val savedXp = prefs.getInt("user_xp", if (isOwner) 1000000 else 10000)
        currentXp = if (savedXp <= 120) (if (isOwner) 1000000 else 10000) else savedXp
        vipLevel = prefs.getInt("vip_level", if (isOwner) 100 else (currentXp / 10000).coerceIn(1, 100))
        saveStats()

        buildHazaHome()
        showRoomsTab()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
    }

    private fun makeBox(bgHex: String, strokeHex: String, strokeW: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(bgHex))
            if (strokeW > 0) setStroke(strokeW, Color.parseColor(strokeHex))
            cornerRadius = radius
        }
    }

    private fun getLevelBadgeColor(lvl: Int): String {
        return when {
            lvl >= 90 -> "#D50000"
            lvl >= 50 -> "#FF8F00"
            lvl >= 20 -> "#7B1FA2"
            else -> "#0288D1"
        }
    }

    private fun buildHazaHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B0719"))
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(28, 40, 28, 24)
            background = makeBox("#1A103C", "#FFD700", 2, 0f)
        }

        val avatar = TextView(this).apply {
            text = "👑"
            textSize = 22f
            gravity = Gravity.CENTER
            background = makeBox("#2E1A66", "#FFD700", 4, 60f)
            layoutParams = LinearLayout.LayoutParams(106, 106).apply { rightMargin = 20 }
        }
        topBar.addView(avatar)

        val infoCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }

        val nameRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        tvHeaderName = TextView(this).apply {
            text = userName
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFD700"))
        }
        tvLevelBadge = TextView(this).apply {
            text = if (vipLevel >= 100) " Lv.100 OWNER " else " Lv.$vipLevel "
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(12, 4, 12, 4)
            background = makeBox(getLevelBadgeColor(vipLevel), "#FFD700", 2, 20f)
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { leftMargin = 12 }
        }
        nameRow.addView(tvHeaderName)
        nameRow.addView(tvLevelBadge)
        infoCol.addView(nameRow)

        infoCol.addView(TextView(this).apply {
            text = "ID: $shortUid  •  ⚡ $currentXp XP"
            textSize = 12f
            setTextColor(Color.parseColor("#C7B8EA"))
            setPadding(0, 4, 0, 0)
        })

        tvWallet = TextView(this).apply {
            text = "🪙 $currentCoins Coins"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(18, 6, 18, 6)
            background = makeBox("#351C75", "#FFA500", 2, 30f)
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { topMargin = 8 }
        }
        infoCol.addView(tvWallet)
        topBar.addView(infoCol)

        val createBtn = TextView(this).apply {
            text = "🎙️\nMy Room"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A0B2E"))
            gravity = Gravity.CENTER
            setPadding(22, 16, 22, 16)
            background = makeBox("#FFD700", "#FFFFFF", 2, 26f)
            setOnClickListener {
                openLiveRoom("100001", "👑 $userName Official VIP Club", userName, "Official")
            }
        }
        topBar.addView(createBtn)
        root.addView(topBar)

        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        mainBody = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 24)
        }
        scroll.addView(mainBody)
        root.addView(scroll)

        val navBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 18, 12, 22)
            background = makeBox("#160D33", "#3D267A", 2, 0f)
        }
        tabRooms = makeNavTab("🏠\nRooms") { showRoomsTab() }
        tabGames = makeNavTab("🎮\nGames") { showGamesTab() }
        tabChat = makeNavTab("💬\nChat") { showChatTab() }
        tabMe = makeNavTab("👤\nMe") { showMeTab() }
        navBar.addView(tabRooms)
        navBar.addView(tabGames)
        navBar.addView(tabChat)
        navBar.addView(tabMe)
        root.addView(navBar)

        setContentView(root)
    }

    private fun makeNavTab(label: String, action: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            setOnClickListener { action() }
        }
    }

    private fun highlightTab(active: TextView) {
        listOf(tabRooms, tabGames, tabChat, tabMe).forEach {
            it.setTextColor(if (it == active) Color.parseColor("#FFD700") else Color.parseColor("#9A8AC4"))
            it.setTypeface(null, if (it == active) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun showRoomsTab() {
        highlightTab(tabRooms)
        mainBody.removeAllViews()

        val officialCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
            background = makeBox("#2E1463", "#FFD700", 4, 28f)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 20 }
            addView(TextView(this@MainActivity).apply {
                text = "🛡️ OFFICIAL VIP ROOM  •  🔊 128 ONLINE  •  ID: 100001"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#00FFAA"))
            })
            addView(TextView(this@MainActivity).apply {
                text = "👑 $userName Official VIP Club"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#FFD700"))
                setPadding(0, 10, 0, 8)
            })
            addView(TextView(this@MainActivity).apply {
                text = "🎙️ Host: $userName [Lv.$vipLevel]  •  Tap to Enter →"
                textSize = 12f
                setTextColor(Color.WHITE)
            })
            setOnClickListener {
                openLiveRoom("100001", "👑 $userName Official VIP Club", userName, "Official")
            }
        }
        mainBody.addView(officialCard)

        val catRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 20 }
        }
        listOf("All", "Official", "Royal", "Music", "Games").forEach { cat ->
            val pill = TextView(this).apply {
                text = if (cat == "All") "🔥 All" else cat
                textSize = 12f
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setTextColor(if (selectedCat == cat) Color.parseColor("#1A0B2E") else Color.WHITE)
                setPadding(12, 14, 12, 14)
                background = makeBox(if (selectedCat == cat) "#FFD700" else "#22134A", "#52339E", 2, 24f)
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = 8 }
                setOnClickListener {
                    selectedCat = cat
                    showRoomsTab()
                }
            }
            catRow.addView(pill)
        }
        mainBody.addView(catRow)

        roomsGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        mainBody.addView(roomsGrid)
        loadRoomsData()
    }

    private fun renderTwoColumnRooms(items: List>) {
        roomsGrid.removeAllViews()
        val filtered = if (selectedCat == "All") items else items.filter { it["cat"] == selectedCat }
        var i = 0
        while (i < filtered.size) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 18 }
            }
            row.addView(buildRoomCard(filtered[i], true))
            if (i + 1 < filtered.size) {
                row.addView(buildRoomCard(filtered[i + 1], false))
            } else {
                val spacer = Space(this).apply { layoutParams = LinearLayout.LayoutParams(0, 10, 1f) }
                row.addView(spacer)
            }
            roomsGrid.addView(row)
            i += 2
        }
    }

    private fun buildRoomCard(room: Map<String, String>, leftMargin: Boolean): LinearLayout {
        val id = room["id"] ?: "100101"
        val name = room["name"] ?: "VIP Voice Room"
        val host = room["host"] ?: "Host"
        val cat = room["cat"] ?: "Royal"
        val users = room["users"] ?: "36"
        val isOfficial = id == "100001"

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 22, 22, 22)
            background = makeBox(if (isOfficial) "#2E1463" else "#1E1242", if (isOfficial) "#FFD700" else "#6A42C2", 3, 28f)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                if (leftMargin) rightMargin = 14
            }
            addView(TextView(this@MainActivity).apply {
                text = if (isOfficial) "🛡️ OFFICIAL • 🔊 $users" else "🔴 LIVE  •  🔊 $users"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#00FFAA"))
            })
            addView(TextView(this@MainActivity).apply {
                text = name
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setPadding(0, 14, 0, 14)
                maxLines = 2
            })
            addView(TextView(this@MainActivity).apply {
                text = "👑 $host  •  $cat"
                textSize = 11f
                setTextColor(Color.parseColor("#FFD700"))
            })
            setOnClickListener { openLiveRoom(id, name, host, cat) }
        }
    }

    private fun showGamesTab() {
        highlightTab(tabGames)
        mainBody.removeAllViews()

        mainBody.addView(TextView(this).apply {
            text = "🎰 HAZA GOLD GREEDY & LUCKY SPIN"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFD700"))
            setPadding(0, 10, 0, 20)
        })

        val gameBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
            background = makeBox("#23134D", "#FFD700", 3, 30f)
        }
        gameBox.addView(TextView(this).apply {
            text = "🍒 🍋 🍇 👑 💎\nBet 1,000 Coins — Win up to 10X (10,000 Coins)!"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 24)
        })
        gameBox.addView(Button(this).apply {
            text = "🎲 SPIN NOW (1,000 Coins)"
            setOnClickListener {
                if (currentCoins >= 1000) {
                    val win = listOf(0, 500, 2000, 5000, 10000).random()
                    currentCoins = currentCoins - 1000 + win
                    currentXp += 2500
                    val autoLvl = (currentXp / 10000).coerceIn(1, 100)
                    if (autoLvl > vipLevel) vipLevel = autoLvl
                    saveStats()
                    Toast.makeText(this@MainActivity, if (win > 0) "🎉 WON $win Coins (+2500 XP)!" else "+2500 XP Added!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Not enough coins!", Toast.LENGTH_SHORT).show()
                }
            }
        })
        mainBody.addView(gameBox)
    }

    private fun showChatTab() {
        highlightTab(tabChat)
        mainBody.removeAllViews()
        listOf(
            "🛡️ Haza Level System Active: All users start at Lv.1 and level up to Lv.100 via XP!",
            "👑 Owner Power Active: Upgrade any user's Level (1-100), Fancy ID & Coins in 'Me' tab.",
            "🎙️ Official Room (ID: 100001) is LIVE & Pinned at #1."
        ).forEach { msg ->
            mainBody.addView(TextView(this).apply {
                text = msg
                textSize = 14f
                setTextColor(Color.WHITE)
                setPadding(26, 24, 26, 24)
                background = makeBox("#1E1242", "#4B2E94", 2, 24f)
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 16 }
            })
        }
    }

    private fun showMeTab() {
        highlightTab(tabMe)
        mainBody.removeAllViews()

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
            background = makeBox("#23134D", "#FFD700", 3, 28f)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 22 }
        }
        card.addView(TextView(this).apply {
            text = "👑 $userName [Lv.$vipLevel OWNER]\nOfficial Room ID: 100001\nMy Fancy ID: $shortUid\nWallet: 🪙 $currentCoins Coins  •  ⚡ $currentXp XP"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 20)
        })
        card.addView(Button(this).apply {
            text = "🎙️ Enter My Official Room (100001)"
            setOnClickListener {
                openLiveRoom("100001", "👑 $userName Official VIP Club", userName, "Official")
            }
        })
        mainBody.addView(card)

        val adminBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
            background = makeBox("#1A0D3D", "#00FFAA", 3, 28f)
        }
        adminBox.addView(TextView(this).apply {
            text = "🛡️ OWNER POWER: LEVEL (1-100), FANCY ID & COINS"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#00FFAA"))
            setPadding(0, 0, 0, 14)
        })

        val etTargetId = EditText(this).apply {
            hint = "Target User ID (e.g. $shortUid)"
            setText(shortUid)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val etFancyId = EditText(this).apply {
            hint = "Assign New Fancy VIP ID (Optional)"
            setText(shortUid)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val etNewLevel = EditText(this).apply {
            hint = "Set Haza Level (1 - 100)"
            setText("100")
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val etAddCoins = EditText(this).apply {
            hint = "Add Bonus Coins (e.g. 100000)"
            setText("100000")
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        adminBox.addView(etTargetId)
        adminBox.addView(etFancyId)
        adminBox.addView(etNewLevel)
        adminBox.addView(etAddCoins)

        adminBox.addView(Button(this).apply {
            text = "⚡ APPLY LEVEL, ID & COINS"
            setOnClickListener {
                val targetId = etTargetId.text.toString().trim().ifEmpty { shortUid }
                val newFancy = etFancyId.text.toString().trim().ifEmpty { targetId }
                val newLvl = (etNewLevel.text.toString().toIntOrNull() ?: 50).coerceIn(1, 100)
                val bonusCoins = etAddCoins.text.toString().toIntOrNull() ?: 0

                if (targetId == shortUid) {
                    shortUid = newFancy
                    prefs.edit().putString("short_uid", shortUid).apply()
                    vipLevel = newLvl
                    currentCoins += bonusCoins
                    currentXp = newLvl * 10000
                    saveStats()
                    showMeTab()
                } else {
                    dbRef.child("users").child(targetId).updateChildren(
                        mapOf("uid" to newFancy, "vipLevel" to newLvl, "xp" to newLvl * 10000, "bonusCoins" to bonusCoins)
                    )
                }
                Toast.makeText(this@MainActivity, "✅ Updated ID $newFancy to Lv.$newLvl (+$bonusCoins Coins)!", Toast.LENGTH_LONG).show()
            }
        })
        mainBody.addView(adminBox)
    }

    private fun openLiveRoom(roomId: String, roomName: String, hostName: String, category: String) {
        try {
            startActivity(Intent(this, LiveRoomActivity::class.java).apply {
                putExtra("ROOM_ID", roomId)
                putExtra("ROOM_NAME", roomName)
                putExtra("HOST_NAME", hostName)
                putExtra("CATEGORY", category)
                putExtra("USER_ID", shortUid)
        
