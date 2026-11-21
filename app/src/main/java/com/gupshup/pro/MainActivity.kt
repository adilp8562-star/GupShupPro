package com.gupshup.pro

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
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
    private val ADMIN_UPI_ID = "adilp8562@okaxis"

    private var currentCoins = 500000
    private var currentXp = 100000
    private var shortUid = "100001"
    private var userName = "Its Meroyal"
    private var userBio = "Welcome to my Royal Voice Party! 👑✨"
    private var userGender = "Male"
    private var userAvatar = "👑"
    private var isSuperAdmin = false
    private var selectedCategory = "All"

    private lateinit var dbRef: DatabaseReference
    private lateinit var prefs: android.content.SharedPreferences

    private lateinit var tvTopMe: TextView
    private lateinit var tvTopPopular: TextView
    private lateinit var indTopMe: View
    private lateinit var indTopPopular: View
    private lateinit var topBarHeader: LinearLayout

    private lateinit var pageMeHome: ScrollView
    private lateinit var pagePopularHome: ScrollView
    private lateinit var pageGamesHub: ScrollView
    private lateinit var pageMessages: ScrollView
    private lateinit var pageProfileMe: ScrollView

    private lateinit var meRoomsGrid: LinearLayout
    private lateinit var popularRoomsGrid: LinearLayout
    private lateinit var tvMyRoomName: TextView
    private lateinit var tvProfileAvatar: TextView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileUid: TextView
    private lateinit var tvProfileCoins: TextView
    private lateinit var layoutSuperAdmin: LinearLayout

    private val bottomNavItems = mutableListOf<TextView>()
    private val catPillViews = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            requestAudioPermission()
            prefs = getSharedPreferences("GupShupPrefs", MODE_PRIVATE)
            shortUid = prefs.getString("user_uid", "100001") ?: "100001"
            userName = prefs.getString("user_name", "Its Meroyal") ?: "Its Meroyal"
            userBio = prefs.getString("user_bio", "Welcome to my Royal Voice Party! 👑✨") ?: "Welcome to my Royal Voice Party! 👑✨"
            userGender = prefs.getString("user_gender", "Male") ?: "Male"
            userAvatar = prefs.getString("user_avatar", "👑") ?: "👑"
            currentCoins = prefs.getInt("user_coins", 500000)
            currentXp = prefs.getInt("user_xp", 100000)
            isSuperAdmin = prefs.getBoolean("is_super_admin", false)
            dbRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).reference
            buildCompleteHazaUI()
            syncFirebaseUser()
            loadRoomsData()
        } catch (e: Exception) {
            setContentView(TextView(this).apply { text = "Error: ${e.message}"; setTextColor(Color.WHITE); setBackgroundColor(Color.BLACK); setPadding(40, 40, 40, 40) })
        }
    }

    private fun makeRoundedBg(colorHex: String, radiusDp: Float, strokeColor: String? = null, strokeWidth: Int = 0): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(colorHex))
            cornerRadius = radiusDp * resources.displayMetrics.density
            if (strokeColor != null && strokeWidth > 0) setStroke(strokeWidth, Color.parseColor(strokeColor))
        }
    }

    private fun makeGradientBg(startHex: String, endHex: String, radiusDp: Float): GradientDrawable {
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor(startHex), Color.parseColor(endHex))).apply {
            cornerRadius = radiusDp * resources.displayMetrics.density
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun buildCompleteHazaUI() {
        val rootFrame = FrameLayout(this).apply { setBackgroundColor(Color.parseColor("#0D101B")) }
        val mainVertical = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        topBarHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#131624"))
            setPadding(dp(16), dp(14), dp(14), dp(10))
        }

        val tabMeBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(0, 0, dp(22), 0) }
        tvTopMe = TextView(this).apply { text = "Me"; textSize = 20f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE) }
        indTopMe = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(26), dp(3)).apply { topMargin = dp(4) }; background = makeRoundedBg("#FFD700", 2f) }
        tabMeBox.addView(tvTopMe); tabMeBox.addView(indTopMe)

        val tabPopBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(0, 0, dp(16), 0) }
        tvTopPopular = TextView(this).apply { text = "Popular"; textSize = 16f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#8A92B2")) }
        indTopPopular = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(42), dp(3)).apply { topMargin = dp(4) }; background = makeRoundedBg("#FFD700", 2f); visibility = View.INVISIBLE }
        tabPopBox.addView(tvTopPopular); tabPopBox.addView(indTopPopular)

        val spacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) }

        val btnSearch = TextView(this).apply {
            text = "🔍"; textSize = 16f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            background = makeRoundedBg("#1E2336", 18f, "#2E3552", 2)
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply { rightMargin = dp(10) }
            setOnClickListener { showSearchRoomDialog() }
        }

        val btnCreateRoom = TextView(this).apply {
            text = "+ Room"; textSize = 13f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#111111"))
            gravity = Gravity.CENTER; setPadding(dp(14), dp(8), dp(14), dp(8))
            background = makeGradientBg("#FFD700", "#FF9800", 20f)
            setOnClickListener { showCreateRoomDialog() }
        }

        topBarHeader.addView(tabMeBox); topBarHeader.addView(tabPopBox); topBarHeader.addView(spacer); topBarHeader.addView(btnSearch); topBarHeader.addView(btnCreateRoom)
        mainVertical.addView(topBarHeader)

        val contentArea = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        pageMeHome = buildMeHomePage()
        pagePopularHome = buildPopularHomePage()
        pageGamesHub = buildGamesAndTasksPage()
        pageMessages = buildOneOnOneChatPage()
        pageProfileMe = buildProfileMePage()

        contentArea.addView(pageMeHome)
        contentArea.addView(pagePopularHome)
        contentArea.addView(pageGamesHub)
        contentArea.addView(pageMessages)
        contentArea.addView(pageProfileMe)
        mainVertical.addView(contentArea)

        val bottomNav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#131624"))
            setPadding(0, dp(8), 0, dp(10))
        }
        val navLabels = listOf("🏠\nParty", "🎮\nGames", "💬\n1-on-1", "👤\nProfile")
        navLabels.forEachIndexed { idx, label ->
            val tv = TextView(this).apply {
                text = label; textSize = 12f; gravity = Gravity.CENTER
                setTypeface(null, if (idx == 0) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (idx == 0) Color.parseColor("#FFD700") else Color.parseColor("#8A92B2"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { switchMainTab(idx) }
            }
            bottomNavItems.add(tv); bottomNav.addView(tv)
        }
        mainVertical.addView(bottomNav)
        rootFrame.addView(mainVertical)
        setContentView(rootFrame)

        tabMeBox.setOnClickListener { switchHomeSubTab(true) }
        tabPopBox.setOnClickListener { switchHomeSubTab(false) }
        switchMainTab(0)
        switchHomeSubTab(true)
    }    private fun switchHomeSubTab(isMe: Boolean) {
        pageMeHome.visibility = if (isMe) View.VISIBLE else View.GONE
        pagePopularHome.visibility = if (!isMe) View.VISIBLE else View.GONE
        tvTopMe.setTextColor(if (isMe) Color.WHITE else Color.parseColor("#8A92B2"))
        tvTopPopular.setTextColor(if (!isMe) Color.WHITE else Color.parseColor("#8A92B2"))
        indTopMe.visibility = if (isMe) View.VISIBLE else View.INVISIBLE
        indTopPopular.visibility = if (!isMe) View.VISIBLE else View.INVISIBLE
    }

    private fun switchMainTab(idx: Int) {
        topBarHeader.visibility = if (idx == 0) View.VISIBLE else View.GONE
        pageMeHome.visibility = if (idx == 0 && indTopMe.visibility == View.VISIBLE) View.VISIBLE else View.GONE
        pagePopularHome.visibility = if (idx == 0 && indTopPopular.visibility == View.VISIBLE) View.VISIBLE else View.GONE
        pageGamesHub.visibility = if (idx == 1) View.VISIBLE else View.GONE
        pageMessages.visibility = if (idx == 2) View.VISIBLE else View.GONE
        pageProfileMe.visibility = if (idx == 3) View.VISIBLE else View.GONE
        bottomNavItems.forEachIndexed { i, tv ->
            tv.setTextColor(if (i == idx) Color.parseColor("#FFD700") else Color.parseColor("#8A92B2"))
            tv.setTypeface(null, if (i == idx) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun buildMeHomePage(): ScrollView {
        val scroll = ScrollView(this)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(24)) }

        val myRoomBanner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeGradientBg("#4A148C", "#1A237E", 18f)
            setPadding(dp(18), dp(16), dp(18), dp(16))
            setOnClickListener { openLiveRoom(shortUid, "👑♡♡$userName♡♡👑", userName, "Royal") }
        }
        val tagMy = TextView(this).apply { text = "👑 MY ROYAL PARTY ROOM"; textSize = 11f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#FFD700")) }
        tvMyRoomName = TextView(this).apply { text = "👑♡♡$userName♡♡👑"; textSize = 19f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE); setPadding(0, dp(4), 0, dp(4)) }
        val subMy = TextView(this).apply { text = "Tap to enter & host your live voice party 🎙️"; textSize = 12f; setTextColor(Color.parseColor("#D1C4E9")) }
        myRoomBanner.addView(tagMy); myRoomBanner.addView(tvMyRoomName); myRoomBanner.addView(subMy)
        col.addView(myRoomBanner)

        val quickRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(14), 0, dp(6)) }
        val qItems = listOf("🧧 Lucky Bag" to { switchMainTab(1) }, "🎯 Ring Toss" to { switchMainTab(1) }, "💬 1-on-1 Chat" to { switchMainTab(2) })
        qItems.forEachIndexed { i, pair ->
            val btn = TextView(this).apply {
                text = pair.first; textSize = 12f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE); gravity = Gravity.CENTER
                background = makeRoundedBg("#1B2034", 12f, "#2E3654", 2)
                setPadding(dp(8), dp(10), dp(8), dp(10))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { if (i < 2) rightMargin = dp(8) }
                setOnClickListener { pair.second() }
            }
            quickRow.addView(btn)
        }
        col.addView(quickRow)

        val secTitle = TextView(this).apply { text = "⭐ Followed & Recent Rooms"; textSize = 15f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE); setPadding(0, dp(14), 0, dp(10)) }
        col.addView(secTitle)

        meRoomsGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(meRoomsGrid)
        scroll.addView(col)
        return scroll
    }

    private fun buildPopularHomePage(): ScrollView {
        val scroll = ScrollView(this)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(10), dp(14), dp(24)) }

        val eventCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeGradientBg("#C2185B", "#512DA8", 16f)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setOnClickListener { switchMainTab(1) }
        }
        eventCard.addView(TextView(this).apply { text = "🎉 HAZA FESTIVAL & LUCKY RED ENVELOPES"; textSize = 11f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#FFD700")) })
        eventCard.addView(TextView(this).apply { text = "Win up to 50,000 Coins in Ring Toss & Greedy Wheel!"; textSize = 15f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE); setPadding(0, dp(4), 0, 0) })
        col.addView(eventCard)

        val catScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; setPadding(0, dp(12), 0, dp(12)) }
        val catRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val categories = listOf("All", "Party", "Music", "Dating", "Royal", "Games")
        categories.forEach { cat ->
            val pill = TextView(this).apply {
                text = cat; textSize = 13f; setTypeface(null, Typeface.BOLD)
                setTextColor(if (cat == selectedCategory) Color.parseColor("#111111") else Color.WHITE)
                background = if (cat == selectedCategory) makeRoundedBg("#FFD700", 16f) else makeRoundedBg("#1B2034", 16f)
                setPadding(dp(14), dp(6), dp(14), dp(6))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { rightMargin = dp(8) }
                setOnClickListener {
                    selectedCategory = cat
                    catPillViews.forEach { p ->
                        val active = p.text.toString() == selectedCategory
                        p.setTextColor(if (active) Color.parseColor("#111111") else Color.WHITE)
                        p.background = if (active) makeRoundedBg("#FFD700", 16f) else makeRoundedBg("#1B2034", 16f)
                    }
                    loadRoomsData()
                }
            }
            catPillViews.add(pill); catRow.addView(pill)
        }
        catScroll.addView(catRow)
        col.addView(catScroll)

        popularRoomsGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(popularRoomsGrid)
        scroll.addView(col)
        return scroll
    }

    private fun renderHazaTwoColumnGrid(targetGrid: LinearLayout, rooms: List>) {
        targetGrid.removeAllViews()
        val colors = listOf("#1E294B" to "#3B1E54", "#1B3B48" to "#1E2749", "#3D1
                            
        private fun buildGamesAndTasksPage(): ScrollView {
        val scroll = ScrollView(this)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(24)) }
        col.addView(TextView(this).apply { text = "🎮 Haza Party Games & Daily Tasks"; textSize = 20f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE); setPadding(0, 0, 0, dp(14)) })

        val games = listOf(
            Triple("🎡 30s Greedy Gold Wheel", "Bet coins on Pizza, Burger, Crown & win up to 15x!", "#6A1B9A") {
                try {
                    startActivity(Intent(this, GreedyGameActivity::class.java).apply { putExtra("USER_ID", shortUid); putExtra("USER_COINS", currentCoins) })
                } catch (_: Exception) {
                    playLuckyRedEnvelope()
                }
            },
            Triple("🧧 Lucky Red Envelope", "Open a royal red packet for instant free bonus coins!", "#B71C1C") { playLuckyRedEnvelope() },
            Triple("🎯 Royal Ring Toss", "Toss a golden ring (200 Coins) to win up to 2,500 Coins!", "#00695C") { playRingTossMiniGame() },
            Triple("📅 Daily Check-In & Task", "Claim +3,000 Coins & +1,500 Royal XP every day!", "#1565C0") { claimDailyTaskReward() }
        )

        games.forEach { (gTitle, gDesc, gHex, action) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = makeRoundedBg(gHex, 16f)
                setPadding(dp(16), dp(14), dp(16), dp(14))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12) }
                setOnClickListener { action() }
            }
            card.addView(TextView(this).apply { text = gTitle; textSize = 16f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE) })
            card.addView(TextView(this).apply { text = gDesc; textSize = 12f; setTextColor(Color.parseColor("#E0E0E0")); setPadding(0, dp(4), 0, 0) })
            col.addView(card)
        }
        scroll.addView(col)
        return scroll
    }

    private fun buildOneOnOneChatPage(): ScrollView {
        val scroll = ScrollView(this)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(24)) }

        val headerRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, dp(14)) }
        headerRow.addView(TextView(this).apply { text = "💬 Private 1-on-1 Chats"; textSize = 20f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
        val btnNewChat = TextView(this).apply {
            text = "+ New Chat"; textSize = 12f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#111111"))
            background = makeRoundedBg("#FFD700", 14f); setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener { showStartPrivateChatDialog() }
        }
        headerRow.addView(btnNewChat)
        col.addView(headerRow)

        val friends = listOf(
            Triple("👸 Ayisha Queen", "ID: 100002 • Online", "Hey! Join my Royal Shayari room tonight? 👑"),
            Triple("🎧 DJ Kabir", "ID: 100003 • In Voice Room", "New Bollywood Lofi track ready for your party!"),
            Triple("💖 Riya Live", "ID: 100004 • Online", "Thanks for the Rose gift in party room! 🌹"),
            Triple("🛡️ Haza Official Support", "ID: 100000 • Verified", "Welcome to GupShupPro! 24/7 Safety & VIP Support active.")
        )

        friends.forEach { (fName, fStatus, lastMsg) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = makeRoundedBg("#181D30", 14f, "#28304D", 2)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10) }
                setOnClickListener { openOneOnOneConversation(fName) }
            }
            card.addView(TextView(this).apply { text = fName; textSize = 15f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE) })
            card.addView(TextView(this).apply { text = fStatus; textSize = 11f; setTextColor(Color.parseColor("#4CAF50")); setPadding(0, dp(2), 0, dp(4)) })
            card.addView(TextView(this).apply { text = lastMsg; textSize = 13f; setTextColor(Color.parseColor("#B0BEC5")) })
            col.addView(card)
        }
            private fun buildProfileMePage(): ScrollView {
        val scroll = ScrollView(this)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(28)) }

        val headerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeGradientBg("#232946", "#161B33", 18f)
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setOnLongClickListener {
                isSuperAdmin = !isSuperAdmin
                prefs.edit().putBoolean("is_super_admin", isSuperAdmin).apply()
                layoutSuperAdmin.visibility = if (isSuperAdmin) View.VISIBLE else View.GONE
                Toast.makeText(this, if (isSuperAdmin) "👑 Super Admin Mode Unlocked!" else "Admin Mode Hidden", Toast.LENGTH_SHORT).show()
                true
            }
        }
        tvProfileAvatar = TextView(this).apply { text = userAvatar; textSize = 36f }
        tvProfileName = TextView(this).apply { text = "$userName 👑"; textSize = 20f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE); setPadding(0, dp(6), 0, dp(2)) }
        tvProfileUid = TextView(this).apply {
            text = "ID: $shortUid  📋 (Tap to copy)"; textSize = 12f; setTextColor(Color.parseColor("#FFD700"))
            setOnClickListener { copyUidToClipboard() }
        }
        tvProfileCoins = TextView(this).apply { text = "🪙 $currentCoins Coins  \vert{}  ⚡$currentXp XP"; textSize = 14f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#80CBC4")); setPadding(0, dp(10), 0, 0) }
        headerCard.addView(tvProfileAvatar); headerCard.addView(tvProfileName); headerCard.addView(tvProfileUid); headerCard.addView(tvProfileCoins)
        col.addView(headerCard)

        val menuItems = listOf(
            "🪙 Coin Recharge Store ($1 = 10,000 Coins)" to { showRechargeStoreDialog() },
            "✏️ Edit Profile & Bio" to { showEditProfileDialog() },
            "🛡️ Safety Center & Female Shield" to { showSafetyCenterDialog() },
            "🎙️ Enter My Royal Voice Room" to { openLiveRoom(shortUid, "👑♡♡$userName♡♡👑", userName, "Royal") }
        )

        menuItems.forEach { (label, action) ->
            val item = TextView(this).apply {
                text = label; textSize = 14f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE)
                background = makeRoundedBg("#181D30", 14f)
                setPadding(dp(16), dp(14), dp(16), dp(14))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) }
                setOnClickListener { action() }
            }
            col.addView(item)
        }

        layoutSuperAdmin = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeRoundedBg("#3E1420", 14f, "#FFD700", 2)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            visibility = if (isSuperAdmin) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) }
        }
        layoutSuperAdmin.addView(TextView(this).apply { text = "⚡ OWNER / SUPER ADMIN PANEL"; textSize = 14f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#FFD700")) })
        val btnAddCoins = TextView(this).apply {
            text = "+ Add 100,000 Free Owner Coins"; textSize = 13f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#111111"))
            background = makeRoundedBg("#FFD700", 12f); setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
            setOnClickListener {
                currentCoins += 100000; saveLocalStats()
                Toast.makeText(this@MainActivity, "+100,000 Coins Added! 👑", Toast.LENGTH_SHORT).show()
            }
        }
        layoutSuperAdmin.addView(btnAddCoins)
        col.addView(layoutSuperAdmin)

        scroll.addView(col)
        return scroll    private fun playLuckyRedEnvelope() {
        val reward = listOf(500, 1000, 2500, 5000).random()
        currentCoins += reward
        saveLocalStats()
        AlertDialog.Builder(this)
            .setTitle("🧧 Lucky Red Envelope Opened!")
            .setMessage("Mubarak ho! You caught a Lucky Red Envelope worth +$reward Coins! 🪙✨")
            .setPositiveButton("Awesome!", null)
            .show()
    }

    private fun playRingTossMiniGame() {
        if (currentCoins < 200) {
            Toast.makeText(this, "Need 200 Coins to throw a ring!", Toast.LENGTH_SHORT).show()
            return
        }
        currentCoins -= 200
        val win = if (Random.nextInt(100) < 55) listOf(400, 1000, 2500).random() else 0
        currentCoins += win
        saveLocalStats()
        val msg = if (win > 0) "🎯 Bullseye! Your ring landed on the Royal Crown! Won +$win Coins!" else "😅 Missed the peg! Try another ring toss!"
        AlertDialog.Builder(this).setTitle("🎯 Royal Ring Toss").setMessage(msg).setPositiveButton("OK", null).show()
    }

    private fun claimDailyTaskReward() {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        if (prefs.getString("last_daily_claim", "") == today) {
            Toast.makeText(this, "✅ Today's Daily Task reward already claimed!", Toast.LENGTH_SHORT).show()
        } else {
            prefs.edit().putString("last_daily_claim", today).apply()
            currentCoins += 3000; currentXp += 1500
            saveLocalStats()
            AlertDialog.Builder(this).setTitle("📅 Daily Task Completed!").setMessage("Claimed +3,000 Coins & +1,500 XP! 🎉").setPositiveButton("Great", null).show()
        }
    }

    private fun showStartPrivateChatDialog() {
        val et = EditText(this).apply { hint = "Enter Friend Name or User ID (e.g. 100005)" }
        AlertDialog.Builder(this).setTitle("💬 Start 1-on-1 Chat").setView(et)
            .setPositiveButton("Open Chat") { _, _ ->
                val target = et.text.toString().trim()
                if (target.isNotEmpty()) openOneOnOneConversation(target)
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showSearchRoomDialog() {
        val et = EditText(this).apply { hint = "Enter Room ID or Topic..." }
        AlertDialog.Builder(this).setTitle("🔍 Search Voice Rooms").setView(et)
            .setPositiveButton("Join") { _, _ ->
                val q = et.text.toString().trim()
                if (q.isNotEmpty()) openLiveRoom(q, "🎙️ $q Party Room", "Host", "Party")
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showCreateRoomDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(12), dp(18), dp(8)) }
        val etName = EditText(this).apply { hint = "Room Title (e.g. Late Night Music & Shayari)" }
        val rg = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        listOf("Party", "Music", "Dating", "Royal").forEachIndexed { idx, cat ->
            rg.addView(RadioButton(this).apply { text = cat; isChecked = idx == 0 })
        }
        box.addView(etName); box.addView(rg)
        AlertDialog.Builder(this).setTitle("🎙️ Create Haza Voice Room").setView(box)
            .setPositiveButton("Go Live") { _, _ ->
                val title = etName.text.toString().trim().ifEmpty { "👑♡♡$userName♡♡👑" }
                val checkedBtn = box.findViewById<RadioButton>(rg.checkedRadioButtonId)
                val cat = checkedBtn?.text?.toString() ?: "Party"
                val roomId = (100200 + Random.nextInt(700)).toString()
                val roomData = mapOf("id" to roomId, "name" to title, "host" to userName, "cat" to cat, "count" to "1")
                dbRef.child("rooms").child(roomId).setValue(roomData)
                openLiveRoom(roomId, title, userName, cat)
            }.setNegativeButton("Cancel", null).show()
    }
    
            }
                private fun showRechargeStoreDialog() {
        val packs = arrayOf("₹85 ($1) ➔ 10,000 Coins", "₹425 ($5) ➔ 55,000 Coins 🔥", "₹850 ($10) ➔ 120,000 VIP Coins 👑")
        AlertDialog.Builder(this).setTitle("🪙 Haza Coin Store ($1 = 10,000 Coins)")
            .setItems(packs) { _, which ->
                val coinsToAdd = listOf(10000, 55000, 120000)[which]
                try {
                    val uri = Uri.parse("upi://pay?pa=$ADMIN_UPI_ID&pn=GupShupPro&cu=INR")
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (_: Exception) { }
                currentCoins += coinsToAdd; saveLocalStats()
                Toast.makeText(this, "+$coinsToAdd Coins credited to your wallet! 🪙", Toast.LENGTH_LONG).show()
            }.setNegativeButton("Close", null).show()
    }

    private fun showEditProfileDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(12), dp(18), dp(8)) }
        val etName = EditText(this).apply { setText(userName); hint = "Display Name" }
        val etBio = EditText(this).apply { setText(userBio); hint = "Bio / Status" }
        box.addView(etName); box.addView(etBio)
        AlertDialog.Builder(this).setTitle("✏️ Edit Profile").setView(box)
            .setPositiveButton("Save") { _, _ ->
                userName = etName.text.toString().trim().ifEmpty { userName }
                userBio = etBio.text.toString().trim().ifEmpty { userBio }
                prefs.edit().putString("user_name", userName).putString("user_bio", userBio).apply()
                tvProfileName.text = "$userName 👑"
                tvMyRoomName.text = "👑♡♡$userName♡♡👑"
                syncFirebaseUser()
                Toast.makeText(this, "Profile Updated! ✅", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showSafetyCenterDialog() {
        AlertDialog.Builder(this)
            .setTitle("🛡️ Haza Safety & Female Protection")
            .setMessage("• 24/7 Anti-Harassment & Female User Shield Active ✅\n• Room Hosts can Mute, Kick, or Lock seats anytime.\n• Instant Block & Report available in all rooms & 1-on-1 chats.")
            .setPositiveButton("Report an Issue") { _, _ ->
                Toast.makeText(this, "Safety Report ticket submitted to Moderation Team 🛡️", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("OK", null).show()
    }

    private fun copyUidToClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("UID", shortUid))
        Toast.makeText(this, "Copied ID: $shortUid ✅", Toast.LENGTH_SHORT).show()
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
        tvProfileCoins.text = "🪙 $currentCoins Coins  |  ⚡ $currentXp XP"
        syncFirebaseUser()
    }

    private fun syncFirebaseUser() {
        val data = mapOf("uid" to shortUid, "name" to userName, "coins" to currentCoins, "xp" to currentXp, "gender" to userGender)
        dbRef.child("users").child(shortUid).updateChildren(data)
    }

    private fun loadRoomsData() {
        val defaultRooms = listOf(
            mapOf("id" to "100101", "name" to "🎶 Late Night Bollywood & Lofi", "host" to "DJ Kabir", "cat" to "Music", "count" to "128"),
            mapOf("id" to "100102", "name" to "👑 Royal Shayari & Friendship", "host" to "Ayisha Queen", "cat" to "Royal", "count" to "94"),
            mapOf("id" to "100103", "name" to "💘 Dil Se Dil Tak 1-on-1 & Chat", "host" to "Riya Live", "cat" to "Dating", "count" to "215"),
            mapOf("id" to "100104", "name" to "🧧 Lucky Bag & Ring Toss Party", "host" to "Its Meroyal", "cat" to "Games", "count" to "176"),
            mapOf("id" to "100105", "name" to "🔥 Non-Stop Punjabi Beats", "host" to "Aryan", "cat" to "Music", "count" to "83"),
            mapOf("id" to "100106", "name" to "🎉 Weekend Mega Voice Club", "host" to "Zoya", "cat" to "Party", "count" to "142")
        )
        val filtered = if (selectedCategory == "All") defaultRooms else defaultRooms.filter { it["cat"].equals(selectedCategory, ignoreCase = true) }
        renderHazaTwoColumnGrid(meRoomsGrid, defaultRooms.take(4))
        renderHazaTwoColumnGrid(popularRoomsGrid, filtered.ifEmpty { defaultRooms })

        dbRef.child("rooms").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val liveList = mutableListOf>()
                for (child in snapshot.children) {
                    val id = child.child("id").getValue(String::class.java) ?: child.key ?: continue
                    val name = child.child("name").getValue(String::class.java) ?: continue
                    val host = child.child("host").getValue(String::class.java) ?: "Host"
                    val cat = child.child("cat").getValue(String::class.java) ?: "Party"
                    val count = child.child("count").getValue(String::class.java) ?: "12"
                    liveList.add(mapOf("id" to id, "name" to name, "host" to host, "cat" to cat, "count" to count))
                }
                val combined = liveList + defaultRooms
                val finalFiltered = if (selectedCategory == "All") combined else combined.filter { it["cat"].equals(selectedCategory, ignoreCase = true) }
                renderHazaTwoColumnGrid(meRoomsGrid, combined.take(4))
                renderHazaTwoColumnGrid(popularRoomsGrid, finalFiltered.ifEmpty { combined })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
    }
    }
    
