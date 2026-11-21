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
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
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
        return GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.parseColor(startHex), Color.parseColor(endHex))).apply {
            cornerRadius = radiusDp * resources.displayMetrics.density
        }
    }
        private fun buildCompleteHazaUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F7FA"))
        }

        topBarHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(36, 20, 36, 16)
        }

        val meTabWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 32, 0)
            setOnClickListener { switchMainSection(0) }
        }
        tvTopMe = TextView(this).apply {
            text = "Me"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#8E96AA"))
        }
        indTopMe = View(this).apply {
            background = makeRoundedBg("#00C49A", 4f)
            layoutParams = LinearLayout.LayoutParams(56, 10).apply { topMargin = 6 }
            visibility = View.INVISIBLE
        }
        meTabWrap.addView(tvTopMe)
        meTabWrap.addView(indTopMe)

        val popTabWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = makeRoundedBg("#E6F9F4", 20f)
            setPadding(28, 8, 28, 8)
            setOnClickListener { switchMainSection(1) }
        }
        tvTopPopular = TextView(this).apply {
            text = "Popular"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1D26"))
        }
        indTopPopular = View(this).apply {
            background = makeRoundedBg("#00C49A", 4f)
            layoutParams = LinearLayout.LayoutParams(80, 10).apply { topMargin = 4 }
        }
        popTabWrap.addView(tvTopPopular)
        popTabWrap.addView(indTopPopular)

        val topSpacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) }

        val btnSearch = TextView(this).apply {
            text = "🔍"
            textSize = 20f
            setPadding(20, 10, 20, 10)
            setOnClickListener { showSearchRoomDialog() }
        }

        val btnCreateHouse = TextView(this).apply {
            text = "🏠+"
            textSize = 18f
            setTextColor(Color.WHITE)
            background = makeGradientBg("#00C9F6", "#6D28D9", 14f)
            setPadding(22, 10, 22, 10)
            setOnClickListener { showCreateRoomDialog() }
        }

        topBarHeader.addView(meTabWrap)
        topBarHeader.addView(popTabWrap)
        topBarHeader.addView(topSpacer)
        topBarHeader.addView(btnSearch)
        topBarHeader.addView(btnCreateHouse)
        root.addView(topBarHeader)

        val contentFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        pageMeHome = buildMeHomeTab()
        pagePopularHome = buildPopularHomeTab()
        pageGamesHub = buildGamesHubTab()
        pageMessages = buildMessagesTab()
        pageProfileMe = buildProfileMeTab()

        contentFrame.addView(pageMeHome)
        contentFrame.addView(pagePopularHome)
        contentFrame.addView(pageGamesHub)
        contentFrame.addView(pageMessages)
        contentFrame.addView(pageProfileMe)
        root.addView(contentFrame)

        val bottomNav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(0, 18, 0, 18)
            elevation = 16f
        }

        val navLabels = listOf("🏠\nParty", "🎮\nGames", "💬\nChat (1)", "👤\nProfile")
        navLabels.forEachIndexed { idx, label ->
            val tv = TextView(this).apply {
                text = label
                textSize = 12f
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setTextColor(if (idx == 0) Color.parseColor("#00C49A") else Color.parseColor("#8E96AA"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    when (idx) {
                        0 -> switchMainSection(1)
                        1 -> switchMainSection(2)
                        2 -> switchMainSection(3)
                        3 -> switchMainSection(4)
                    }
                }
            }
            bottomNavItems.add(tv)
            bottomNav.addView(tv)
        }
        root.addView(bottomNav)
        setContentView(root)
        switchMainSection(1)
        }
            private fun switchMainSection(sec: Int) {
        pageMeHome.visibility = if (sec == 0) View.VISIBLE else View.GONE
        pagePopularHome.visibility = if (sec == 1) View.VISIBLE else View.GONE
        pageGamesHub.visibility = if (sec == 2) View.VISIBLE else View.GONE
        pageMessages.visibility = if (sec == 3) View.VISIBLE else View.GONE
        pageProfileMe.visibility = if (sec == 4) View.VISIBLE else View.GONE
        topBarHeader.visibility = if (sec == 0 || sec == 1) View.VISIBLE else View.GONE

        tvTopMe.setTextColor(if (sec == 0) Color.parseColor("#1A1D26") else Color.parseColor("#8E96AA"))
        tvTopPopular.setTextColor(if (sec == 1) Color.parseColor("#1A1D26") else Color.parseColor("#8E96AA"))
        indTopMe.visibility = if (sec == 0) View.VISIBLE else View.INVISIBLE
        indTopPopular.visibility = if (sec == 1) View.VISIBLE else View.INVISIBLE

        val activeNav = when (sec) { 0, 1 -> 0; 2 -> 1; 3 -> 2; else -> 3 }
        bottomNavItems.forEachIndexed { i, tv ->
            tv.setTextColor(if (i == activeNav) Color.parseColor("#00C49A") else Color.parseColor("#8E96AA"))
        }
    }

    private fun buildPopularHomeTab(): ScrollView {
        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 20, 28, 40)
        }

        val banner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeGradientBg("#4C1D95", "#B45309", 24f)
            setPadding(36, 36, 36, 36)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 24 }
            setOnClickListener { showWealthPodiumDialog() }
        }
        banner.addView(TextView(this).apply {
            text = "🏆 WEALTH & CHARM 2X PODIUM"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFD700"))
        })
        banner.addView(TextView(this).apply {
            text = "Top 10 Weekly Ranking • Tap to View Live Podium ➔"
            textSize = 13f
            setTextColor(Color.WHITE)
            setPadding(0, 8, 0, 0)
        })
        box.addView(banner)

        val quickRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 24 }
        }
        val qItems = listOf("🏆\nPodium", "🎡\nGreedy", "🎁\n200K Free", "💎\nVIP")
        qItems.forEachIndexed { idx, label ->
            val card = TextView(this).apply {
                text = label
                textSize = 13f
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1A1D26"))
                background = makeRoundedBg("#FFFFFF", 18f)
                setPadding(12, 24, 12, 24)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (idx < 3) rightMargin = 14
                }
                setOnClickListener {
                    when (idx) {
                        0 -> showWealthPodiumDialog()
                        1 -> startActivity(Intent(this@MainActivity, GreedyGameActivity::class.java))
                        2 -> claimDaily200kCoins()
                        3 -> switchMainSection(4)
                    }
                }
            }
            quickRow.addView(card)
        }
        box.addView(quickRow)

        box.addView(TextView(this).apply {
            text = "🔥 Live Royal Voice Rooms"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1D26"))
            setPadding(4, 0, 0, 16)
        })

        popularRoomsGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(popularRoomsGrid)
        scroll.addView(box)
        return scroll
    }

    private fun buildMeHomeTab(): ScrollView {
        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 20, 28, 40)
        }

        val myRoomCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeGradientBg("#00B4D8", "#4F46E5", 24f)
            setPadding(36, 36, 36, 36)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 24 }
            setOnClickListener { openRoom("100101", true) }
        }
        myRoomCard.addView(TextView(this).apply {
            text = "👑 MY ROYAL PARTY ROOM"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#E0F2FE"))
        })
        tvMyRoomName = TextView(this).apply {
            text = "👑♡♡$userName♡♡👑\nTap to enter your Royal Room"
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 10, 0, 0)
        }
        myRoomCard.addView(tvMyRoomName)
        box.addView(myRoomCard)

        box.addView(TextView(this).apply {
            text = "⭐ Followed & Recent Rooms"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1D26"))
            setPadding(4, 0, 0, 16)
        })

        meRoomsGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(meRoomsGrid)
        scroll.addView(box)
        return scroll
    }
        private fun buildGamesHubTab(): ScrollView {
        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 40)
        }

        box.addView(TextView(this).apply {
            text = "🎮 Royal Arcade & Games"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1D26"))
            setPadding(0, 0, 0, 20)
        })

        val greedyCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeGradientBg("#F59E0B", "#DC2626", 24f)
            setPadding(36, 36, 36, 36)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 20 }
            setOnClickListener { startActivity(Intent(this@MainActivity, GreedyGameActivity::class.java)) }
        }
        greedyCard.addView(TextView(this).apply {
            text = "🎡 GREEDY WHEEL 30s"
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        })
        greedyCard.addView(TextView(this).apply {
            text = "Bet on 8 Royal Items • Win up to 45X Gold Coins! Tap to Play ➔"
            textSize = 14f
            setTextColor(Color.parseColor("#FEF3C7"))
            setPadding(0, 8, 0, 0)
        })
        box.addView(greedyCard)

        val ludoCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeGradientBg("#10B981", "#047857", 24f)
            setPadding(36, 36, 36, 36)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 20 }
            setOnClickListener { showSimplePopup("🎲 Royal Ludo", "4-Player Voice Ludo Room matchmaking opening soon! Play Greedy Wheel right now for 45X rewards.") }
        }
        ludoCard.addView(TextView(this).apply {
            text = "🎲 ROYAL VOICE LUDO"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        })
        ludoCard.addView(TextView(this).apply {
            text = "Play with friends on live mic • Coming Soon"
            textSize = 13f
            setTextColor(Color.parseColor("#D1FAE5"))
            setPadding(0, 6, 0, 0)
        })
        box.addView(ludoCard)

        scroll.addView(box)
        return scroll
    }

    private fun buildMessagesTab(): ScrollView {
        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 40)
        }

        box.addView(TextView(this).apply {
            text = "💬 Messages & System"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1D26"))
            setPadding(0, 0, 0, 20)
        })

        val sysCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeRoundedBg("#FFFFFF", 20f)
            setPadding(32, 28, 32, 28)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 }
            setOnClickListener { showSimplePopup("👑 GupShupPro Official", "Welcome to GupShupPro! Claim your daily 2,00,000 Free Coins every day and climb the Weekly Wealth Podium!") }
        }
        sysCard.addView(TextView(this).apply {
            text = "👑 GupShupPro Official Team"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1D26"))
        })
        sysCard.addView(TextView(this).apply {
            text = "Welcome! Tap here to claim your 2,00,000 Daily Royal Bonus 🎁"
            textSize = 13f
            setTextColor(Color.parseColor("#6B7280"))
            setPadding(0, 8, 0, 0)
        })
        box.addView(sysCard)

        scroll.addView(box)
        return scroll
    }
        private fun buildProfileMeTab(): ScrollView {
        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 40)
        }

        val profCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = makeGradientBg("#1E1B4B", "#4C1D95", 24f)
            setPadding(36, 40, 36, 40)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 20 }
        }
        tvProfileAvatar = TextView(this).apply {
            text = userAvatar
            textSize = 46f
            gravity = Gravity.CENTER
        }
        tvProfileName = TextView(this).apply {
            text = "👑♡♡$userName♡♡👑"
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 10, 0, 4)
        }
        tvProfileUid = TextView(this).apply {
            text = "ID:$shortUid 📋  •  Lv.${(currentXp / 50000) + 1}"
            textSize = 13f
            setTextColor(Color.parseColor("#C7D2FE"))
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("UID", shortUid))
                Toast.makeText(this@MainActivity, "UID $shortUid copied!", Toast.LENGTH_SHORT).show()
            }
        }
        tvProfileCoins = TextView(this).apply {
            text = "🪙 $currentCoins  •  ⚡ $currentXp XP"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFD700"))
            setPadding(0, 12, 0, 0)
        }
        profCard.addView(tvProfileAvatar)
        profCard.addView(tvProfileName)
        profCard.addView(tvProfileUid)
        profCard.addView(tvProfileCoins)
        box.addView(profCard)

        val menuItems = listOf(
            "✏️ Edit Royal Profile & Avatar",
            "🎁 Claim Daily 2,00,000 Free Coins",
            "💎 VIP Royal Store & Recharge",
            "🏆 Weekly Wealth & Charm Podium"
        )
        menuItems.forEachIndexed { idx, label ->
            val btn = TextView(this).apply {
                text = label
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1A1D26"))
                background = makeRoundedBg("#FFFFFF", 18f)
                setPadding(32, 32, 32, 32)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 14 }
                setOnClickListener {
                    when (idx) {
                        0 -> showEditProfileDialog()
                        1 -> claimDaily200kCoins()
                        2 -> showVipRechargeDialog()
                        3 -> showWealthPodiumDialog()
                    }
                }
            }
            box.addView(btn)
        }

        layoutSuperAdmin = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (isSuperAdmin) View.VISIBLE else View.GONE
        }
        val btnAdmin = TextView(this).apply {
            text = "🛡️ SUPER ADMIN PANEL"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = makeRoundedBg("#DC2626", 18f)
            setPadding(32, 32, 32, 32)
            setOnClickListener { showSimplePopup("🛡️ Super Admin", "Admin UID: $shortUid • UPI: $ADMIN_UPI_ID") }
        }
        layoutSuperAdmin.addView(btnAdmin)
        box.addView(layoutSuperAdmin)

        scroll.addView(box)
        return scroll
        }
            private fun loadRoomsData() {
        dbRef.child("rooms").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                popularRoomsGrid.removeAllViews()
                meRoomsGrid.removeAllViews()
                var count = 0
                for (roomSnap in snapshot.children) {
                    val rId = roomSnap.key ?: continue
                    val rName = roomSnap.child("name").getValue(String::class.java) ?: "Royal Party Room #$rId"
                    val rHost = roomSnap.child("hostName").getValue(String::class.java) ?: "Royal Host"
                    val rOnline = roomSnap.child("onlineCount").getValue(Int::class.java) ?: Random.nextInt(12, 85)
                    addRoomCardToGrid(popularRoomsGrid, rId, rName, rHost, rOnline)
                    if (count < 3) addRoomCardToGrid(meRoomsGrid, rId, rName, rHost, rOnline)
                    count++
                }
                if (count == 0) {
                    addRoomCardToGrid(popularRoomsGrid, "100101", "👑♡♡$userName♡♡👑", userName, 48)
                    addRoomCardToGrid(popularRoomsGrid, "100102", "🔥 Dil Se Dosti & Music Party", "Aarav_VIP", 92)
                    addRoomCardToGrid(popularRoomsGrid, "100103", "💎 Royal Shayari & Late Night", "Zoya_Queen", 64)
                    addRoomCardToGrid(meRoomsGrid, "100101", "👑♡♡$userName♡♡👑", userName, 48)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun addRoomCardToGrid(container: LinearLayout, roomId: String, roomName: String, hostName: String, onlineCount: Int) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = makeRoundedBg("#FFFFFF", 20f)
            setPadding(28, 28, 28, 28)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 }
            setOnClickListener { openRoom(roomId, roomId == "100101") }
        }

        val avatar = TextView(this).apply {
            text = "🎙️"
            textSize = 26f
            gravity = Gravity.CENTER
            background = makeGradientBg("#6366F1", "#EC4899", 16f)
            layoutParams = LinearLayout.LayoutParams(120, 120).apply { rightMargin = 24 }
        }

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        info.addView(TextView(this).apply {
            text = roomName
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1D26"))
        })
        info.addView(TextView(this).apply {
            text = "Host: $hostName  •  ID: $roomId"
            textSize = 12f
            setTextColor(Color.parseColor("#6B7280"))
            setPadding(0, 6, 0, 0)
        })

        val badge = TextView(this).apply {
            text = "🔊 $onlineCount"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#059669"))
            background = makeRoundedBg("#D1FAE5", 12f)
            setPadding(18, 10, 18, 10)
        }

        card.addView(avatar)
        card.addView(info)
        card.addView(badge)
        container.addView(card)
    }

    private fun showCreateRoomDialog() {
        val etName = EditText(this).apply {
            hint = "Enter Royal Room Name"
            setText("👑 $userName's Party")
            setPadding(36, 28, 36, 28)
        }
        AlertDialog.Builder(this)
            .setTitle("🏠 Create Royal Voice Room")
            .setView(etName)
            .setPositiveButton("Create & Enter") { _, _ ->
                val rName = etName.text.toString().trim().ifEmpty { "👑 $userName's Party" }
                val newRoomId = (100200 + Random.nextInt(800)).toString()
                val roomData = mapOf("name" to rName, "hostName" to userName, "hostUid" to shortUid, "onlineCount" to 1)
                dbRef.child("rooms").child(newRoomId).setValue(roomData)
                openRoom(newRoomId, true)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSearchRoomDialog() {
        val etId = EditText(this).apply {
            hint = "Enter Room ID (e.g. 100101)"
            setPadding(36, 28, 36, 28)
        }
        AlertDialog.Builder(this)
            .setTitle("🔍 Search Room by ID")
            .setView(etId)
            .setPositiveButton("Join Room") { _, _ ->
                val rId = etId.text.toString().trim()
                if (rId.isNotEmpty()) openRoom(rId, rId == "100101")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
        private fun showWealthPodiumDialog() {
        val msg = """
            🥇 1st: 👑♡♡$userName♡♡👑 — 98,50,000 Wealth
            🥈 2nd: 💎 Zoya_Queen — 74,20,000 Wealth
            🥉 3rd: 🔥 Aarav_VIP — 61,80,000 Wealth
            4️⃣ Royal_Prince — 45,10,000 Wealth
            5️⃣ Dil_Se_King — 38,90,000 Wealth
        """.trimIndent()
        showSimplePopup("🏆 Weekly Wealth & Charm Podium", msg)
    }

    private fun showVipRechargeDialog() {
        val packs = arrayOf(
            "🪙 1,00,000 Gold Coins — ₹99",
            "💎 5,50,000 Gold Coins + VIP1 — ₹499",
            "👑 15,00,000 Gold Coins + Royal VIP — ₹999"
        )
        AlertDialog.Builder(this)
            .setTitle("💎 Royal Coin & VIP Store")
            .setItems(packs) { _, which ->
                val amount = when (which) { 0 -> "99"; 1 -> "499"; else -> "999" }
                val upiUri = Uri.parse("upi://pay?pa=$ADMIN_UPI_ID&pn=GupShupPro&am=$amount&cu=INR")
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, upiUri))
                } catch (e: Exception) {
                    showSimplePopup("💳 UPI Payment", "Send ₹$amount to UPI ID:\n$ADMIN_UPI_ID")
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showEditProfileDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 12)
        }
        val etName = EditText(this).apply { hint = "Nickname"; setText(userName) }
        val etBio = EditText(this).apply { hint = "Bio"; setText(userBio) }
        val etAvatar = EditText(this).apply { hint = "Avatar Emoji"; setText(userAvatar) }
        val rg = RadioGroup(this).apply { orientation = LinearLayout.HORIZONTAL }
        val rbM = RadioButton(this).apply { text = "♂️ Male"; id = View.generateViewId(); isChecked = userGender != "Female" }
        val rbF = RadioButton(this).apply { text = "♀️ Female"; id = View.generateViewId(); isChecked = userGender == "Female" }
        rg.addView(rbM); rg.addView(rbF)
        layout.addView(etName); layout.addView(etBio); layout.addView(etAvatar); layout.addView(rg)
        AlertDialog.Builder(this)
            .setTitle("✏️ Edit VIP Profile")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                userName = etName.text.toString().trim().ifEmpty { userName }
                userBio = etBio.text.toString().trim().ifEmpty { userBio }
                userAvatar = etAvatar.text.toString().trim().ifEmpty { "👑" }
                userGender = if (rbF.isChecked) "Female" else "Male"
                prefs.edit().putString("user_name", userName).putString("user_bio", userBio).putString("user_avatar", userAvatar).putString("user_gender", userGender).apply()
                dbRef.child("users").child(shortUid).updateChildren(mapOf("name" to userName, "bio" to userBio, "avatar" to userAvatar))
                refreshProfileViews()
                Toast.makeText(this, "✅ Profile Saved!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun claimDaily200kCoins() {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        if (prefs.getString("last_daily_claim", "") == today) {
            Toast.makeText(this, "⏳ Already claimed today! Come back tomorrow ✨", Toast.LENGTH_LONG).show()
            return
        }
        currentCoins += 200000
        currentXp += 100000
        prefs.edit().putString("last_daily_claim", today).putInt("user_coins", currentCoins).putInt("user_xp", currentXp).apply()
        dbRef.child("users").child(shortUid).updateChildren(mapOf("coins" to currentCoins, "xp" to currentXp))
        refreshProfileViews()
        showSimplePopup("🎁 2X DAILY BONUS CLAIMED!", "Received 🪙 2,00,000 Coins + ⚡ 1,00,000 EXP!\n\nNew Balance: 🪙 $currentCoins")
    }

    private fun syncFirebaseUser() {
        dbRef.child("users").child(shortUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    snapshot.child("coins").getValue(Int::class.java)?.let {
                        currentCoins = it
                        prefs.edit().putInt("user_coins", currentCoins).apply()
                    }
                    snapshot.child("xp").getValue(Int::class.java)?.let {
                        currentXp = it
                        prefs.edit().putInt("user_xp", currentXp).apply()
                    }
                    userName = snapshot.child("name").getValue(String::class.java) ?: userName
                    userAvatar = snapshot.child("avatar").getValue(String::class.java) ?: userAvatar
                    isSuperAdmin = snapshot.child("isSuperAdmin").getValue(Boolean::class.java) ?: isSuperAdmin
                    layoutSuperAdmin.visibility = if (isSuperAdmin) View.VISIBLE else View.GONE
                    refreshProfileViews()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun refreshProfileViews() {
        tvMyRoomName.text = "👑♡♡$userName♡♡👑\nTap to enter your Royal Room"
        tvProfileAvatar.text = userAvatar
        tvProfileName.text = "👑♡♡$userName♡♡👑"
        tvProfileUid.text = "ID:$shortUid 📋  •  Lv.${(currentXp / 50000) + 1}"
        tvProfileCoins.text = "🪙 $currentCoins  •  ⚡ $currentXp XP"
    }

    private fun showSimplePopup(title: String, msg: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(msg).setPositiveButton("GOT IT ✨", null).show()
    }

    private fun openRoom(roomId: String, isHost: Boolean) {
        startActivity(Intent(this, LiveRoomActivity::class.java).apply {
            putExtra("ROOM_ID", roomId)
            putExtra("IS_HOST", isHost)
        })
    }

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
    }
}
