package com.gupshup.pro

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val dbUrl = "https://gupshuppro-9ab9e-default-rtdb.firebaseio.com"
    private lateinit var dbRef: DatabaseReference

    private var userId = "777777"
    private var userName = "Its Meroyal"
    private var userCoins = 500000
    private var vipLevel = 100
    private val officialRoomId = "100001"

    private lateinit var tvCoins: TextView
    private lateinit var tvGlobalPati: TextView
    private var patiAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("GupShupPrefs", Context.MODE_PRIVATE)
        userId = prefs.getString("short_uid", "777777") ?: "
        
