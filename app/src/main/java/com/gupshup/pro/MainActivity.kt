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
            setPadding
            
