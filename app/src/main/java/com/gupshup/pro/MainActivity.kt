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
            setContentView(TextView(this).apply { text = "Error: ${e.message}"; setTextColor(Color.WHITE); setBackgroundColor(Color.BLACK) })
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
            setPadding(36, 26, 36, 16)
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
            layoutParams = LinearLayout.LayoutParams(68, 10).apply { topMargin = 4 }
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
            background = makeGradientBg("#8B5CF6", "#6D28D9", 14f)
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
            setPadding(0, 16, 0, 18)
            elevation = 16f
        }

        val navLabels = listOf("🏠\nParty", "🎮\nGames", "💬\nChat (2)", "👤\nProfile")
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
        private fun buildMeHomeTab(): ScrollView {
        val scroll = ScrollView(this).apply { visibility = View.GONE }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 48)
        }

        val myRoomCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = makeRoundedBg("#FFFFFF", 18f)
            setPadding(24, 24, 28, 24)
            elevation = 4f
            setOnClickListener { openRoom(shortUid, true) }
        }
        val avatarBox = TextView(this).apply {
            text = userAvatar
            textSize = 32f
            gravity = Gravity.CENTER
            background = makeGradientBg("#2E1A47", "#5B2C6F", 14f)
            layoutParams = LinearLayout.LayoutParams(128, 128)
        }
        tvMyRoomName = TextView(this).apply {
            text = "👑♡♡$userName♡♡👑\nTap to enter your Royal Room"
            textSize = 15f
            setTextColor(Color.parseColor("#1A1D26"))
            setTypeface(null, Typeface.BOLD)
            setPadding(24, 0, 12, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val arrow = TextView(this).apply {
            text = "❯"
            textSize = 18f
            setTextColor(Color.parseColor("#8E96AA"))
        }
        myRoomCard.addView(avatarBox)
        myRoomCard.addView(tvMyRoomName)
        myRoomCard.addView(arrow)
        box.addView(myRoomCard)

        box.addView(TextView(this).apply {
            text = "Following   •   Recent Rooms"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1D26"))
            setPadding(8, 32, 8, 20)
        })

        meRoomsGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(meRoomsGrid)

        scroll.addView(box)
        return scroll
    }

    private fun buildPopularHomeTab(): ScrollView {
        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 16, 28, 48)
        }

        val rocketTicker = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = makeGradientBg("#1E3A8A", "#0284C7", 18f)
            setPadding(24, 18, 24, 18)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 20 }
            setOnClickListener { openRoom("551754", false) }
        }
        rocketTicker.addView(TextView(this).apply {
            text = "🚀 Room ID: 551754\nLaunching LV3 Purple Rocket (2X Rewards!)"
            setTextColor(Color.WHITE)
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        rocketTicker.addView(TextView(this).apply {
            text = "  Go ➔  "
            setTextColor(Color.parseColor("#1A0B2E"))
            background = makeRoundedBg("#FFD700", 20f)
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setPadding(16, 10, 16, 10)
        })
        box.addView(rocketTicker)

        val heroBanner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = makeGradientBg("#B91C1C", "#F59E0B", 20f)
            setPadding(32, 34, 32, 34)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 22 }
            setOnClickListener { showWealthPodiumDialog() }
        }
        heroBanner.addView(TextView(this).apply {
            text = "🐉 PK SUPPORT & 2X LUCKY CARNIVAL 🏆"
            setTextColor(Color.parseColor("#FFFBEB"))
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        })
        heroBanner.addView(TextView(this).apply {
            text = "Double Coins & Double EXP • Win up to 40,00,000 Coins!"
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        })
        box.addView(heroBanner)

        val trioRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 24 }
        }
        val cardConfigs = listOf(
            Triple("🏆\nRanking", "#FEF3C7", "#B45309"),
            Triple("🏰\nFamily", "#DBEAFE", "#1D4ED8"),
            Triple("💑\nCP Space", "#F3E8FF", "#7E22CE")
        )
        cardConfigs.forEachIndexed { idx, (label, bgHex, txtHex) ->
            val card = TextView(this).apply {
                text = label
                textSize = 14f
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor(txtHex))
                background = makeRoundedBg(bgHex, 16f)
                setPadding(12, 24, 12, 24)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (idx < 2) rightMargin = 14
                }
                setOnClickListener {
                    when (idx) {
                        0 -> showWealthPodiumDialog()
                        1 -> showSimplePopup("🏰 ROYAL FAMILY CLAN", "Join or create your Royal Family!\n• Family Badge on profile\n• Weekly 20,00,000 Coin Treasury (2X!)")
                        2 -> showSimplePopup("💑 ROYAL CP SPACE", "Send a Diamond Ring inside any Voice Room to unlock Golden Heart CP Frame!")
                    }
                }
            }
            trioRow.addView(card)
        }
        box.addView(trioRow)

        popularRoomsGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(popularRoomsGrid)

        scroll.addView(box)
        return scroll
    }
        private fun buildGamesHubTab(): ScrollView {
        val scroll = ScrollView(this).apply { visibility = View.GONE }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 48)
        }

        box.addView(TextView(this).apply {
            text = "🎮 2X REWARDS & JACKPOT GAMES"
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1D26"))
            setPadding(0, 0, 0, 24)
        })

        val dailyCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeGradientBg("#059669", "#10B981", 20f)
            setPadding(32, 32, 32, 32)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 24 }
        }
        dailyCard.addView(TextView(this).apply {
            text = "🎁 2X DAILY VIP CHECK-IN BONUS"
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        })
        dailyCard.addView(TextView(this).apply {
            text = "Claim 🪙 2,00,000 FREE Coins + ⚡ 1,00,000 EXP every day!"
            setTextColor(Color.parseColor("#ECFDF5"))
            textSize = 13f
            setPadding(0, 8, 0, 20)
        })
        dailyCard.addView(Button(this).apply {
            text = "✨ CLAIM +2,00,000 COINS & +1,00,000 XP"
            setTextColor(Color.parseColor("#064E3B"))
            background = makeRoundedBg("#FFD700", 14f)
            setTypeface(null, Typeface.BOLD)
            setOnClickListener { claimDaily200kCoins() }
        })
        box.addView(dailyCard)

        val gameCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeGradientBg("#4C1D95", "#7C3AED", 20f)
            setPadding(32, 32, 32, 32)
        }
        gameCard.addView(TextView(this).apply {
            text = "🎰 GREEDY & FRUIT JACKPOT WHEEL"
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        })
        gameCard.addView(TextView(this).apply {
            text = "Bet Gold Coins & win up to 45x Mega Jackpot in 30 seconds!"
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(0, 8, 0, 20)
        })
        gameCard.addView(Button(this).apply {
            text = "🔥 PLAY GREEDY & FRUIT GAME"
            setTextColor(Color.WHITE)
            background = makeRoundedBg("#F59E0B", 14f)
            setTypeface(null, Typeface.BOLD)
            setOnClickListener { startActivity(Intent(this@MainActivity, GreedyGameActivity::class.java)) }
        })
        box.addView(gameCard)

        scroll.addView(box)
        return scroll
    }

    private fun buildMessagesTab(): ScrollView {
        val scroll = ScrollView(this).apply { visibility = View.GONE }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 48)
        }
        box.addView(TextView(this).apply {
            text = "💬 OFFICIAL MESSAGES & ALERTS"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1D26"))
            setPadding(0, 0, 0, 24)
        })

        val msgs = listOf(
            Triple("👑 GupShup Official Team", "Welcome to GupShup Pro! Your VIP ID $shortUid has 🪙 $currentCoins & ⚡ $currentXp EXP (2X Edition).", "Just now"),
            Triple("🚀 Lv.3 Rocket Launch", "Room 551754 launched a Lv.3 Purple Rocket! Join now to win 40,00,000 Coins!", "2m ago")
        )
        msgs.forEach { (sender, body, time) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = makeRoundedBg("#FFFFFF", 16f)
                setPadding(28, 24, 28, 24)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 18 }
            }
            card.addView(TextView(this).apply {
                text = "$sender   •   $time"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1A1D26"))
            })
            card.addView(TextView(this).apply {
                text = body
                textSize = 13f
                setTextColor(Color.parseColor("#4B5563"))
                setPadding(0, 8, 0, 0)
            })
            box.addView(card)
        }
        scroll.addView(box)
        return scroll
    }
        private fun buildProfileMeTab(): ScrollView {
        val scroll = ScrollView(this).apply { visibility = View.GONE }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 48)
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 12, 8, 24)
            setOnClickListener { showEditProfileDialog() }
        }
        tvProfileAvatar = TextView(this).apply {
            text = userAvatar
            textSize = 42f
            gravity = Gravity.CENTER
            background = makeGradientBg("#23123D", "#5B2C6F", 45f)
            layoutParams = LinearLayout.LayoutParams(165, 165)
        }
        val infoCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 0, 12, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        tvProfileName = TextView(this).apply {
            text = "👑♡♡$userName♡♡👑"
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1D26"))
        }
        tvProfileUid = TextView(this).apply {
            text = "ID:$shortUid 📋  •  Lv.${(currentXp / 50000) + 1}"
            textSize = 13f
            setTextColor(Color.parseColor("#6B7280"))
            setPadding(0, 8, 0, 8)
            setOnClickListener {
                val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("UID", shortUid))
                Toast.makeText(this@MainActivity, "📋 Copied ID: $shortUid", Toast.LENGTH_SHORT).show()
            }
        }
        tvProfileCoins = TextView(this).apply {
            text = "🪙 $currentCoins  •  ⚡ $currentXp XP"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#B45309"))
        }
        infoCol.addView(tvProfileName)
        infoCol.addView(tvProfileUid)
        infoCol.addView(tvProfileCoins)

        val editArrow = TextView(this).apply {
            text = "✏️ ❯"
            textSize = 16f
            setTextColor(Color.parseColor("#8E96AA"))
        }
        headerRow.addView(tvProfileAvatar)
        headerRow.addView(infoCol)
        headerRow.addView(editArrow)
        box.addView(headerRow)

        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 28)
        }
        val statsData = listOf("215\nFriend", "3K\nFollowing", "240\nFollowers", "1.8K\nVisitors")
        statsData.forEach { st ->
            statsRow.addView(TextView(this).apply {
                text = st
                textSize = 14f
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1A1D26"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
        box.addView(statsRow)

        val dualRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 26 }
        }
        val cardSvip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = makeGradientBg("#291D0E", "#85581F", 18f)
            setPadding(28, 30, 28, 30)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = 14 }
            setOnClickListener { showSimplePopup("👑 ROYAL SVIP 7", "Your SVIP 7 Privileges:\n• Golden Entry Ride in all rooms\n• 2X Daily Coins & 2X EXP Boost\n• Exclusive VIP Nameplate & Frame") }
        }
        cardSvip.addView(TextView(this).apply {
            text = "SVIP  👑"
            setTextColor(Color.parseColor("#FDE68A"))
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        })

        val cardWallet = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = makeGradientBg("#0F294A", "#1D4ED8", 18f)
            setPadding(28, 30, 28, 30)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = 14 }
            setOnClickListener { showWalletRechargeDialog() }
        }
        cardWallet.addView(TextView(this).apply {
            text = "Wallet  👛"
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        })
        dualRow.addView(cardSvip)
        dualRow.addView(cardWallet)
        box.addView(dualRow)
                val menuCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeRoundedBg("#FFFFFF", 20f)
            setPadding(28, 12, 28, 12)
        }
        val menuItems = listOf(
            Triple("👥  Invite Friends", "Get +1,00,000 Coins ❯", "#FEF3C7"),
            Triple("👑  Aristocracy", "Royal Duke & King ❯", "#FFFFFF"),
            Triple("🛡️  Family", "Royal Clan ❯", "#FFFFFF"),
            Triple("🏠  CP Space", "Couple Ring ❯", "#FFFFFF"),
            Triple("🏪  Store (Rides & Frames)", "2X VIP Shop ❯", "#FFFFFF"),
            Triple("👕  Dress & Avatar Frames", "Equipped ❯", "#FFFFFF"),
            Triple("🎒  My Bag", "Gifts & Cards ❯", "#FFFFFF"),
            Triple("🏅  Medal & Achievements", "Lv.62 Pro ❯", "#FFFFFF")
        )
        menuItems.forEach { (leftTxt, rightTxt, badgeBg) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(8, 26, 8, 26)
                setOnClickListener {
                    if (leftTxt.contains("Invite")) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Join me on GupShup Pro Voice Party! Use my ID $shortUid & get 5,00,000 FREE Coins!")
                        }
                        startActivity(Intent.createChooser(shareIntent, "Invite Friends"))
                    } else {
                        showSimplePopup(leftTxt.trim(), "Feature unlocked with your SVIP 7 account!\nCurrent Balance: 🪙 $currentCoins\nCurrent EXP: ⚡ $currentXp")
                    }
                }
            }
            row.addView(TextView(this).apply {
                text = leftTxt
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1A1D26"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(this).apply {
                text = rightTxt
                textSize = 12f
                setTextColor(Color.parseColor("#B45309"))
                background = makeRoundedBg(badgeBg, 14f)
                setPadding(16, 8, 16, 8)
            })
            menuCard.addView(row)
        }
        box.addView(menuCard)

        layoutSuperAdmin = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeRoundedBg("#FFFBEB", 18f, "#F59E0B", 3)
            setPadding(32, 32, 32, 32)
            visibility = if (isSuperAdmin) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 28 }
        }
        layoutSuperAdmin.addView(TextView(this).apply {
            text = "🛡️ SUPER ADMIN COIN & XP CONTROL"
            setTextColor(Color.parseColor("#B45309"))
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
        })
        val etTargetUid = EditText(this).apply { hint = "Enter User ID (e.g. 100001)" }
        val etSendCoins = EditText(this).apply { hint = "Coins to Add/Deduct (e.g. 1000000)" }
        val btnSend = Button(this).apply {
            text = "⚡ SEND / UPDATE USER COINS"
            setTextColor(Color.WHITE)
            background = makeRoundedBg("#D97706", 12f)
            setTypeface(null, Typeface.BOLD)
            setOnClickListener {
                val targetUid = etTargetUid.text.toString().trim()
                val amount = etSendCoins.text.toString().trim().toIntOrNull()
                if (targetUid.isEmpty() || amount == null) {
                    Toast.makeText(this@MainActivity, "Enter valid ID & Coins!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val targetRef = dbRef.child("users").child(targetUid).child("coins")
                targetRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snap: DataSnapshot) {
                        val old = snap.getValue(Int::class.java) ?: 0
                        val updated = (old + amount).coerceAtLeast(0)
                        targetRef.setValue(updated)
                        Toast.makeText(this@MainActivity, "✅ ID $targetUid updated to 🪙 $updated!", Toast.LENGTH_SHORT).show()
                        etSendCoins.setText("")
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }
        layoutSuperAdmin.addView(etTargetUid)
        layoutSuperAdmin.addView(etSendCoins)
        layoutSuperAdmin.addView(btnSend)
        box.addView(layoutSuperAdmin)

        box.addView(Button(this).apply {
            text = "🔄 Switch Account / Logout"
            setTextColor(Color.WHITE)
            background = makeRoundedBg("#DC2626", 14f)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 120).apply { topMargin = 28 }
            setOnClickListener {
                prefs.edit().clear().apply()
                startActivity(Intent(this@MainActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
        })

        scroll.addView(box)
        return scroll
        }
            private fun showWalletRechargeDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#180A2E"))
            setPadding(36, 36, 36, 36)
        }
        box.addView(TextView(this).apply {
            text = "👛 OFFICIAL 2X COIN WALLET\nBalance: 🪙 $currentCoins  \vert{}  ID:$shortUid"
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 22)
        })

        val packs = listOf(
            Triple("🪙 10,00,000 Coins (2X Offer)", "₹99", 99),
            Triple("🪙 60,00,000 Coins + SVIP 3", "₹499", 499),
            Triple("🪙 1,50,00,000 Coins + SVIP 7", "₹999", 999)
        )
        packs.forEach { (title, price, amt) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = makeRoundedBg("#2E1A47", 14f, "#FFD700", 2)
                setPadding(22, 20, 22, 20)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 14 }
            }
            row.addView(TextView(this).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(Button(this).apply {
                text = "Buy $price"
                setTextColor(Color.parseColor("#180A2E"))
                background = makeRoundedBg("#FFD700", 12f)
                setTypeface(null, Typeface.BOLD)
                setOnClickListener {
                    try {
                        val uri = Uri.parse("upi://pay?pa=$ADMIN_UPI_ID&pn=GupShupPro&am=$amt&cu=INR&tn=ID_$shortUid")
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Send $price to UPI: $ADMIN_UPI_ID with ID$shortUid", Toast.LENGTH_LONG).show()
                    }
                }
            })
            box.addView(row)
        }
        AlertDialog.Builder(this).setView(box).setPositiveButton("CLOSE", null).show()
    }

    private fun switchMainSection(section: Int) {
        pageMeHome.visibility = if (section == 0) View.VISIBLE else View.GONE
        pagePopularHome.visibility = if (section == 1) View.VISIBLE else View.GONE
        pageGamesHub.visibility = if (section == 2) View.VISIBLE else View.GONE
        pageMessages.visibility = if (section == 3) View.VISIBLE else View.GONE
        pageProfileMe.visibility = if (section == 4) View.VISIBLE else View.GONE

        topBarHeader.visibility = if (section == 0 || section == 1) View.VISIBLE else View.GONE

        tvTopMe.setTextColor(if (section == 0) Color.parseColor("#1A1D26") else Color.parseColor("#8E96AA"))
        indTopMe.visibility = if (section == 0) View.VISIBLE else View.INVISIBLE
        tvTopPopular.setTextColor(if (section == 1) Color.parseColor("#1A1D26") else Color.parseColor("#8E96AA"))
        indTopPopular.visibility = if (section == 1) View.VISIBLE else View.INVISIBLE

        val activeBottom = when (section) {
            0, 1 -> 0
            2 -> 1
            3 -> 2
            else -> 3
        }
        bottomNavItems.forEachIndexed { idx, tv ->
            tv.setTextColor(if (idx == activeBottom) Color.parseColor("#00C49A") else Color.parseColor("#8E96AA"))
        }
    }

    private fun loadRoomsData() {
        dbRef.child("rooms").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                data class RoomModel(val id: String, val title: String, val host: String, val avatar: String, val gender: String, val count: Int, val tag: String, val c1: String, val c2: String)
                val rooms = mutableListOf<RoomModel>()
                for (snap in snapshot.children) {
                    val rId = snap.key ?: continue
                    val title = snap.child("title").getValue(String::class.java) ?: "Royal Voice Party"
                    val host = snap.child("host").getValue(String::class.java) ?: "VIP Host"
                    rooms.add(RoomModel(rId, title, host, "👑", "♂", Random.nextInt(6, 35), "New", "#3B1E54", "#6D28D9"))
                }
                rooms.add(RoomModel("100101", "i love moha...", "🔥 Dimple 💔", "👩‍🎤", "♀", 13, "SVIP
                                   5", "#831843", "#DB2777"))
                rooms.add(RoomModel("100102", "HAZA OFFICIAL RO...", "THALAPATI", "🦁", "♂", 16, "SVIP 3", "#064E3B", "#059669"))
                rooms.add(RoomModel("100103", "welcome dosto", "surya bisht uk", "😎", "♂", 24, "Lv.34", "#1E3A8A", "#2563EB"))
                rooms.add(RoomModel("100104", "Queen", "Aayat 👑", "👸", "♀", 19, "New", "#4C1D95", "#9333EA"))

                populate2ColumnGrid(popularRoomsGrid, rooms.map { Triple(it.id, "${it.gender} ${it.title}", "🇮🇳 ${it.host}") }, rooms.map { Triple(it.avatar, "📶 ${it.count} • ${it.tag}", Pair(it.c1, it.c2)) })
                populate2ColumnGrid(meRoomsGrid, rooms.take(4).map { Triple(it.id, "${it.gender} ${it.title}", "🇮🇳 ${it.host}") }, rooms.take(4).map { Triple(it.avatar, "📶 ${it.count}", Pair(it.c1, it.c2)) })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun populate2ColumnGrid(grid: LinearLayout, infoList: List<Triple<String, String, String>>, styleList: List<Triple<String, String, Pair<String, String>>>) {
        grid.removeAllViews()
        var i = 0
        while (i < infoList.size) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 22 }
            }
            row.addView(buildRoomCard(infoList[i], styleList[i], true))
            if (i + 1 < infoList.size) {
                row.addView(buildRoomCard(infoList[i + 1], styleList[i + 1], false))
            } else {
                row.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
            }
            grid.addView(row)
            i += 2
        }
    }

    private fun buildRoomCard(info: Triple<String, String, String>, style: Triple<String, String, Pair<String, String>>, isLeft: Boolean): LinearLayout {
        val (roomId, title, host) = info
        val (avatar, badge, colors) = style
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (isLeft) rightMargin = 12 else leftMargin = 12
            }
            setOnClickListener { openRoom(roomId, false) }

            val cover = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                background = makeGradientBg(colors.first, colors.second, 18f)
                setPadding(18, 16, 18, 16)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 360)
            }
            cover.addView(TextView(this@MainActivity).apply {
                text = badge
                setTextColor(Color.WHITE)
                background = makeRoundedBg("#66000000", 12f)
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setPadding(12, 4, 12, 4)
            })
            cover.addView(TextView(this@MainActivity).apply {
                text = avatar
                textSize = 52f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            })
            cover.addView(TextView(this@MainActivity).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 13f
                maxLines = 1
                setTypeface(null, Typeface.BOLD)
            })
            addView(cover)

            addView(TextView(this@MainActivity).apply {
                text = host
                setTextColor(Color.parseColor("#1A1D26"))
                textSize = 13f
                maxLines = 1
                setTypeface(null, Typeface.BOLD)
                setPadding(6, 10, 6, 0)
            })
        }
    }
        private fun showWealthPodiumDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#23120B"))
            setPadding(36, 36, 36, 36)
        }
        box.addView(TextView(this).apply {
            text = "🏆 WEALTH & CHARM 2X PODIUM 🏆\nLeft time: 01:19:33:58"
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        })
        val ranks = listOf(
            "🥇 #1  👑♡♡$userName♡♡👑  (Lv.62)  —  🪙 $currentCoins",
            "🥈 #2  🔥 Dimple 💔  (Lv.54)  —  🪙 2,62,800",
            "🥉 #3  🦁 THALAPATI  (Lv.49)  —  🪙 1,97,000"
        )
        ranks.forEach { r ->
            box.addView(TextView(this).apply {
                text = r
                setTextColor(Color.WHITE)
                background = makeRoundedBg("#3B2114", 14f, "#FFD700", 2)
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setPadding(24, 20, 24, 20)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 14 }
            })
        }
        AlertDialog.Builder(this).setView(box).setPositiveButton("CLOSE ✨", null).show()
    }

    private fun showSearchRoomDialog() {
        val et = EditText(this).apply { hint = "Enter 6-digit Room ID (e.g. 100101)" }
        AlertDialog.Builder(this)
            .setTitle("🔍 Join Voice Room by ID")
            .setView(et)
            .setPositiveButton("Join Room ➔") { _, _ ->
                val id = et.text.toString().trim()
                if (id.length >= 4) openRoom(id, false)
                else Toast.makeText(this, "Enter valid Room ID!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreateRoomDialog() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 24, 48, 12) }
        val etTitle = EditText(this).apply { hint = "Room Name"; setText("$userName's Royal Room") }
        layout.addView(etTitle)
        AlertDialog.Builder(this)
            .setTitle("🎙️ Create Live Voice Room")
            .setView(layout)
            .setPositiveButton("Go Live") { _, _ ->
                val newId = Random.nextInt(100000, 999999).toString()
                val title = etTitle.text.toString().trim().ifEmpty { "$userName's Party" }
                dbRef.child("rooms").child(newId).setValue(mapOf("title" to title, "host" to userName, "hostUid" to shortUid))
                openRoom(newId, true)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditProfileDialog() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 24, 48, 12) }
        val etName = EditText(this).apply { hint = "Display Name"; setText(userName) }
        val etBio = EditText(this).apply { hint = "Bio"; setText(userBio) }
        val etAvatar = EditText(this).apply { hint = "Avatar Emoji"; setText(userAvatar) }
        val rg = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        val rbM = RadioButton(this).apply { text = "♂️ Male"; id = View.generateViewId() }
        val rbF = RadioButton(this).apply { text = "♀️ Female"; id = View.generateViewId() }
        rg.addView(rbM); rg.addView(rbF)
        if (userGender == "Female") rbF.isChecked = true else rbM.isChecked = true
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
                dbRef.child("users").child(shortUid).updateChildren(mapOf("name" to userName, "bio" to userBio, "avatar" to userAvatar, "gender" to userGender))
                refreshProfileViews()
                Toast.makeText(this, "✅ Profile Saved!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun claimDaily200kCoins() {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        if (prefs.getString("last_daily_claim", "") == today) {
            Toast.makeText(this, "🎁 Already claimed today! Come back tomorrow ✨", Toast.LENGTH_LONG).show()
            return
        }
        currentCoins += 200000
        currentXp += 100000
        prefs.edit().putString("last_daily_claim", today).putInt("user_coins", currentCoins).putInt("user_xp", currentXp).apply()
        dbRef.child("users").child(shortUid).updateChildren(mapOf("coins" to currentCoins, "xp" to currentXp))
        refreshProfileViews()
        showSimplePopup("🎉 2X DAILY BONUS CLAIMED!", "Received 🪙 2,00,000 Coins + ⚡ 1,00,000 EXP!\n\nNew Balance: 🪙 $currentCoins")
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
                          
