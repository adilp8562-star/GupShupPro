package com.gupshup.pro

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zegocloud.uikit.prebuilt.liveaudioroom.ZegoUIKitPrebuiltLiveAudioRoomConfig
import com.zegocloud.uikit.prebuilt.liveaudioroom.ZegoUIKitPrebuiltLiveAudioRoomFragment
import kotlin.random.Random

class LiveRoomActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_room)

        val appID: Long = 1671813519L
        val appSign = "040b2f45d06222c7e4497aa818811b376483c7b1d0446d563ec2e84481692936"

        val userID = "user_${Random.nextInt(10000, 99999)}"
        val userName = intent.getStringExtra("USER_NAME") ?: "Adil"
        val roomID = intent.getStringExtra("ROOM_ID") ?: "1001"
        val isHost = intent.getBooleanExtra("IS_HOST", false)

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
    }
}
