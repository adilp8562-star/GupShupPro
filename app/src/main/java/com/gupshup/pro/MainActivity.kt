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
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
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
    private val ADMIN_WHATSAPP = "919999999999"

    private var currentCoins = 
        private fun buildGamesHubTab(): ScrollView {
        val scroll = ScrollView(this).apply { visibility = View.GONE }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 48)
        }

        box.addView(TextView(this).apply {
            text = "🎮 2X REWARDS & JACKPOT GAMES"
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1D26"))
            setPadding(0, 0, 0, 24)
        })

        val dailyCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeGradientBg("#059669", "#10B981", 20f)
            setPadding(32, 32, 32, 32)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 24 }
        }
        dailyCard.addView(TextView(this).apply {
            text = "🎁 2X DAILY VIP CHECK-IN BONUS"
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        })
        dailyCard.addView(TextView(this).apply {
            text = "Claim 🪙 2,00,000 FREE Coins + ⚡ 1,00,000 EXP every day!"
            setTextColor(Color.parseColor("#ECFDF5"))
            textSize = 13f
            setPadding(0, 8, 0, 20)
        })
        dailyCard.addView(Button(this).apply {
            text = "✨ CLAIM +2,00,000 COINS & +1,00,000 XP"
            setTextColor(Color.parseColor("#064E3B"))
            background = makeRoundedBg("#FFD700", 14f)
            setTypeface(null, Typeface.BOLD)
            setOnClickListener { claimDaily200kCoins() }
        })
        box.addView(dailyCard)

        val gameCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeGradientBg("#4C1D95", "#7C3AED", 20f)
            setPadding(32, 32, 32, 32)
        }
        gameCard.addView(TextView(this).apply {
            text = "🎰 GREEDY & FRUIT JACKPOT WHEEL"
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        })
        gameCard.addView(TextView(this).apply {
            text = "Bet Gold Coins & win up to 45x Mega Jackpot in 30 seconds!"
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(0, 8, 0, 20)
        })
        gameCard.addView(Button(this).apply {
            text = "🔥 PLAY GREEDY & FRUIT GAME"
            setTextColor(Color.WHITE)
            background = makeRoundedBg("#F59E0B", 14f)
            setTypeface(null, Typeface.BOLD)
            setOnClickListener { startActivity(Intent(this@MainActivity, GreedyGameActivity::class.java)) }
        })
        box.addView(gameCard)

        scroll.addView(box)
        return scroll
    }

    private fun buildMessagesTab(): ScrollView {
        val scroll = ScrollView(this).apply { visibility = View.GONE }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 48)
        }
        box.addView(TextView(this).apply {
            text = "💬 OFFICIAL MESSAGES & ALERTS"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1D26"))
            setPadding(0, 0, 0, 24)
        })

        val msgs = listOf(
            Triple("👑 GupShup Official Team", "Welcome to GupShup Pro! Your VIP ID $shortUid has 🪙 $currentCoins & ⚡$currentXp EXP (2X Edition).", "Just now"),
            Triple("🚀 Lv.3 Rocket Launch", "Room 551754 launched a Lv.3 Purple Rocket! Join now to win 40,00,000 Coins!", "2m ago")
        )
        msgs.forEach { (sender, body, time) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = makeRoundedBg("#FFFFFF", 16f)
                setPadding(28, 24, 28, 24)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 18 }
            }
            card.addView(TextView(this).apply {
                text = "$sender   •   $time"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1A1D26"))
            })
            card.addView(TextView(this).apply {
                text = body
                textSize = 13f
                setTextColor(Color.parseColor("#4B5563"))
                setPadding(0, 8, 0, 0)
            })
            box.addView(card)
        }
        scroll.addView(box)
        return scroll
    }

    private fun buildProfileMeTab(): ScrollView {
        val scroll = ScrollView(this).apply { visibility = View.GONE }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 48)
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 12, 8, 24)
            setOnClickListener { showEditProfileDialog() }
        }
        tvProfileAvatar = TextView(this).apply {
            text = userAvatar
            textSize = 42f
            gravity = Gravity.CENTER
            background = makeGradientBg("#23123D", "#5B2C6F", 45f)
            layoutParams = LinearLayout.LayoutParams(165, 165)
        }
        val infoCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 0, 12, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        tvProfileName = TextView(this).apply {
            text = "👑♡♡$userName♡♡👑"
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1D26"))
        }
        tvProfileUid = TextView(this).apply {
            text = "ID:$shortUid 📋  •  Lv.${(currentXp / 50000) + 1}"
            textSize = 13f
            setTextColor(Color.parseColor("#6B7280"))
            setPadding(0, 8, 0, 8)
            setOnClickListener {
                val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("UID", shortUid))
                Toast.makeText(this@MainActivity, "📋 Copied ID: $shortUid", Toast.LENGTH_SHORT).show()
            }
        }
        tvProfileCoins = TextView(this).apply {
            text = "🪙 $currentCoins  •  ⚡ $currentXp XP"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#B45309"))
        }
        infoCol.addView(tvProfileName)
        infoCol.addView(tvProfileUid)
        infoCol.addView(tvProfileCoins)

        val editArrow = TextView(this).apply {
            text = "✏️ ❯"
            textSize = 16f
            setTextColor(Color.parseColor("#8E96AA"))
        }
        headerRow.addView(tvProfileAvatar)
        headerRow.addView(infoCol)
        headerRow.addView(editArrow)
        box.addView(headerRow)

        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 28)
        }
        val statsData = listOf("215\nFriend", "3K\nFollowing", "240\nFollowers", "1.8K\nVisitors")
        statsData.forEach { st ->
            statsRow.addView(TextView(this).apply {
                text = st
                textSize = 14f
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1A1D26"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
        box.addView(statsRow)

        val dualRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 26 }
        }
        val cardSvip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = makeGradientBg("#291D0E", "#85581F", 18f)
            setPadding(28, 30, 28, 30)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = 14 }
            setOnClickListener { showSimplePopup("👑 ROYAL SVIP 7", "Your SVIP 7 Privileges:\n• Golden Entry Ride in all rooms\n• 2X Daily Coins & 2X EXP Boost\n• Exclusive VIP Nameplate & Frame") }
        }
        cardSvip.addView(TextView(this).apply {
            text = "SVIP  👑"
            setTextColor(Color.parseColor("#FDE68A"))
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        })

        val cardWallet = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = makeGradientBg("#0F294A", "#1D4ED8", 18f)
            setPadding(28, 30, 28, 30)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = 14 }
            setOnClickListener { showWalletRechargeDialog() }
        }
        cardWallet.addView(TextView(this).apply {
            text = "Wallet  👛"
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        })
        dualRow.addView(cardSvip)
        dualRow.addView(cardWallet)
        box.addView(dualRow)

        val menuCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeRoundedBg("#FFFFFF", 20f)
            setPadding(28, 12, 28, 12)
        }
        val menuItems = listOf(
            Triple("👥  Invite Friends", "Get +1,00,000 Coins ❯", "#FEF3C7"),
            Triple("👑  Aristocracy", "Royal Duke & King ❯", "#FFFFFF"),
            Triple("🛡️  Family", "Royal Clan ❯", "#FFFFFF"),
            Triple("🏠  CP Space", "Couple Ring ❯", "#FFFFFF"),
            Triple("🏪  Store (Rides & Frames)", "2X VIP Shop ❯", "#FFFFFF"),
            Triple("👕  Dress & Avatar Frames", "Equipped ❯", "#FFFFFF"),
            Triple("🎒  My Bag", "Gifts & Cards ❯", "#FFFFFF"),
            Triple("🏅  Medal & Achievements", "Lv.62 Pro ❯", "#FFFFFF")
        )
        menuItems.forEach { (leftTxt, rightTxt, badgeBg) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(8, 26, 8, 26)
                setOnClickListener {
                    if (leftTxt.contains("Invite")) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Join me on GupShup Pro Voice Party! Use my ID $shortUid & get 5,00,000 FREE Coins!")
                        }
                        startActivity(Intent.createChooser(shareIntent, "Invite Friends"))
                    } else {
                        showSimplePopup(leftTxt.trim(), "Feature unlocked with your SVIP 7 account!\nCurrent Balance: 🪙 $currentCoins\nCurrent EXP: ⚡$currentXp")
                    }
                }
            }
            row.addView(TextView(this).apply {
                text = leftTxt
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1A1D26"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(this).apply {
                text = rightTxt
                textSize = 12f
                setTextColor(Color.parseColor("#B45309"))
                background = makeRoundedBg(badgeBg, 14f)
                setPadding(16, 8, 16, 8)
            })
            menuCard.addView(row)
        }
        box.addView(menuCard)

        layoutSuperAdmin = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeRoundedBg("#FFFBEB", 18f, "#F59E0B", 3)
            setPadding(32, 32, 32, 32)
            visibility = if (isSuperAdmin) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 28 }
        }
        layoutSuperAdmin.addView(TextView(this).apply {
            text = "🛡️ SUPER ADMIN COIN & XP CONTROL"
            setTextColor(Color.parseColor("#B45309"))
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
        })
        val etTargetUid = EditText(this).apply { hint = "Enter User ID (e.g. 100001)" }
        val etSendCoins = EditText(this).apply { hint = "Coins to Add/Deduct (e.g. 1000000)" }
        val btnSend = Button(this).apply {
            text = "⚡ SEND / UPDATE USER COINS"
            setTextColor(Color.WHITE)
            background = makeRoundedBg("#D97706", 12f)
            setTypeface(null, Typeface.BOLD)
            setOnClickListener {
                val targetUid = etTargetUid.text.toString().trim()
                val amount = etSendCoins.text.toString().trim().toIntOrNull()
                if (targetUid.isEmpty() || amount == null) {
                    Toast.makeText(this@MainActivity, "Enter valid ID & Coins!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val targetRef = dbRef.child("users").child(targetUid).child("coins")
                targetRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snap: DataSnapshot) {
                        val old = snap.getValue(Int::class.java) ?: 0
                        val updated = (old + amount).coerceAtLeast(0)
                        targetRef.setValue(updated)
                        Toast.makeText(this@MainActivity, "✅ ID $targetUid updated to 🪙 $updated!", Toast.LENGTH_SHORT).show()
                        etSendCoins.setText("")
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }
        layoutSuperAdmin.addView(etTargetUid)
        layoutSuperAdmin.addView(etSendCoins)
        layoutSuperAdmin.addView(btnSend)
        box.addView(layoutSuperAdmin)

        box.addView(Button(this).apply {
            text = "🔄 Switch Account / Logout"
            setTextColor(Color.WHITE)
            background = makeRoundedBg("#DC2626", 14f)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 120).apply { topMargin = 28 }
            setOnClickListener {
                prefs.edit().clear().apply()
                startActivity(Intent(this@MainActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
        })

        scroll.addView(box)
        return scroll
    }

    private fun showWalletRechargeDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#180A2E"))
            setPadding(36, 36, 36, 36)
        }
        box.addView(TextView(this).apply {
            text = "👛 OFFICIAL 2X COIN WALLET\nBalance: 🪙 $currentCoins  \vert{}  ID:$shortUid"
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 22)
        })

        val packs = listOf(
            Triple("🪙 10,00,000 Coins (2X Offer)", "₹99", 99),
            Triple("🪙 60,00,000 Coins + SVIP 3", "₹499", 499),
            Triple("🪙 1,50,00,000 Coins + SVIP 7", "₹999", 999)
        )
        packs.forEach { (title, price, amt) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = makeRoundedBg("#2E1A47", 14f, "#FFD700", 2)
                setPadding(22, 20, 22, 20)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 14 }
            }
            row.addView(TextView(this).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(Button(this).apply {
                text = "Buy $price"
                setTextColor(Color.parseColor("#180A2E"))
                background = makeRoundedBg("#FFD700", 12f)
                setTypeface(null, Typeface.BOLD)
                setOnClickListener {
                    try {
                        val uri = Uri.parse("upi://pay?pa=$ADMIN_UPI_ID&pn=GupShupPro&am=$amt&cu=INR&tn=ID_$shortUid")
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Send $price to UPI: $ADMIN_UPI_ID with ID$shortUid", Toast.LENGTH_LONG).show()
                    }
                }
            })
            box.addView(row)
        }
        AlertDialog.Builder(this).setView(box).setPositiveButton("CLOSE", null).show()
    }

    private fun switchMainSection(section: Int) {
        pageMeHome.visibility = if (section == 0) View.VISIBLE else View.GONE
        pagePopularHome.visibility = if (section == 1) View.VISIBLE else View.GONE
        pageGamesHub.visibility = if (section == 2) View.VISIBLE else View.GONE
        pageMessages.visibility = if (section == 3) View.VISIBLE else View.GONE
        pageProfileMe.visibility = if (section == 4) View.VISIBLE else View.GONE

        topBarHeader.visibility = if (section == 0 || section == 1) View.VISIBLE else View.GONE

        tvTopMe.setTextColor(if (section == 0) Color.parseColor("#1A1D26") else Color.parseColor("#8E96AA"))
        indTopMe.visibility = if (section == 0) View.VISIBLE else View.INVISIBLE
        tvTopPopular.setTextColor(if (section == 1) Color.parseColor("#1A1D26") else Color.parseColor("#8E96AA"))
        indTopPopular.visibility = if (section == 1) View.VISIBLE else View.INVISIBLE

        val activeBottom = when (section) {
            0, 1 -> 0
            2 -> 1
            3 -> 2
            else -> 3
        }
        bottomNavItems.forEachIndexed { idx, tv ->
            tv.setTextColor(if (idx == activeBottom) Color.parseColor("#00C49A") else Color.parseColor("#8E96AA"))
        }
    }

    private fun loadRoomsData() {
        dbRef.child("rooms").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                data class RoomModel(val id: String, val title: String, val host: String, val avatar: String, val gender: String, val count: Int, val tag: String, val c1: String, val c2: String)
                val rooms = mutableListOf<RoomModel>()
                for (snap in snapshot.children) {
                    val rId = snap.key ?: continue
                    val title = snap.child("title").getValue(String::class.java) ?: "Royal Voice Party"
                    val host = snap.child("host").getValue(String::class.java) ?: "VIP Host"
                    rooms.add(RoomModel(rId, title, host, "👑", "♂", Random.nextInt(6, 35), "New", 
