package com.gupshup.pro

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.zegocloud.uikit.prebuilt.liveaudioroom.ZegoUIKitPrebuiltLiveAudioRoomConfig
import com.zegocloud.uikit.prebuilt.liveaudioroom.ZegoUIKitPrebuiltLiveAudioRoomFragment
import kotlin.random.Random

class LiveRoomActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var dbRef: DatabaseReference
    private lateinit var roomRef: DatabaseReference
    private lateinit var userRef: DatabaseReference

    private lateinit var tvRoomCoins: TextView
    private lateinit var tvRocketStatus: TextView
    private lateinit var rocketProgress: ProgressBar
    private lateinit var btnTreasureBox: Button

    private var userName: String = "Adil"
    private var roomID: String = "1001"
    private var isHost: Boolean = false
    private var myCoins: Int = 5000
    private var currentRocketCoins: Int = 0
    private val rocketTarget: Int = 10000
    private var treasureUnlocked: Boolean = false

    private val FIREBASE_DB_URL = "https://gupshuppro-9ab9e-default-rtdb.firebaseio.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_room)

        val appID: Long = 1671813519L
        val appSign = "040b2f45d06222c7e4497aa810311b370483c7b1d0440d503ec2e04401692938"

        val userID = "user_${Random.nextInt(10000, 99999)}"
        userName = intent.getStringExtra("USER_NAME") ?: "Adil"
        roomID = intent.getStringExtra("ROOM_ID") ?: "1001"
        isHost = intent.getBooleanExtra("IS_HOST", false)

        prefs = getSharedPreferences("GupShupPrefs", MODE_PRIVATE)
        myCoins = prefs.getInt("user_coins", 5000)

        dbRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).reference
        val userKey = userName.replace(Regex("[^a-zA-Z0-9_]"), "_")
        userRef = dbRef.child("users").child(userKey).child("coins")
        roomRef = dbRef.child("rooms").child(roomID)

        // Start ZEGOCLOUD Audio Room
        val config = if (isHost) {
            ZegoUIKitPrebuiltLiveAudioRoomConfig.host()
        } else {
            ZegoUIKitPrebuiltLiveAudioRoomConfig.audience()
        }

        val fragment = ZegoUIKitPrebuiltLiveAudioRoomFragment.newInstance(
            appID,
            appSign,
            userID,
            userName,
            roomID,
            config
        )

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitNow()

        // Attach Live Haza-style Rocket, Gift & Treasure Overlay
        setupLiveHazaOverlay()
        listenToUserCoins()
        listenToLiveRoomState()
    }

    private fun setupLiveHazaOverlay() {
        val rootView = findViewById<ViewGroup>(android.R.id.content)

        val overlayBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 14, 24, 14)
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#CC1A103C"))
                cornerRadius = 28f
                setStroke(2, Color.parseColor("#FFD700"))
            }
            background = bg
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        tvRoomCoins = TextView(this).apply {
            text = "🪙 $myCoins"
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnSendGift = Button(this).apply {
            text = "🎁 Gift"
            textSize = 12f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.parseColor("#FFD700"))
            setPadding(20, 4, 20, 4)
            setOnClickListener { showGiftDialog() }
        }

        btnTreasureBox = Button(this).apply {
            text = "🔒 Box"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#6A1B9A"))
            setPadding(20, 4, 20, 4)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = 12 }
            layoutParams = lp
            setOnClickListener { claimTreasureBox() }
        }

        topRow.addView(tvRoomCoins)
        topRow.addView(btnSendGift)
        topRow.addView(btnTreasureBox)

        tvRocketStatus = TextView(this).apply {
            text = "🚀 Room $roomID Rocket: 0 / $rocketTarget"
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(0, 8, 0, 4)
        }

        rocketProgress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = rocketTarget
            progress = 0
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                18
            )
        }

        overlayBar.addView(topRow)
        overlayBar.addView(tvRocketStatus)
        overlayBar.addView(rocketProgress)

        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
            setMargins(24, 0, 24, 150)
        }

        rootView.addView(overlayBar, params)
    }

    private fun listenToUserCoins() {
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cloudCoins = snapshot.getValue(Int::class.java)
                if (cloudCoins != null) {
                    myCoins = cloudCoins
                    prefs.edit().putInt("user_coins", myCoins).apply()
                    tvRoomCoins.text = "🪙 $myCoins"
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenToLiveRoomState() {
        roomRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentRocketCoins = snapshot.child("rocketCoins").getValue(Int::class.java) ?: 0
                treasureUnlocked = snapshot.child("treasureUnlocked").getValue(Boolean::class.java) ?: false

                rocketProgress.progress = currentRocketCoins.coerceAtMost(rocketTarget)
                tvRocketStatus.text = "🚀 Room $roomID Rocket: $currentRocketCoins / $rocketTarget"

                if (treasureUnlocked || currentRocketCoins >= rocketTarget) {
                    btnTreasureBox.text = "🎁 OPEN BOX!"
                    btnTreasureBox.setBackgroundColor(Color.parseColor("#00C853"))
                } else {
                    btnTreasureBox.text = "🔒 Box"
                    btnTreasureBox.setBackgroundColor(Color.parseColor("#6A1B9A"))
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showGiftDialog() {
        val gifts = arrayOf(
            "🌹 Rose (500 Coins)",
            "🏎️ Sports Car (2000 Coins)",
            "🚀 Mega Rocket (5000 Coins)"
        )
        val costs = arrayOf(500, 2000, 5000)

        AlertDialog.Builder(this)
            .setTitle("🎁 Send Live Gift (Rocket Boost)")
            .setItems(gifts) { _, which ->
                val cost = costs[which]
                if (myCoins < cost) {
                    Toast.makeText(this, "❌ पर्याप्त Coins नहीं हैं!", Toast.LENGTH_SHORT).show()
                    return@setItems
                }

                myCoins -= cost
                prefs.edit().putInt("user_coins", myCoins).apply()
                tvRoomCoins.text = "🪙 $myCoins"
                userRef.setValue(myCoins)

                // Add gift coins to live room rocket
                roomRef.child("rocketCoins").runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val current = currentData.getValue(Int::class.java) ?: 0
                        val updated = current + cost
                        currentData.value = updated
                        if (updated >= rocketTarget) {
                            roomRef.child("treasureUnlocked").setValue(true)
                        }
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                        Toast.makeText(
                            this@LiveRoomActivity,
                            "🚀 आपने ${gifts[which]} भेजा! Rocket आगे बढ़ा!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun claimTreasureBox() {
        if (!treasureUnlocked && currentRocketCoins < rocketTarget) {
            val remaining = rocketTarget - currentRocketCoins
            Toast.makeText(
                this,
                "🔒 Treasure Box खोलने के लिए $remaining Coins के Gifts और भेजें!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val reward = 1000
        myCoins += reward
        prefs.edit().putInt("user_coins", myCoins).apply()
        tvRoomCoins.text = "🪙 $myCoins"
        userRef.setValue(myCoins)

        // Reset Rocket for next round
        roomRef.child("rocketCoins").setValue(0)
        roomRef.child("treasureUnlocked").setValue(false)

        Toast.makeText(this, "🎉 मुबारक हो! Treasure Box से +$reward Coins मिले!", Toast.LENGTH_LONG).show()
    }
}
