package com.gupshap.pro

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private val dbUrl = "https://gupshuppro-9ab9e-default-rtdb.firebaseio.com"
    private lateinit var dbRef: DatabaseReference

    private var userId = "777777"
    private var userName = "Its Meroyal"
    private var userCoins = 500000
    private var userGems = 5000
    private var vipLevel = 100
    private val officialRoomId = "100001"

    private lateinit var tvCoins: TextView
    private lateinit var tvGlobalPati: TextView
    private var patiAnimator: ObjectAnimator? = null

    private lateinit var myPageDrawer: LinearLayout
    private lateinit var drawerBackdrop: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("GupShupPrefs", Context.MODE_PRIVATE)
        userId = prefs.getString("short_uid", "777777") ?: "777777"
        userName = prefs.getString("user_name", "Its Meroyal") ?: "Its Meroyal"
        userCoins = prefs.getInt("user_coins", 500000)
        vipLevel = prefs.getInt("vip_level", 100)

        dbRef = FirebaseDatabase.getInstance(dbUrl).reference

        buildUI()
        listenGlobalWins()
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("GupShupPrefs", Context.MODE_PRIVATE)
        userCoins = prefs.getInt("user_coins", userCoins)
        userName = prefs.getString("user_name", userName) ?: userName
        vipLevel = prefs.getInt("vip_level", vipLevel)
        if (::tvCoins.isInitialized) {
            tvCoins.text = "💰 $userCoins"
        }
    }

    private fun buildUI() {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#F7F1E5"))
        }

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
        }

        val mainCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 130)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 6, 4, 18)
        }

        val tvMe = TextView(this).apply {
            text = "Me"
            textSize = 18f
            setTextColor(Color.parseColor("#7A6A53"))
            setPadding(0, 0, 20, 0)
            setOnClickListener { toggleMyPageDrawer(true) }
        }

        val tvPopular = TextView(this).apply {
            text = "Popular"
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#2A2118"))
            setPadding(0, 0, 16, 0)
        }

        val tvBrand = TextView(this).apply {
            text = "Royal GupShup"
            textSize = 19f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            setTextColor(Color.parseColor("#B8860B"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        tvCoins = TextView(this).apply {
            text = "💰 $userCoins"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#4A3515"))
            setPadding(16, 8, 16, 8)
            background = box("#FFF8E3", "#D4AF37", 3, 40f)
            setOnClickListener { toggleMyPageDrawer(true) }
        }

        topBar.addView(tvMe)
        topBar.addView(tvPopular)
        topBar.addView(tvBrand)
        topBar.addView(tvCoins)
        mainCol.addView(topBar)

        val weeklyBanner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 34, 24, 34)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.parseColor("#1D1712"),
                    Color.parseColor("#3B2C18"),
                    Color.parseColor("#1D1712")
                )
            ).apply {
                cornerRadius = 26f
                setStroke(4, Color.parseColor("#E5C158"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }

        weeklyBanner.addView(TextView(this).apply {
            text = "✦ ROYAL CROWN EVENT ✦"
            textSize = 11f
            setTextColor(Color.parseColor("#F5D77F"))
            gravity = Gravity.CENTER
        })

        weeklyBanner.addView(TextView(this).apply {
            text = "Weekly Star"
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFD700"))
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 4)
        })

        weeklyBanner.addView(TextView(this).apply {
            text = "Gold & Ruby Hall of Fame"
            textSize = 12f
            setTextColor(Color.parseColor("#EAD699"))
            gravity = Gravity.CENTER
        })

        mainCol.addView(weeklyBanner)

        val catRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }

        val catItems = listOf(
            Triple("Ranking", "💎", "#FFF5E4"),
            Triple("Family", "👑", "#F9F1E3"),
            Triple("CP", "❤️", "#FFF2F0")
        )

        catItems.forEachIndexed { i, (label, icon, bgColor) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(12, 16, 12, 16)
                background = box(bgColor, "#D4AF37", 3, 20f)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    if (i < catItems.lastIndex) rightMargin = 12
                }
            }
            card.addView(TextView(this).apply {
                text = label
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#4A3515"))
            })
            card.addView(TextView(this).apply {
                text = "$icon  ✦"
                textSize = 17f
                setTextColor(Color.parseColor("#9B1B30"))
                setPadding(0, 6, 0, 0)
            })
            catRow.addView(card)
        }
        mainCol.addView(catRow)

        val roomCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 18, 20, 18)
            background = box("#FFFDF9", "#C59B27", 3, 22f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 14
            }
        }

        val roomInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        roomInfo.addView(TextView(this).apply {
            text = "🎙 Official VIP Room"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#3A280E"))
        })
        roomInfo.addView(TextView(this).apply {
            text = "Room ID: $officialRoomId"
            textSize = 13f
            setTextColor(Color.parseColor("#8C6D3F"))
        })

        val btnEnterRoom = Button(this).apply {
            text = "Enter Room"
            setTextColor(Color.parseColor("#2B1B04"))
            background = box("#F5D061", "#B8860B", 2, 30f)
            setOnClickListener {
                try {
                    val intent = Intent(this@MainActivity, Class.forName("com.gupshap.pro.LiveRoomActivity"))
                    intent.putExtra("ROOM_ID", officialRoomId)
                    startActivity(intent)
                } catch (_: Exception) {
                }
            }
        }

        roomCard.addView(roomInfo)
        roomCard.addView(btnEnterRoom)
        mainCol.addView(roomCard)

        val gameCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 18, 20, 18)
            background = box("#FFFDF9", "#9B1B30", 3, 22f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }

        val gameInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        gameInfo.addView(TextView(this).apply {
            text = "🎰 Girdy 30s Royal Arena"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#5C101D"))
        })
        gameInfo.addView(TextView(this).apply {
            text = "30-Sec Fast Gold Coins Round"
            textSize = 13f
            setTextColor(Color.parseColor("#8C6D3F"))
        })

        val btnPlayGame = Button(this).apply {
            text = "Play 30s"
            setTextColor(Color.WHITE)
            background = box("#9B1B30", "#D4AF37", 2, 30f)
            setOnClickListener {
                try {
                    val intent = Intent(this@MainActivity, Class.forName("com.gupshap.pro.GreedyGameActivity"))
                    startActivity(intent)
                } catch (_: Exception) {
                }
            }
        }

        gameCard.addView(gameInfo)
        gameCard.addView(btnPlayGame)
        mainCol.addView(gameCard)

        val featuredGrid = GridLayout(this).apply {
            columnCount = 2
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        val roomsList = listOf(
            Triple("✨ Royal Darbar", "Lv.18", "#FFFDF8"),
            Triple("👑 Maharaja Lounge", "Lv.15", "#FFFDF8"),
            Triple("💖 Ruby Heart Room", "Lv.12", "#FFF8F7"),
            Triple("✦ Golden Palace", "New", "#FFFDF8")
        )

        roomsList.forEachIndexed { idx, (rTitle, badge, rBg) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(14, 16, 14, 16)
                background = box(rBg, "#D4AF37", 3, 20f)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(idx % 2, 1f)
                    setMargins(
                        if (idx % 2 == 0) 0 else 8,
                        0,
                        if (idx % 2 == 0) 8 else 0,
                        14
                    )
                }
            }
            card.addView(TextView(this).apply {
                text = "❖ $badge ❖"
                textSize = 11f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#9B1B30"))
                setPadding(12, 4, 12, 4)
                background = box("#FFF3D1", "#C59B27", 2, 30f)
            })
            card.addView(TextView(this).apply {
                text = "♛"
                textSize = 32f
                setTextColor(Color.parseColor("#B8860B"))
                setPadding(0, 10, 0, 8)
            })
            card.addView(TextView(this).apply {
                text = rTitle
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#3A280E"))
                gravity = Gravity.CENTER
            })
            featuredGrid.addView(card)
        }
        mainCol.addView(featuredGrid)

        tvGlobalPati = TextView(this).apply {
            text = "👑 Royal Mega Win Feed • Live on GupShup Pro"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#4A3515"))
            setPadding(18, 12, 18, 12)
            background = box("#FFF6DC", "#C59B27", 3, 30f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        mainCol.addView(tvGlobalPati)

        scrollView.addView(mainCol)
        root.addView(scrollView)

        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(10, 14, 10, 14)
            background = box("#FFFDF9", "#D4AF37", 2, 0f)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        }

        val navItems = listOf("🏠 Home", "🧩 Rooms", "💬 Chat", "👤 Me")
        navItems.forEachIndexed { idx, title ->
            bottomBar.addView(TextView(this).apply {
                text = title
                textSize = 13f
                gravity = Gravity.CENTER
                setTypeface(typeface, if (idx == 0) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (idx == 0) Color.parseColor("#0F6B4A") else Color.parseColor("#5C4D3C"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    if (idx == 3) toggleMyPageDrawer(true)
                }
            })
        }
        root.addView(bottomBar)

        drawerBackdrop = View(this).apply {
            setBackgroundColor(Color.parseColor("#80000000"))
            visibility = View.GONE
            setOnClickListener { toggleMyPageDrawer(false) }
        }
        root.addView(drawerBackdrop)

        myPageDrawer = buildMyPageDrawer()
        root.addView(myPageDrawer)

        setContentView(root)
    }

    private fun buildMyPageDrawer(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 36, 24, 28)
            visibility = View.GONE
            background = box("#FAF3E3", "#C59B27", 4, 26f)
            layoutParams = FrameLayout.LayoutParams(
                (resources.displayMetrics.widthPixels * 0.82).toInt(),
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.END
            )

            addView(TextView(this@MainActivity).apply {
                text = "👑"
                textSize = 42f
                gravity = Gravity.CENTER
                setPadding(24, 16, 24, 16)
                background = box("#FFF8E3", "#D4AF37", 3, 80f)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    bottomMargin = 16
                }
            })

            addView(TextView(this@MainActivity).apply {
                text = "Name: $userName"
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#2E1F0A"))
                setPadding(0, 3, 0, 3)
            })

            addView(TextView(this@MainActivity).apply {
                text = "ID: $userId"
                textSize = 14f
                setTextColor(Color.parseColor("#5C492E"))
                setPadding(0, 2, 0, 2)
            })

            addView(TextView(this@MainActivity).apply {
                text = "VIP Level: 👑 VIP $vipLevel"
                textSize = 14f
                setTextColor(Color.parseColor("#9B1B30"))
                setPadding(0, 2, 0, 2)
            })

            addView(TextView(this@MainActivity).apply {
                text = "Coins: 💰 $userCoins"
                textSize = 14f
                setTextColor(Color.parseColor("#8A6207"))
                setPadding(0, 2, 0, 2)
            })

            addView(TextView(this@MainActivity).apply {
                text = "Gems: 💎 $userGems"
                textSize = 14f
                setTextColor(Color.parseColor("#1D4ED8"))
                setPadding(0, 2, 0, 10)
            })

            addView(TextView(this@MainActivity).apply {
                text = "\"Welcome to my royal corner of GupShup!\""
                textSize = 13f
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                setTextColor(Color.parseColor("#5A462B"))
                setPadding(14, 12, 14, 12)
                background = box("#FFFDF8", "#D4AF37", 2, 16f)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 18
                }
            })

            val drawerButtons = listOf(
                "🪞 Edit Profile",
                "👛 My Wallet",
                "⚙️ Settings",
                "🤝 Friends"
            )

            drawerButtons.forEach { label ->
                addView(Button(this@MainActivity).apply {
                    text = label
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    setTextColor(Color.parseColor("#3A280E"))
                    background = box("#FFF9EA", "#C59B27", 2, 16f)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 10
                    }
                    setOnClickListener { toggleMyPageDrawer(false) }
                })
            }
        }
    }

    private fun toggleMyPageDrawer(open: Boolean) {
        drawerBackdrop.visibility = if (open) View.VISIBLE else View.GONE
        myPageDrawer.visibility = if (open) View.VISIBLE else View.GONE
    }

    private fun listenGlobalWins() {
        dbRef.child("global_wins").limitToLast(1).addChildEventListener(object : ChildEventListener 
