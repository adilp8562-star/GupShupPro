package com.gupshup.pro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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
import com.zegocloud.uikit.prebuilt.liveaudioroom.core.ZegoLiveAudioRoomLayoutAlignment
import com.zegocloud.uikit.prebuilt.liveaudioroom.core.ZegoLiveAudioRoomLayoutConfig
import com.zegocloud.uikit.prebuilt.liveaudioroom.core.ZegoLiveAudioRoomLayoutRowConfig
import kotlin.random.Random

class LiveRoomActivity : AppCompatActivity() {

    private lateinit var dbRef: DatabaseReference
    private lateinit var roomRef: DatabaseReference
    private lateinit var userCoinsRef: DatabaseReference

    private lateinit var tvRoomHeader: TextView
    private lateinit var tvRoomCoins: TextView
    private lateinit var tvRocketStatus: TextView
    private lateinit var rocketProgress: ProgressBar
    private lateinit var btnTreasureBox: Button

    private var userName: String = "Royal Host"
    private var shortUid: String = "100001"
    private var roomID: String = "8888"
    private var isHost: Boolean = false
    private var myCoins: Int = 50000
    private var currentRocketCoins: Int = 0
    private val rocketTarget: Int = 10000
    private var treasureUnlocked: Boolean = false

    private val FIREBASE_DB_URL = "https://gupshuppro-9ab9e-default-rtdb.firebaseio.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_room)

        val appID: Long = 1471013519L
        val appSign = "940b2f45d06222c7e4497aa010311b3704833c7b1d0440d503ec2e04401652938"

        val prefs = getSharedPreferences("GupShupPrefs", MODE_PRIVATE)
        userName = intent.getStringExtra("USER_NAME") ?: prefs.getString("user_name", "Royal Host") ?: "Royal Host"
        shortUid = intent.getStringExtra("USER_UID") ?: prefs.getString("user_uid", "100001") ?: "100001"
        roomID = intent.getStringExtra("ROOM_ID") ?: "8888"
        isHost = intent.getBooleanExtra("IS_HOST", false)
        myCoins = prefs.getInt("user_coins", 50000)

        tvRoomHeader = findViewById(R.id.tvRoomHeader)
        tvRoomCoins = findViewById(R.id.tvRoomCoins)
        tvRocketStatus = findViewById(R.id.tvRocketStatus)
        rocketProgress = findViewById(R.id.rocketProgress)
        btnTreasureBox = findViewById(R.id.btnTreasureBox)

        tvRoomHeader.text = "🎙️ Room #$roomID • 10 Seats"
        tvRoomCoins.text = "🪙 $myCoins"

        dbRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).reference
        userCoinsRef = dbRef.child("users").child(shortUid).child("coins")
        roomRef = dbRef.child("rooms").child(roomID)

        // Real-time Coins Sync
        userCoinsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val liveCoins = snapshot.getValue(Int::class.java)
                if (liveCoins != null) {
                    myCoins = liveCoins
                    tvRoomCoins.text = "🪙 $myCoins"
                    prefs.edit().putInt("user_coins", myCoins).apply()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Real-time Rocket Meter Sync
        roomRef.child("rocketCoins").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentRocketCoins = snapshot.getValue(Int::class.java) ?: 0
                val progress = currentRocketCoins.coerceAtMost(rocketTarget)
                rocketProgress.max = rocketTarget
                rocketProgress.progress = progress

                if (currentRocketCoins >= rocketTarget) {
                    treasureUnlocked = true
                    tvRocketStatus.text = "🚀 ROCKET LAUNCHED! Treasure Ready!"
                    btnTreasureBox.text = "🎁 CLAIM +3,000"
                    btnTreasureBox.isEnabled = true
                } else {
                    treasureUnlocked = false
                    tvRocketStatus.text = "🚀 Rocket Fuel: $currentRocketCoins / $rocketTarget"
                    btnTreasureBox.text = "🔒 Locked"
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Send Rocket Fuel Gift (1,000 Coins)
        findViewById<Button>(R.id.btnSendRocketGift).setOnClickListener {
            if (myCoins < 1000) {
                Toast.makeText(this, "कम से कम 1,000 Coins चाहिए!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val newBalance = myCoins - 1000
            userCoinsRef.setValue(newBalance)

            roomRef.child("rocketCoins").runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val current = currentData.getValue(Int::class.java) ?: 0
                    currentData.value = current + 1000
                    return Transaction.success(currentData)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    Toast.makeText(this@LiveRoomActivity, "🚀 +1,000 Rocket Fuel भेजा गया!", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // Claim Rocket Treasure Box
        btnTreasureBox.setOnClickListener {
            if (!treasureUnlocked) {
                Toast.makeText(this, "पहले Rocket को 10,000 तक पूरा भरें!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            roomRef.child("rocketCoins").setValue(0)
            val bonusCoins = myCoins + 3000
            userCoinsRef.setValue(bonusCoins)
            AlertDialog.Builder(this)
                .setTitle("🚀 ROCKET BLAST JACKPOT!")
                .setMessage("मुबारक हो $userName! Rocket Launch से आपको +3,000 Gold Coins का बोनस मिला!")
                .setPositiveButton("Awesome!", null)
                .show()
        }

        // Open Mini Greedy inside Room
        findViewById<Button>(R.id.btnRoomGreedy).setOnClickListener {
            startActivity(Intent(this, GreedyGameActivity::class.java))
        }

        // Setup 10-Seat ZEGOCLOUD HD Voice Room (2 Rows x 5 Seats = 10 Seats)
        val zegoUserId = "${shortUid}_${Random.nextInt(100, 999)}"
        val config = if (isHost) {
            ZegoUIKitPrebuiltLiveAudioRoomConfig.host()
        } else {
            ZegoUIKitPrebuiltLiveAudioRoomConfig.audience()
        }

        val layoutConfig = ZegoLiveAudioRoomLayoutConfig()
        val row1 = ZegoLiveAudioRoomLayoutRowConfig(5, 12, ZegoLiveAudioRoomLayoutAlignment.SPACE_AROUND)
        val row2 = ZegoLiveAudioRoomLayoutRowConfig(5, 12, ZegoLiveAudioRoomLayoutAlignment.SPACE_AROUND)
        layoutConfig.rowConfigs = mutableListOf(row1, row2)
        config.layoutConfig = layoutConfig

        if (isHost) {
            config.takeSeatIndexWhenJoining = 0
        }

        val fragment = ZegoUIKitPrebuiltLiveAudioRoomFragment.newInstance(
            appID, appSign, zegoUserId, userName, roomID, config
        )
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitNow()
    }
}
