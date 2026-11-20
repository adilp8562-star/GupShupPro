package com.gupshup.pro

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class GreedyGameActivity : AppCompatActivity() {

    private var coins = 5500
    private var selectedChip = 100
    private var isBettingOpen = true

    private lateinit var boxes: List<Button>
    private val bets = IntArray(8) { 0 }
    private val names = listOf("🍔", "🍅", "🍕", "🌽", "🍗", "🥕", "🌭", "👑")
    private val multipliers = listOf(5, 10, 5, 15, 5, 25, 5, 45)
    private val historyList = mutableListOf("🍔", "🍕", "🍅", "🍗", "👑", "🥕")

    private lateinit var tvCoins: TextView
    private lateinit var tvTimerStatus: TextView
    private lateinit var tvCenterDisplay: TextView
    private lateinit var tvHistory: TextView

    private var roundTimer: CountDownTimer? = null
    private val handler = Handler(Looper.getMainLooper())

    
