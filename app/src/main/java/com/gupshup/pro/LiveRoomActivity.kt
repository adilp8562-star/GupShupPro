package com.gupshup.pro

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import im.zego.zegoexpress.ZegoExpressEngine
import im.zego.zegoexpress.constants.ZegoScenario
import im.zego.zegoexpress.entity.ZegoEngineProfile
import im.zego.zegoexpress.entity.ZegoRoomConfig
import im.zego.zegoexpress.entity.ZegoUser
import kotlin.random.Random

class LiveRoomActivity : AppCompatActivity() {

    private val ZEGO_APP_ID = 1484647939L
    private val ZEGO_APP_SIGN = "9214518432350226502849284031094810293847561029384756102938475610"
    private val FIREBASE_DB_URL = "https://gupshuppro-9ab9e-default-rtdb.firebaseio.com"

    private var roomId = "100101"
    private var isHost = false
    private var shortUid = "100001"
        private var userName = "Its Meroyal"
    private var userAvatar = "👑"
    private var currentCoins = 500000
    private var currentXp = 100000
    private var isMicMuted = false
    private var mySeatIndex = -1
    private var rocketPoints = 35
    private var rocketLevel = 1

    private lateinit var dbRef: DatabaseReference
    private lateinit var prefs: android.content.SharedPreferences
    private var zegoEngine: ZegoExpressEngine? = null

    private lateinit var tvRocketStatus: TextView
    private lateinit var tvRoomCoins: TextView
    private lateinit var chatScroll: ScrollView
    private lateinit var chatBox: LinearLayout
    private lateinit var btnMicToggle: TextView
    private val seatViews = mutableListOf<LinearLayout>()
    private val seatNames = mutableListOf<TextView>()
    private val seatAvatars = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            roomId = intent.getStringExtra("ROOM_ID") ?: "100101"
            isHost = intent.getBooleanExtra("IS_HOST", false)
            prefs = getSharedPreferences("GupShupPrefs", MODE_PRIVATE)
            shortUid = prefs.getString("user_uid", "100001") ?: "100001"
            userName = prefs.getString("user_name", "Its Meroyal") ?: "Its Meroyal"
            userAvatar = prefs.getString("user_avatar", "👑") ?: "👑"
            currentCoins = prefs.getInt("user_coins", 500000)
            currentXp = prefs.getInt("user_xp", 100000)

            dbRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).reference
            initZegoAudio()
            buildRoyalRoomUI()
            listenRoomRealtime()
            if (isHost) takeSeat(0)
        } catch (e: Exception) {
            setContentView(TextView(this).apply { text = "Room Safe Mode: ${e.message}"; setTextColor(Color.WHITE); setBackgroundColor(Color.BLACK) })
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
        return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.parseColor(startHex), Color.parseColor(endHex))).apply {
            cornerRadius = radiusDp * resources.displayMetrics.density
        }
    }
        private fun buildRoyalRoomUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeGradientBg("#1E0938", "#3B0D5E", 0f)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(28, 26, 28, 16)
        }
        val roomInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeRoundedBg("#44000000", 20f)
            setPadding(24, 12, 24, 12)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        roomInfo.addView(TextView(this).apply {
            text = "👑 Royal Voice Room #$roomId"
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
        })
        tvRoomCoins = TextView(this).apply {
            text = "🪙 $currentCoins  •  ⚡ $currentXp XP"
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
        }
        roomInfo.addView(tvRoomCoins)

        val btnRanking = TextView(this).apply {
            text = " 🏆 "
            textSize = 20f
            background = makeRoundedBg("#55000000", 20f)
            setPadding(16, 12, 16, 12)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { leftMargin = 14 }
            setOnClickListener { showRoomPodiumDialog() }
        }
        val btnExit = TextView(this).apply {
            text = " ✕ "
            setTextColor(Color.WHITE)
            textSize = 18f
            background = makeRoundedBg("#66DC2626", 20f)
            setPadding(18, 12, 18, 12)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { leftMargin = 14 }
            setOnClickListener { leaveSeat(); finish() }
        }
        topBar.addView(roomInfo)
        topBar.addView(btnRanking)
        topBar.addView(btnExit)
        root.addView(topBar)

        val rocketStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = makeRoundedBg("#33FFD700", 16f, "#FFD700", 2)
            setPadding(22, 14, 22, 14)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(28, 6, 28, 16)
            }
            setOnClickListener { showRocketDialog() }
        }
        tvRocketStatus = TextView(this).apply {
            text = "🚀 Space Rocket Lv.$rocketLevel  ($rocketPoints/100)  •  Tap to Fuel 🔥"
            setTextColor(Color.parseColor("#FDE68A"))
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnLuckyPacket = TextView(this).apply {
            text = " 🧧 Lucky Bag "
            setTextColor(Color.WHITE)
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            background = makeRoundedBg("#DC2626", 14f)
            setPadding(16, 10, 16, 10)
            setOnClickListener { showLuckyPacketDialog() }
        }
        rocketStrip.addView(tvRocketStatus)
        rocketStrip.addView(btnLuckyPacket)
        root.addView(rocketStrip)
                val seatsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 12)
        }
        seatViews.clear(); seatNames.clear(); seatAvatars.clear()
        for (rowIdx in 0 until 3) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 14 }
            }
            for (colIdx in 0 until 3) {
                val seatIdx = rowIdx * 3 + colIdx
                val isBossSeat = (seatIdx == 0)
                val seatCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = makeRoundedBg(if (isBossSeat) "#33FFD700" else "#22FFFFFF", 18f, if (isBossSeat) "#FFD700" else "#55FFFFFF", 2)
                    setPadding(8, 16, 8, 16)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (colIdx < 2) rightMargin = 12
                    }
                    setOnClickListener { onSeatClicked(seatIdx) }
                }
                val av = TextView(this).apply {
                    text = if (isBossSeat) "👑" else "🛋️"
                    textSize = 26f
                    gravity = Gravity.CENTER
                }
                val nm = TextView(this).apply {
                    text = if (isBossSeat) "Host Seat" else "Seat ${seatIdx + 1}"
                    setTextColor(if (isBossSeat) Color.parseColor("#FFD700") else Color.WHITE)
                    textSize = 11f
                    maxLines = 1
                    gravity = Gravity.CENTER
                    setTypeface(null, Typeface.BOLD)
                    setPadding(0, 6, 0, 0)
                }
                seatCard.addView(av)
                seatCard.addView(nm)
                seatViews.add(seatCard)
                seatAvatars.add(av)
                seatNames.add(nm)
                row.addView(seatCard)
            }
            seatsContainer.addView(row)
        }
        root.addView(seatsContainer)

        chatScroll = ScrollView(this).apply {
            background = makeRoundedBg("#33000000", 18f)
            setPadding(24, 18, 24, 18)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                setMargins(28, 6, 28, 14)
            }
        }
        chatBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        chatScroll.addView(chatBox)
        root.addView(chatScroll)

        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#2A000000"))
            setPadding(24, 16, 24, 20)
        }
        val btnChat = TextView(this).apply {
            text = "💬 Say hi..."
            setTextColor(Color.WHITE)
            textSize = 13f
            background = makeRoundedBg("#33FFFFFF", 20f)
            setPadding(26, 16, 26, 16)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showSendChatDialog() }
        }
        btnMicToggle = TextView(this).apply {
            text = " 🎙️ "
            textSize = 20f
            background = makeRoundedBg("#059669", 22f)
            setPadding(18, 12, 18, 12)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { leftMargin = 14 }
            setOnClickListener { toggleMic() }
        }
        val btnWheel = TextView(this).apply {
            text = " 🎰 "
            textSize = 20f
            background = makeRoundedBg("#7C3AED", 22f)
            setPadding(18, 12, 18, 12)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { leftMargin = 14 }
            setOnClickListener { startActivity(Intent(this@LiveRoomActivity, GreedyGameActivity::class.java)) }
        }
        val btnGift = TextView(this).apply {
            text = " 🎁 "
            textSize = 20f
            background = makeGradientBg("#F59E0B", "#DC2626", 22f)
            setPadding(20, 12, 20, 12)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { leftMargin = 14 }
            setOnClickListener { showGiftBoxDialog() }
        }
        bottomBar.addView(btnChat)
        bottomBar.addView(btnMicToggle)
        bottomBar.addView(btnWheel)
        bottomBar.addView(btnGift)
        root.addView(bottomBar)

        setContentView(root)
        addChatLine("👑 System", "Welcome to Royal Room #$roomId! Tap any sofa seat to speak on mic.")
        }
            private fun initZegoAudio() {
        try {
            val profile = ZegoEngineProfile().apply {
                appID = ZEGO_APP_ID
                appSign = ZEGO_APP_SIGN
                scenario = ZegoScenario.GENERAL
                application = applicationContext
            }
            zegoEngine = ZegoExpressEngine.createEngine(profile, null)
            zegoEngine?.loginRoom(roomId, ZegoUser(shortUid, userName), ZegoRoomConfig())
            zegoEngine?.muteMicrophone(true)
        } catch (_: Exception) {}
    }

    private fun onSeatClicked(seatIdx: Int) {
        if (mySeatIndex == seatIdx) {
            AlertDialog.Builder(this)
                .setTitle("🛋️ Seat ${seatIdx + 1}")
                .setMessage("Leave your mic seat?")
                .setPositiveButton("Leave Seat") { _, _ -> leaveSeat() }
                .setNegativeButton("Stay", null)
                .show()
        } else {
            takeSeat(seatIdx)
        }
    }

    private fun takeSeat(seatIdx: Int) {
        if (mySeatIndex != -1) leaveSeat()
        mySeatIndex = seatIdx
        isMicMuted = false
        zegoEngine?.muteMicrophone(false)
        zegoEngine?.startPublishingStream("stream_${roomId}_$shortUid")
        btnMicToggle.text = " 🎙️ "
        btnMicToggle.background = makeRoundedBg("#059669", 22f)
        dbRef.child("rooms").child(roomId).child("seats").child(seatIdx.toString())
            .setValue(mapOf("uid" to shortUid, "name" to userName, "avatar" to userAvatar))
        addChatLine("🎙️ Seat", "$userName took Seat ${seatIdx + 1}")
    }

    private fun leaveSeat() {
        if (mySeatIndex != -1) {
            dbRef.child("rooms").child(roomId).child("seats").child(mySeatIndex.toString()).removeValue()
            mySeatIndex = -1
        }
        zegoEngine?.stopPublishingStream()
        zegoEngine?.muteMicrophone(true)
    }

    private fun toggleMic() {
        if (mySeatIndex == -1) {
            Toast.makeText(this, "Tap any empty sofa seat first!", Toast.LENGTH_SHORT).show()
            return
        }
        isMicMuted = !isMicMuted
        zegoEngine?.muteMicrophone(isMicMuted)
        btnMicToggle.text = if (isMicMuted) " 🔇 " else " 🎙️ "
        btnMicToggle.background = makeRoundedBg(if (isMicMuted) "#DC2626" else "#059669", 22f)
    }

    private fun showGiftBoxDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#180A2E"))
            setPadding(32, 32, 32, 32)
        }
        box.addView(TextView(this).apply {
            text = "🎁 ROYAL GIFT STORE (2X EXP)\nBalance: 🪙 $currentCoins"
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        })

        val gifts = listOf(
            Triple("🌹 Royal Rose", 1000, 5),
            Triple("🏎️ Golden Ferrari", 50000, 25),
            Triple("💍 Diamond Ring (CP)", 100000, 45),
            Triple("🏰 Royal Castle", 500000, 100)
        )
        gifts.forEach { (gName, cost, rPts) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = makeRoundedBg("#2E1A47", 14f, "#FFD700", 2)
                setPadding(20, 18, 20, 18)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 12 }
            }
            row.addView(TextView(this).apply {
                text = "$gName\n🪙 $cost  •  +$rPts Rocket Fuel"
                setTextColor(Color.WHITE)
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(Button(this).apply {
                text = "SEND"
                setTextColor(Color.parseColor("#180A2E"))
                background = makeRoundedBg("#FFD700", 12f)
                setTypeface(null, Typeface.BOLD)
                setOnClickListener { sendGift(gName, cost, rPts) }
            })
            box.addView(row)
        }
        AlertDialog.Builder(this).setView(box).setPositiveButton("CLOSE", null).show()
    }

    private fun sendGift(giftName: String, cost: Int, fuel: Int) {
        if (currentCoins < cost) {
            Toast.makeText(this, "Not enough coins! Claim daily 2,00,000 coins!", Toast.LENGTH_SHORT).show()
            return
        }
        currentCoins -= cost
        currentXp += cost * 2
        prefs.edit().putInt("user_coins", currentCoins).putInt("user_xp", currentXp).apply()
        dbRef.child("users").child(shortUid).updateChildren(mapOf("coins" to currentCoins, "xp" to currentXp))
        tvRoomCoins.text = "🪙 $currentCoins  •  ⚡ $currentXp XP"

        rocketPoints += fuel
        if (rocketPoints >= 100) {
            rocketPoints -= 100
            rocketLevel = if (rocketLevel >= 3) 1 else rocketLevel + 1
            val winBonus = 100000 * rocketLevel
            currentCoins += winBonus
            prefs.edit().putInt("user_coins", currentCoins).apply()
            dbRef.child("users").child(shortUid).child("coins").setValue(currentCoins)
            addChatLine("🚀 ROCKET LAUNCH!", "Lv.$rocketLevel Space Rocket Launched! Everyone won 🪙 $winBonus Coins!")
        }
        tvRocketStatus.text = "🚀 Space Rocket Lv.$rocketLevel  ($rocketPoints/100)  •  Tap to Fuel 🔥"
        addChatLine("🎁 Gift", "$userName sent $giftName (🪙 $cost)!")
    }

    private fun showRocketDialog() {
        AlertDialog.Builder(this)
            .setTitle("🚀 3-LEVEL SPACE ROCKET (Lv.$rocketLevel)")
            .setMessage("Current Fuel: $rocketPoints / 100\n\nSend gifts in this room to charge the Rocket!\n• Lv.1 Launch = 🪙 1,00,000 Reward\n• Lv.2 Launch = 🪙 2,00,000 Reward\n• Lv.3 Purple Rocket = 🪙 3,00,000 Mega Reward!")
            .setPositiveButton("SEND GIFT TO FUEL 🎁") { _, _ -> showGiftBoxDialog() }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showLuckyPacketDialog() {
        val bonus = Random.nextInt(10000, 50000)
        currentCoins += bonus
        prefs.edit().putInt("user_coins", currentCoins).apply()
        dbRef.child("users").child(shortUid).child("coins").setValue(currentCoins)
        tvRoomCoins.text = "🪙 $currentCoins  •  ⚡ $currentXp XP"
        addChatLine("🧧 Lucky Bag", "$userName grabbed 🪙 $bonus Coins from the Red Envelope!")
        AlertDialog.Builder(this)
            .setTitle("🧧 RED LUCKY PACKET OPENED!")
            .setMessage("Congratulations! You grabbed 🪙 $bonus FREE Coins!")
            .setPositiveButton("AWESOME ✨", null)
            .show()
    }

    private fun showRoomPodiumDialog() {
        AlertDialog.Builder(this)
            .setTitle("🏆 ROOM CHARM & WEALTH PODIUM")
            .setMessage("🥇 #1  👑 $userName  —  🪙 $currentCoins\n🥈 #2  🔥 Dimple 💔  —  🪙 2,62,800\n🥉 #3  🦁 THALAPATI  —  🪙 1,97,000")
            .setPositiveButton("CLOSE", null)
            .show()
    }

    private fun showSendChatDialog() {
        val et = EditText(this).apply { hint = "Type your message..." }
        AlertDialog.Builder(this)
            .setTitle("💬 Send Room Message")
            .setView(et)
            .setPositiveButton("Send") { _, _ ->
                val msg = et.text.toString().trim()
                if (msg.isNotEmpty()) {
                    dbRef.child("rooms").child(roomId).child("chat").push()
                        .setValue(mapOf("sender" to userName, "text" to msg))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addChatLine(sender: String, text: String) {
        val tv = TextView(this).apply {
            this.text = "$sender: $text"
            setTextColor(Color.WHITE)
            textSize = 13f
            background = makeRoundedBg("#44000000", 12f)
            setPadding(18, 10, 18, 10)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 10 }
        }
        chatBox.addView(tv)
        Handler(Looper.getMainLooper()).post { chatScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun listenRoomRealtime() {
        dbRef.child("rooms").child(roomId).child("seats").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (i in 0 until 9) {
                    val s = snapshot.child(i.toString())
                    if (s.exists()) {
                        seatAvatars[i].text = s.child("avatar").getValue(String::class.java) ?: "👑"
                        seatNames[i].text = s.child("name").getValue(String::class.java) ?: "VIP"
                    } else {
                        seatAvatars[i].text = if (i == 0) "👑" else "🛋️"
                        seatNames[i].text = if (i == 0) "Host Seat" else "Seat ${i + 1}"
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        dbRef.child("rooms").child(roomId).child("chat").limitToLast(20).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, prev: String?) {
                val sender = snapshot.child("sender").getValue(String::class.java) ?: return
                val msg = snapshot.child("text").getValue(String::class.java) ?: return
                addChatLine("👑 $sender", msg)
            }
            override fun onChildChanged(snapshot: DataSnapshot, prev: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, prev: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        leaveSeat()
        try {
            zegoEngine?.logoutRoom(roomId)
            ZegoExpressEngine.destroyEngine(null)
        } catch (_: Exception) {}
    }
}
