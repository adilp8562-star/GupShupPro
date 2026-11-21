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

    private val navIcons = arrayListOf(TextView(this)).apply { clear() }
    private val navLabels = arrayListOf(TextView(this)).apply { clear() }
        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        navIcons.clear()
        navLabels.clear()

        prefs = getSharedPreferences("GupShupHazaPro", Context.MODE_PRIVATE)
        currentCoins = prefs.getInt("user_coins", 500000)
        currentXp = prefs.getInt("user_xp", 100000)
        userName = prefs.getString("user_name", "Its Meroyal") ?: "Its Meroyal"
        userBio = prefs.getString("user_bio", "Welcome to my Royal Voice Party! 👑✨") ?: "Welcome to my Royal Voice Party! 👑✨"
        userGender = prefs.getString("user_gender", "Male") ?: "Male"
        userAvatar = prefs.getString("user_avatar", "👑") ?: "👑"
        isSuperAdmin = prefs.getBoolean("is_super_admin", false)

        shortUid = prefs.getString("short_uid", "") ?: ""
        if (shortUid.isEmpty()) {
            shortUid = (100001 + Random.nextInt(899999)).toString()
            prefs.edit().putString("short_uid", shortUid).apply()
        }

        dbRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).reference
        syncFirebaseUser()

        val rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0B0B14"))
        }

        topBarHeader = createHazaTopHeader()
        rootContainer.addView(topBarHeader, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, dp(62)
        ))

        val contentFrame = FrameLayout(this)
        val contentLp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).apply {
            topMargin = dp(62)
            bottomMargin = dp(66)
        }

        pageMeHome = createMeTabPage()
        pagePopularHome = createPopularTabPage()
        pageGamesHub = createGamesTabPage()
        pageMessages = createMessagesTabPage()
        pageProfileMe = createProfileTabPage()

        contentFrame.addView(pageMeHome)
        contentFrame.addView(pagePopularHome)
        contentFrame.addView(pageGamesHub)
        contentFrame.addView(pageMessages)
        contentFrame.addView(pageProfileMe)

        rootContainer.addView(contentFrame, contentLp)

        val bottomNav = createHazaBottomNav()
        val navLp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, dp(66)
        ).apply {
            gravity = Gravity.BOTTOM
        }
        rootContainer.addView(bottomNav, navLp)

        setContentView(rootContainer)
        switchBottomTab(0)
        loadRoomsData()
        requestAudioPermission()
        }
            private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun makeGradient(colors: IntArray, radiusDp: Float, strokeColor: Int = 0, strokeWidthDp: Int = 0): GradientDrawable {
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
            cornerRadius = radiusDp * resources.displayMetrics.density
            if (strokeColor != 0 && strokeWidthDp > 0) {
                setStroke(dp(strokeWidthDp), strokeColor)
            }
        }
    }

    private fun createHazaTopHeader(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(14), dp(8))
            setBackgroundColor(Color.parseColor("#0B0B14"))

            val tabsRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val meBox = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, 0, dp(20), 0)
                setOnClickListener { switchTopHomeTab(0) }
            }
            tvTopMe = TextView(this@MainActivity).apply {
                text = "Me"
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
            }
            indTopMe = View(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(3)).apply { topMargin = dp(4) }
                background = makeGradient(intArrayOf(Color.parseColor("#FF2E93"), Color.parseColor("#7928CA")), 2f)
            }
            meBox.addView(tvTopMe)
            meBox.addView(indTopMe)

            val popBox = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setOnClickListener { switchTopHomeTab(1) }
            }
            tvTopPopular = TextView(this@MainActivity).apply {
                text = "Popular"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#8E8E9E"))
            }
            indTopPopular = View(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(26), dp(3)).apply { topMargin = dp(4) }
                background = makeGradient(intArrayOf(Color.parseColor("#FF2E93"), Color.parseColor("#7928CA")), 2f)
                visibility = View.INVISIBLE
            }
            popBox.addView(tvTopPopular)
            popBox.addView(indTopPopular)

            tabsRow.addView(meBox)
            tabsRow.addView(popBox)
            addView(tabsRow)

            val btnSearch = TextView(this@MainActivity).apply {
                text = "🔍"
                textSize = 17f
                gravity = Gravity.CENTER
                background = makeGradient(intArrayOf(Color.parseColor("#1A1A2E"), Color.parseColor("#1A1A2E")), 18f)
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { rightMargin = dp(10) }
                setOnClickListener { showSearchRoomDialog() }
            }
            addView(btnSearch)

            val btnCreateRoom = TextView(this@MainActivity).apply {
                text = "🎙️ + Room"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(6), dp(12), dp(6))
                background = makeGradient(intArrayOf(Color.parseColor("#FF2E93"), Color.parseColor("#7928CA")), 18f)
                setOnClickListener { showCreateRoomDialog() }
            }
            addView(btnCreateRoom)
        }
    }    private fun createHazaBottomNav(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#11111F"))
            setPadding(0, dp(6), 0, dp(6))

            val items = listOf(
                Pair("🏠", "Home"),
                Pair("🎮", "Games"),
                Pair("💬", "Messages"),
                Pair("👤", "Me")
            )

            for (i in items.indices) {
                val tab = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    setOnClickListener { switchBottomTab(i) }
                }
                val icon = TextView(this@MainActivity).apply {
                    text = items[i].first
                    textSize = 20f
                    gravity = Gravity.CENTER
                }
                val label = TextView(this@MainActivity).apply {
                    text = items[i].second
                    textSize = 11f
                    gravity = Gravity.CENTER
                    setTextColor(Color.parseColor("#8E8E9E"))
                }
                navIcons.add(icon)
                navLabels.add(label)
                tab.addView(icon)
                tab.addView(label)
                addView(tab)
            }
        }
    }

    private fun switchBottomTab(index: Int) {
        pageMeHome.visibility = View.GONE
        pagePopularHome.visibility = View.GONE
        pageGamesHub.visibility = View.GONE
        pageMessages.visibility = View.GONE
        pageProfileMe.visibility = View.GONE

        topBarHeader.visibility = if (index == 0) View.VISIBLE else View.GONE

        when (index) {
            0 -> switchTopHomeTab(0)
            1 -> pageGamesHub.visibility = View.VISIBLE
            2 -> pageMessages.visibility = View.VISIBLE
            3 -> pageProfileMe.visibility = View.VISIBLE
        }

        for (i in navIcons.indices) {
            if (i == index) {
                navLabels[i].setTextColor(Color.parseColor("#FF2E93"))
                navLabels[i].setTypeface(null, Typeface.BOLD)
            } else {
                navLabels[i].setTextColor(Color.parseColor("#8E8E9E"))
                navLabels[i].setTypeface(null, Typeface.NORMAL)
            }
        }
    }

    private fun switchTopHomeTab(tab: Int) {
        if (tab == 0) {
            pageMeHome.visibility = View.VISIBLE
            pagePopularHome.visibility = View.GONE
            tvTopMe.setTextColor(Color.WHITE)
            tvTopMe.textSize = 18f
            indTopMe.visibility = View.VISIBLE
            tvTopPopular.setTextColor(Color.parseColor("#8E8E9E"))
            tvTopPopular.textSize = 16f
            indTopPopular.visibility = View.INVISIBLE
        } else {
            pageMeHome.visibility = View.GONE
            pagePopularHome.visibility = View.VISIBLE
            tvTopPopular.setTextColor(Color.WHITE)
            tvTopPopular.textSize = 18f
            indTopPopular.visibility = View.VISIBLE
            tvTopMe.setTextColor(Color.parseColor("#8E8E9E"))
            tvTopMe.textSize = 16f
            indTopMe.visibility = View.INVISIBLE
        }
    }    private fun createMeTabPage(): ScrollView {
        return ScrollView(this).apply {
            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(8), dp(14), dp(24))
            }

            val myRoomCard = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                background = makeGradient(intArrayOf(Color.parseColor("#2A1647"), Color.parseColor("#18182C")), 16f, Color.parseColor("#FF2E93"), 1)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(16)
                }
                setOnClickListener {
                    openLiveRoom("room_$shortUid", "$userName's Royal Room", userName, "Royal")
                }
            }

            val myIcon = TextView(this@MainActivity).apply {
                text = "👑"
                textSize = 26f
                gravity = Gravity.CENTER
                background = makeGradient(intArrayOf(Color.parseColor("#FF2E93"), Color.parseColor("#7928CA")), 24f)
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply { rightMargin = dp(12) }
            }
            myRoomCard.addView(myIcon)

            val myInfo = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            tvMyRoomName = TextView(this@MainActivity).apply {
                text = "$userName's Voice Room"
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
            }
            val tvMySub = TextView(this@MainActivity).apply {
                text = "ID: $shortUid  •  Tap to go LIVE 🎙️"
                textSize = 12f
                setTextColor(Color.parseColor("#C7C7D9"))
            }
            myInfo.addView(tvMyRoomName)
            myInfo.addView(tvMySub)
            myRoomCard.addView(myInfo)

            val goLiveBadge = TextView(this@MainActivity).apply {
                text = "LIVE"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setPadding(dp(10), dp(4), dp(10), dp(4))
                background = makeGradient(intArrayOf(Color.parseColor("#FF2E93"), Color.parseColor("#FF6B6B")), 12f)
            }
            myRoomCard.addView(goLiveBadge)
            container.addView(myRoomCard)

            val titleJoined = TextView(this@MainActivity).apply {
                text = "🔥 Recently Joined & VIP Rooms"
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setPadding(0, 0, 0, dp(10))
            }
            container.addView(titleJoined)

            meRoomsGrid = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }
            container.addView(meRoomsGrid)

            addView(container)
        }
    }

    private fun createPopularTabPage(): ScrollView {
        return ScrollView(this).apply {
            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(6), dp(14), dp(24))
            }

            val banner = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
                background = makeGradient(intArrayOf(Color.parseColor("#FF2E93"), Color.parseColor("#7928CA"), Color.parseColor("#2A1647")), 16f)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(14)
                }
                setOnClickListener { switchBottomTab(1) }
            }
            banner.addView(TextView(this@MainActivity).apply {
                text = "🎡 LUCKY WHEEL & GREEDY CARNIVAL"
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
            })
            banner.addView(TextView(this@MainActivity).apply {
                text = "Win up to 50,000 Coins every 30s! Tap to Play ➔"
                textSize = 12f
                setTextColor(Color.parseColor("#F3E8FF"))
                setPadding(0, dp(4), 0, 0)
            })
            container.addView(banner)

            val catScroll = HorizontalScrollView(this@MainActivity).apply {
                isHorizontalScrollBarEnabled = false
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(12)
                }
            }
            val catRow = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
            val categories = listOf("All", "Party", "Music", "Royal", "Dating", "Games")
            for (cat in categories) {
                val chip = TextView(this@MainActivity).apply {
                    text = cat
                    textSize = 12f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(if (cat == selectedCategory) Color.WHITE else Color.parseColor("#A0A0B5"))
                    setPadding(dp(14), dp(6), dp(14), dp(6))
                    background = if (cat == selectedCategory) {
                        makeGradient(intArrayOf(Color.parseColor("#FF2E93"), Color.parseColor("#7928CA")), 16f)
                    } else {
                        makeGradient(intArrayOf(Color.parseColor("#1A1A2E"), Color.parseColor("#1A1A2E")), 16f)
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        rightMargin = dp(8)
                    }
                    setOnClickListener {
                        selectedCategory = cat
                        loadRoomsData()
                        Toast.makeText(this@MainActivity, "Filter: $cat", Toast.LENGTH_SHORT).show()
                    }
                }
                catRow.addView(chip)
            }
            catScroll.addView(catRow)
            container.addView(catScroll)

            popularRoomsGrid = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }
            container.addView(popularRoomsGrid)

            addView(container)
        }
    }
        private fun renderHazaTwoColumnGrid(targetLayout: LinearLayout, rooms: List<Any>) {
        targetLayout.removeAllViews()
        var i = 0
        while (i < rooms.size) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(10)
                }
            }
            val firstMap = rooms[i] as Map<*, *>
            val card1 = createHazaRoomCard(firstMap)
            card1.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(5)
            }
            row.addView(card1)

            if (i + 1 < rooms.size) {
                val secondMap = rooms[i + 1] as Map<*, *>
                val card2 = createHazaRoomCard(secondMap)
                card2.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = dp(5)
                }
                row.addView(card2)
            } else {
                val spacer = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(10), 1f).apply { leftMargin = dp(5) }
                }
                row.addView(spacer)
            }
            targetLayout.addView(row)
            i += 2
        }
    }

    private fun createHazaRoomCard(room: Map<*, *>): LinearLayout {
        val roomId = room["id"]?.toString() ?: "100101"
        val roomName = room["name"]?.toString() ?: "Royal Voice Party"
        val hostName = room["host"]?.toString() ?: "Host"
        val category = room["cat"]?.toString() ?: "Party"
        val onlineCount = room["count"]?.toString() ?: "48"

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = makeGradient(intArrayOf(Color.parseColor("#18182C"), Color.parseColor("#131324")), 14f, Color.parseColor("#2A2A45"), 1)
            setOnClickListener {
                openLiveRoom(roomId, roomName, hostName, category)
            }

            val topRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val badge = TextView(this@MainActivity).apply {
                text = "🎙️ $category"
                textSize = 10f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setPadding(dp(8), dp(2), dp(8), dp(2))
                background = makeGradient(intArrayOf(Color.parseColor("#FF2E93"), Color.parseColor("#7928CA")), 10f)
            }
            val users = TextView(this@MainActivity).apply {
                text = "🔥 $onlineCount"
                textSize = 11f
                setTextColor(Color.parseColor("#FFD700"))
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            topRow.addView(badge)
            topRow.addView(users)
            addView(topRow)

            val title = TextView(this@MainActivity).apply {
                text = roomName
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                maxLines = 2
                setPadding(0, dp(10), 0, dp(6))
            }
            addView(title)

                        val host = TextView(this@MainActivity).apply {
                text = "👑 $hostName  •  ID:$roomId"
                textSize = 11f
                setTextColor(Color.parseColor("#A0A0B5"))
            }
            addView(host)
        }
    }    private fun createGamesTabPage(): ScrollView {
        return ScrollView(this).apply {
            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(24))
            }

            container.addView(TextView(this@MainActivity).apply {
                text = "🎮 Royal Gold Carnival & Mini-Games"
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setPadding(0, 0, 0, dp(14))
            })

            val games = listOf(
                Triple("🎡 Greedy 30s Gold Wheel", "Bet on Fruits & Meat — Win up to 45x Coins!", "#FF2E93"),
                Triple("🧧 Lucky Bag & Room Rockets", "Join live rooms to catch free coin drops!", "#7928CA"),
                Triple("🎲 Royal Ludo & Teen Patti Club", "Play with voice friends & climb VIP ranks!", "#00B4D8")
            )

            for (g in games) {
                val card = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(16), dp(16), dp(16), dp(16))
                    background = makeGradient(intArrayOf(Color.parseColor("#18182C"), Color.parseColor(g.third)), 16f)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = dp(12)
                    }
                    setOnClickListener {
                        openLiveRoom("100104", "🧧 Lucky Bag & Ring Toss Party", "Its Meroyal", "Games")
                    }
                }
                card.addView(TextView(this@MainActivity).apply {
                    text = g.first
                    textSize = 16f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                })
                card.addView(TextView(this@MainActivity).apply {
                    text = g.second
                    textSize = 12f
                    setTextColor(Color.parseColor("#E0E0F0"))
                    setPadding(0, dp(4), 0, 0)
                })
                container.addView(card)
            }
            addView(container)
        }
    }

    private fun createMessagesTabPage(): ScrollView {
        return ScrollView(this).apply {
            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(24))
            }
            container.addView(TextView(this@MainActivity).apply {
                text = "💬 Messages & System Inbox"
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setPadding(0, 0, 0, dp(14))
            })

            val msgs = listOf(
                Pair("👑 GupShupPro Official", "Welcome $userName! Your 500,000 VIP Coins are ready."),
                Pair("💖 Ayisha Queen", "Hey! Join my Royal Shayari room tonight 🎶"),
                Pair("🎁 Daily Login Bonus", "Claim your daily VIP XP & Lucky Bag rewards inside any room!")
            )
            for (m in msgs) {
                val row = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    background = makeGradient(intArrayOf(Color.parseColor("#18182C"), Color.parseColor("#131324")), 14f)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = dp(10)
                    }
                }
                row.addView(TextView(this@MainActivity).apply {
                    text = m.first
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                })
                row.addView(TextView(this@MainActivity).apply {
                    text = m.second
                    textSize = 12f
                    setTextColor(Color.parseColor("#B0B0C5"))
                    setPadding(0, dp(4), 0, 0)
                })
                container.addView(row)
            }
            addView(container)
        }
    }    private fun createProfileTabPage(): ScrollView {
        return ScrollView(this).apply {
            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(24))
            }

            val profileCard = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(16), dp(20), dp(16), dp(20))
                background = makeGradient(intArrayOf(Color.parseColor("#2A1647"), Color.parseColor("#18182C")), 18f, Color.parseColor("#FF2E93"), 1)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(14)
                }
            }

            tvProfileAvatar = TextView(this@MainActivity).apply {
                text = userAvatar
                textSize = 40f
                gravity = Gravity.CENTER
                background = makeGradient(intArrayOf(Color.parseColor("#FF2E93"), Color.parseColor("#7928CA")), 36f)
                layoutParams = LinearLayout.LayoutParams(dp(72), dp(72)).apply { bottomMargin = dp(10) }
            }
            profileCard.addView(tvProfileAvatar)

            tvProfileName = TextView(this@MainActivity).apply {
                text = userName
                textSize = 20f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
            }
            profileCard.addView(tvProfileName)

            tvProfileUid = TextView(this@MainActivity).apply {
                text = "UID: $shortUid  📋"
                textSize = 13f
                setTextColor(Color.parseColor("#FFD700"))
                setPadding(0, dp(4), 0, dp(8))
                setOnClickListener { copyUidToClipboard() }
            }
            profileCard.addView(tvProfileUid)

            tvProfileCoins = TextView(this@MainActivity).apply {
                text = "🪙 $currentCoins Coins  -  ⚡ $currentXp XP"
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setPadding(dp(14), dp(6), dp(14), dp(6))
                background = makeGradient(intArrayOf(Color.parseColor("#121222"), Color.parseColor("#121222")), 14f)
            }
            profileCard.addView(tvProfileCoins)
            container.addView(profileCard)

            val menuItems = listOf(
                Pair("✏️ Edit Royal Profile & Bio", 1),
                Pair("💎 VIP Recharge & Gold Store", 2),
                Pair("🎙️ Create / Open My Voice Room", 3),
                Pair("🛡️ Safety Center & Super Admin", 4)
            )
            for (item in menuItems) {
                val btn = TextView(this@MainActivity).apply {
                    text = item.first
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                    background = makeGradient(intArrayOf(Color.parseColor("#18182C"), Color.parseColor("#131324")), 14f)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = dp(10)
                    }
                    setOnClickListener {
                        when (item.second) {
                            1 -> showEditProfileDialog()
                            2 -> showRechargeDialog()
                            3 -> showCreateRoomDialog()
                            4 -> showAdminUnlockDialog()
                        }
                    }
                }
                container.addView(btn)
            }

            layoutSuperAdmin = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                visibility = if (isSuperAdmin) View.VISIBLE else View.GONE
                setPadding(dp(14), dp(14), dp(14), dp(14))
                background = makeGradient(intArrayOf(Color.parseColor("#3B0D2E"), Color.parseColor("#1A1A2E")), 14f)
                addView(TextView(this@MainActivity).apply {
                    text = "👑 SUPER ADMIN CONTROL PANEL ACTIVE"
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#FFD700"))
                })
            }
            container.addView(layoutSuperAdmin)

            addView(container)
        }
    }    private fun showEditProfileDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(8))
        }
        val etName = EditText(this).apply { setText(userName); hint = "Enter Nickname" }
        val etBio = EditText(this).apply { setText(userBio); hint = "Enter Royal Bio" }
        val rgGender = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        val rbMale = RadioButton(this).apply { text = "Male 👑"; isChecked = userGender == "Male" }
        val rbFemale = RadioButton(this).apply { text = "Female 💖"; isChecked = userGender == "Female" }
        rgGender.addView(rbMale)
        rgGender.addView(rbFemale)
        box.addView(etName)
        box.addView(etBio)
        box.addView(rgGender)

        AlertDialog.Builder(this)
            .setTitle("✏️ Edit Royal Profile")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val newName = etName.text.toString().trim()
                if (newName.isNotEmpty()) userName = newName
                userBio = etBio.text.toString().trim().ifEmpty { userBio }
                userGender = if (rbFemale.isChecked) "Female" else "Male"
                userAvatar = if (userGender == "Female") "👸" else "👑"
                prefs.edit()
                    .putString("user_name", userName)
                    .putString("user_bio", userBio)
                    .putString("user_gender", userGender)
                    .putString("user_avatar", userAvatar)
                    .apply()
                tvProfileName.text = userName
                tvProfileAvatar.text = userAvatar
                tvMyRoomName.text = "$userName's Voice Room"
                syncFirebaseUser()
                Toast.makeText(this, "Profile Updated! ✨", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRechargeDialog() {
        val packs = arrayOf(
            "🪙 10,000 Coins — ₹99 (Instant VIP)",
            "🪙 55,000 Coins — ₹499 (+5,000 Bonus)",
            "👑 1,20,000 Coins — ₹999 (Royal SVIP)"
        )
        AlertDialog.Builder(this)
            .setTitle("💎 Royal Gold & VIP Store")
            .setItems(packs) { _, which ->
                val addCoins = when (which) {
                    0 -> 10000
                    1 -> 55000
                    else -> 120000
                }
                val amount = when (which) {
                    0 -> "99"
                    1 -> "499"
                    else -> "999"
                }
                try {
                    val uri = Uri.parse("upi://pay?pa=$ADMIN_UPI_ID&pn=GupShupPro&am=$amount&cu=INR")
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (e: Exception) {
                    Toast.makeText(this, "UPI App not found — Demo VIP Coins credited!", Toast.LENGTH_SHORT).show()
                }
                currentCoins += addCoins
                currentXp += addCoins / 10
                saveLocalStats()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showCreateRoomDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(8))
        }
        val etRoom = EditText(this).apply { hint = "Room Title (e.g. Royal Shayari Night)" }
        val etCat = EditText(this).apply { setText("Party"); hint = "Category: Party / Music / Royal / Games" }
        box.addView(etRoom)
        box.addView(etCat)

        AlertDialog.Builder(this)
            .setTitle("🎙️ Start Live Voice Party Room")
            .setView(box)
            .setPositiveButton("Go Live") { _, _ ->
                val title = etRoom.text.toString().trim().ifEmpty { "$userName's Royal Room" }
                val cat = etCat.text.toString().trim().ifEmpty { "Party" }
                val newId = (200000 + Random.nextInt(799999)).toString()
                val roomData = mapOf(
                    "id" to newId,
                    "name" to title,
                    "host" to userName,
                    "cat" to cat,
                    "count" to "1"
                )
                dbRef.child("rooms").child(newId).setValue(roomData)
                openLiveRoom(newId, title, userName, cat)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSearchRoomDialog() {
        val et = EditText(this).apply { hint = "Enter Room ID or Host Name" }
        AlertDialog.Builder(this)
            .setTitle("🔍 Search Voice Room")
            .setView(et)
            .setPositiveButton("Join") { _, _ ->
                val q = et.text.toString().trim()
                if (q.isNotEmpty()) {
                    openLiveRoom(q, "VIP Room #$q", "Royal Host", "Party")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAdminUnlockDialog() {
        val et = EditText(this).apply { hint = "Enter Super Admin Secret Code" }
        AlertDialog.Builder(this)
            .setTitle("🛡️ Safety Center & Admin")
            .setMessage("24x7 AI Voice Moderation Active. Admins can enter passkey below:")
            .setView(et)
            .setPositiveButton("Verify") { _, _ ->
                if (et.text.toString().trim() == "8562") {
                    isSuperAdmin = true
                    prefs.edit().putBoolean("is_super_admin", true).apply()
                    layoutSuperAdmin.visibility = View.VISIBLE
                    currentCoins += 1000000
                    saveLocalStats()
                    Toast.makeText(this, "👑 Super Admin Unlocked + 1M Coins!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Safety Report ticket submitted to Moderation Team 🛡️", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }    }

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
        tvProfileCoins.text = "🪙 $currentCoins Coins  -  ⚡ $currentXp XP"
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
                val liveList = arrayListOf(mapOf("id" to "", "name" to "", "host" to "", "cat" to "", "count" to "")).apply { clear() }
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

    
    
    
    
        }
    }
    
    
    
