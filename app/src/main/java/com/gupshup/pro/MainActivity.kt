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
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.parseColor("#F7F1E5"))

        val scrollView = ScrollView(this)
        scrollView.isFillViewport = true

        val mainCol = LinearLayout(this)
        mainCol.orientation = LinearLayout.VERTICAL
        mainCol.setPadding(24, 24, 24, 130)

        // Top Bar
        val topBar = LinearLayout(this)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.setPadding(4, 6, 4, 18)

        val tvMe = TextView(this)
        tvMe.text = "Me"
        tvMe.textSize = 18f
        tvMe.setTextColor(Color.parseColor("#7A6A53"))
        tvMe.setPadding(0, 0, 20, 0)
        tvMe.setOnClickListener { toggleMyPageDrawer(true) }

        val tvPopular = TextView(this)
        tvPopular.text = "Popular"
        tvPopular.textSize = 22f
        tvPopular.setTypeface(tvPopular.typeface, Typeface.BOLD)
        tvPopular.setTextColor(Color.parseColor("#2A2118"))
        tvPopular.setPadding(0, 0, 16, 0)

        val tvBrand = TextView(this)
        tvBrand.text = "Royal GupShup"
        tvBrand.textSize = 19f
        tvBrand.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        tvBrand.setTextColor(Color.parseColor("#B8860B"))
        tvBrand.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        tvCoins = TextView(this)
        tvCoins.text = "💰 $userCoins"
        tvCoins.textSize = 13f
        tvCoins.setTypeface(tvCoins.typeface, Typeface.BOLD)
        tvCoins.setTextColor(Color.parseColor("#4A3515"))
        tvCoins.setPadding(16, 8, 16, 8)
        tvCoins.background = box("#FFF8E3", "#D4AF37", 3, 40f)
        tvCoins.setOnClickListener { toggleMyPageDrawer(true) }

        topBar.addView(tvMe)
        topBar.addView(tvPopular)
        topBar.addView(tvBrand)
        topBar.addView(tvCoins)
        mainCol.addView(topBar)

        // Weekly Star Banner
        val weeklyBanner = LinearLayout(this)
        weeklyBanner.orientation = LinearLayout.VERTICAL
        weeklyBanner.gravity = Gravity.CENTER
        weeklyBanner.setPadding(24, 34, 24, 34)
        weeklyBanner.background = GradientDrawable(
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
        val bannerLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        bannerLp.bottomMargin = 20
        weeklyBanner.layoutParams = bannerLp

        val tvSub = TextView(this)
        tvSub.text = "✦ ROYAL CROWN EVENT ✦"
        tvSub.textSize = 11f
        tvSub.setTextColor(Color.parseColor("#F5D77F"))
        tvSub.gravity = Gravity.CENTER

        val tvStar = TextView(this)
        tvStar.text = "Weekly Star"
        tvStar.textSize = 28f
        tvStar.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        tvStar.setTextColor(Color.parseColor("#FFD700"))
        tvStar.gravity = Gravity.CENTER
        tvStar.setPadding(0, 6, 0, 4)

        val tvHall = TextView(this)
        tvHall.text = "Gold & Ruby Hall of Fame"
        tvHall.textSize = 12f
        tvHall.setTextColor(Color.parseColor("#EAD699"))
        tvHall.gravity = Gravity.CENTER

        weeklyBanner.addView(tvSub)
        weeklyBanner.addView(tvStar)
        weeklyBanner.addView(tvHall)
        mainCol.addView(weeklyBanner)

        // Ranking / Family / CP
        val catRow = LinearLayout(this)
        catRow.orientation = LinearLayout.HORIZONTAL
        catRow.weightSum = 3f
        val catLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        catLp.bottomMargin = 20
        catRow.layoutParams = catLp

        val cats = listOf(
            Triple("Ranking", "💎", "#FFF5E4"),
            Triple("Family", "👑", "#F9F1E3"),
            Triple("CP", "❤️", "#FFF2F0")
        )

        cats.forEachIndexed { idx, item ->
            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.gravity = Gravity.CENTER
            card.setPadding(12, 16, 12, 16)
            card.background = box(item.third, "#D4AF37", 3, 20f)
            val cardLp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            if (idx < cats.lastIndex) cardLp.rightMargin = 12
            card.layoutParams = cardLp

            val t1 = TextView(this)
            t1.text = item.first
            t1.textSize = 14f
            t1.setTypeface(t1.typeface, Typeface.BOLD)
            t1.setTextColor(Color.parseColor("#4A3515"))

            val t2 = TextView(this)
            t2.text = "${item.second}  ✦"
            t2.textSize = 17f
            t2.setTextColor(Color.parseColor("#9B1B30"))
            t2.setPadding(0, 6, 0, 0)

            card.addView(t1)
            card.addView(t2)
            catRow.addView(card)
        }
        mainCol.addView(catRow)

        // Official Room 100001
        val roomCard = LinearLayout(this)
        roomCard.orientation = LinearLayout.HORIZONTAL
        roomCard.gravity = Gravity.CENTER_VERTICAL
        roomCard.setPadding(20, 18, 20, 18)
        roomCard.background = box("#FFFDF9", "#C59B27", 3, 22f)
        val roomLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        roomLp.bottomMargin = 14
        roomCard.layoutParams = roomLp

        val roomTextCol = LinearLayout(this)
        roomTextCol.orientation = LinearLayout.VERTICAL
        roomTextCol.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val rTitle = TextView(this)
        rTitle.text = "🎙 Official VIP Room"
        rTitle.textSize = 16f
        rTitle.setTypeface(rTitle.typeface, Typeface.BOLD)
        rTitle.setTextColor(Color.parseColor("#3A280E"))

        val rSub = TextView(this)
        rSub.text = "Room ID: $officialRoomId"
        rSub.textSize = 13f
        rSub.setTextColor(Color.parseColor("#8C6D3F"))

        roomTextCol.addView(rTitle)
        roomTextCol.addView(rSub)

        val btnEnterRoom = Button(this)
        btnEnterRoom.text = "Enter Room"
        btnEnterRoom.setTextColor(Color.parseColor("#2B1B04"))
        btnEnterRoom.background = box("#F5D061", "#B8860B", 2, 30f)
        btnEnterRoom.setOnClickListener {
            try {
                val intent = Intent(this, Class.forName("com.gupshap.pro.LiveRoomActivity"))
                intent.putExtra("ROOM_ID", officialRoomId)
                startActivity(intent)
            } catch (_: Exception) {
            }
        }

        roomCard.addView(roomTextCol)
        roomCard.addView(btnEnterRoom)
        mainCol.addView(roomCard)

        // Girdy 30s Card
        val gameCard = LinearLayout(this)
        gameCard.orientation = LinearLayout.HORIZONTAL
        gameCard.gravity = Gravity.CENTER_VERTICAL
        gameCard.setPadding(20, 18, 20, 18)
        gameCard.background = box("#FFFDF9", "#9B1B30", 3, 22f)
        val gameLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        gameLp.bottomMargin = 20
        gameCard.layoutParams = gameLp

        val gameTextCol = LinearLayout(this)
        gameTextCol.orientation = LinearLayout.VERTICAL
        gameTextCol.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val gTitle = TextView(this)
        gTitle.text = "🎰 Girdy 30s Royal Arena"
        gTitle.textSize = 16f
        gTitle.setTypeface(gTitle.typeface, Typeface.BOLD)
        gTitle.setTextColor(Color.parseColor("#5C101D"))

        val gSub = TextView(this)
        gSub.text = "30-Sec Fast Gold Coins Round"
        gSub.textSize = 13f
        gSub.setTextColor(Color.parseColor("#8C6D3F"))

        gameTextCol.addView(gTitle)
        gameTextCol.addView(gSub)

        val btnPlayGame = Button(this)
        btnPlayGame.text = "Play 30s"
        btnPlayGame.setTextColor(Color.WHITE)
        btnPlayGame.background = box("#9B1B30", "#D4AF37", 2, 30f)
        btnPlayGame.setOnClickListener {
            try {
                val intent = Intent(this, Class.forName("com.gupshap.pro.GreedyGameActivity"))
                startActivity(intent)
            } catch (_: Exception) {
            }
        }

        gameCard.addView(gameTextCol)
        gameCard.addView(btnPlayGame)
        mainCol.addView(gameCard)

        // Featured Grid
        val grid = GridLayout(this)
        grid.columnCount = 2
        val gridLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        gridLp.bottomMargin = 16
        grid.layoutParams = gridLp

        val rooms = listOf(
            Pair("✨ Royal Darbar", "Lv.18"),
            Pair("👑 Maharaja Lounge", "Lv.15"),
            Pair("💖 Ruby Heart Room", "Lv.12"),
            Pair("✦ Golden Palace", "New")
        )

        rooms.forEachIndexed { i, room ->
            val itemCard = LinearLayout(this)
            itemCard.orientation = LinearLayout.VERTICAL
            itemCard.gravity = Gravity.CENTER
            itemCard.setPadding(14, 16, 14, 16)
            itemCard.background = box("#FFFDF8", "#D4AF37", 3, 20f)

            val glp = GridLayout.LayoutParams()
            glp.width = 0
            glp.height = GridLayout.LayoutParams.WRAP_CONTENT
            glp.columnSpec = GridLayout.spec(i % 2, 1f)
            glp.setMargins(if (i % 2 == 0) 0 else 8, 0, if (i % 2 == 0) 8 else 0, 14)
            itemCard.layoutParams = glp

            val badge = TextView(this)
            badge.text = "❖ ${room.second} ❖"
            badge.textSize = 11f
            badge.setTypeface(badge.typeface, Typeface.BOLD)
            badge.setTextColor(Color.parseColor("#9B1B30"))
            badge.setPadding(12, 4, 12, 4)
            badge.background = box("#FFF3D1", "#C59B27", 2, 30f)

            val crown = TextView(this)
            crown.text = "♛"
            crown.textSize = 32f
            crown.setTextColor(Color.parseColor("#B8860B"))
            crown.setPadding(0, 10, 0, 8)

            val name = TextView(this)
            name.text = room.first
            name.textSize = 14f
            name.setTypeface(name.typeface, Typeface.BOLD)
            name.setTextColor(Color.parseColor("#3A280E"))
            name.gravity = Gravity.CENTER

            itemCard.addView(badge)
            itemCard.addView(crown)
            itemCard.addView(name)
            grid.addView(itemCard)
        }
        mainCol.addView(grid)

        // Global Win Ticker
        tvGlobalPati = TextView(this)
        tvGlobalPati.text = "👑 Royal Mega Win Feed • Live on GupShup Pro"
        tvGlobalPati.textSize = 13f
        tvGlobalPati.setTypeface(tvGlobalPati.typeface, Typeface.BOLD)
        tvGlobalPati.setTextColor(Color.parseColor("#4A3515"))
        tvGlobalPati.setPadding(18, 12, 18, 12)
        tvGlobalPati.background = box("#FFF6DC", "#C59B27", 3, 30f)
        mainCol.addView(tvGlobalPati)

        scrollView.addView(mainCol)
        root.addView(scrollView)

        // Bottom Bar
        val bottomBar = LinearLayout(this)
        bottomBar.orientation = LinearLayout.HORIZONTAL
        bottomBar.gravity = Gravity.CENTER
        bottomBar.setPadding(10, 14, 10, 14)
        bottomBar.background = box("#FFFDF9", "#D4AF37", 2, 0f)
        bottomBar.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        )

        val tabs = listOf("🏠 Home", "🧩 Rooms", "💬 Chat", "👤 Me")
        tabs.forEachIndexed { idx, title ->
            val tab = TextView(this)
            tab.text = title
            tab.textSize = 13f
            tab.gravity = Gravity.CENTER
            tab.setTypeface(tab.typeface, if (idx == 0) Typeface.BOLD else Typeface.NORMAL)
            tab.setTextColor(if (idx == 0) Color.parseColor("#0F6B4A") else Color.parseColor("#5C4D3C"))
            tab.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            tab.setOnClickListener {
                if (idx == 3) toggleMyPageDrawer(true)
            }
            bottomBar.addView(tab)
        }
        root.addView(bottomBar)

        // Drawer Backdrop & My Page Drawer
        drawerBackdrop = View(this)
        drawerBackdrop.setBackgroundColor(Color.parseColor("#80000000"))
        drawerBackdrop.visibility = View.GONE
        drawerBackdrop.setOnClickListener { toggleMyPageDrawer(false) }
        root.addView(drawerBackdrop)

        myPageDrawer = buildMyPageDrawer()
        root.addView(myPageDrawer)

        setContentView(root)
    }

    private fun buildMyPageDrawer(): LinearLayout {
        val drawer = LinearLayout(this)
        drawer.orientation = LinearLayout.VERTICAL
        drawer.setPadding(24, 36, 24, 28)
        drawer.visibility = View.GONE
        drawer.background = box("#FAF3E3", "#C59B27", 4, 26f)
        drawer.layoutParams = FrameLayout.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.82).toInt(),
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.END
        )

        val avatar = TextView(this)
        avatar.text = "👑"
        avatar.textSize = 42f
        avatar.gravity = Gravity.CENTER
        avatar.setPadding(24, 16, 24, 16)
        avatar.background = box("#FFF8E3", "#D4AF37", 3, 80f)
        val avatarLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        avatarLp.gravity = Gravity.CENTER_HORIZONTAL
        avatarLp.bottomMargin = 16
        avatar.layoutParams = avatarLp
        drawer.addView(avatar)

        val infoLines = listOf(
            "Name: $userName",
            "ID: $userId",
            "VIP Level: 👑 VIP $vipLevel",
            "Coins: 💰 $userCoins",
            "Gems: 💎 $userGems"
        )

        infoLines.forEach { line ->
            val tv = TextView(this)
            tv.text = line
            tv.textSize = 14f
            tv.setTextColor(Color.parseColor("#2E1F0A"))
            tv.setPadding(0, 4, 0, 4)
            drawer.addView(tv)
        }

        val bio = TextView(this)
        bio.text = "\"Welcome to my royal corner of GupShup!\""
        bio.textSize = 13f
        bio.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        bio.setTextColor(Color.parseColor("#5A462B"))
        bio.setPadding(14, 12, 14, 12)
        bio.background = box("#FFFDF8", "#D4AF37", 2, 16f)
        val bioLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        bioLp.topMargin = 10
        bioLp.bottomMargin = 18
        bio.layoutParams = bioLp
        drawer.addView(bio)

        val actions = listOf("🪞 Edit Profile", "👛 My Wallet", "⚙️ Settings", "🤝 Friends")
        actions.forEach { label ->
            val btn = Button(this)
            btn.text = label
            btn.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            btn.setTextColor(Color.parseColor("#3A280E"))
            btn.background = box("#FFF9EA", "#C59B27", 2, 16f)
            val btnLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            btnLp.bottomMargin = 10
            btn.layoutParams = btnLp
            btn.setOnClickListener { toggleMyPageDrawer(false) }
            drawer.addView(btn)
        }

        return drawer
    }

    private fun toggleMyPageDrawer(open: Boolean) {
        drawerBackdrop.visibility = if (open) View.VISIBLE else View.GONE
        myPageDrawer.visibility = if (open) View.VISIBLE else View.GONE
    }

    private fun listenGlobalWins() {
        dbRef.child("global_wins").limitToLast(1).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val msg = snapshot.child("message").getValue(String::class.java)
                    ?: snapshot.getValue(String::class.java)
                if (!msg.isNullOrBlank()) {
                    tvGlobalPati.text = "👑 $msg"
                    patiAnimator?.cancel()
                    patiAnimator = ObjectAnimator.ofFloat(tvGlobalPati, "alpha", 0.3f, 1f).apply {
                        duration = 600
                        start()
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun box(bg: String, stroke: String, widthPx: Int, radiusPx: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(bg))
            if (widthPx > 0) setStroke(widthPx, Color.parseColor(stroke))
            cornerRadius = radiusPx
        }
    }
}
