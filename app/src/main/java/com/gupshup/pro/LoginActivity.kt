package com.gupshup.pro

import android.accounts.AccountManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class LoginActivity : AppCompatActivity() {

    private val FIREBASE_DB_URL = "https://gupshuppro-9ab9e-default-rtdb.firebaseio.com"
    private val OWNER_EMAIL = "adilp8562@gmail.com"

    private val NEW_USER_COINS = 500000
    private val NEW_USER_XP = 100000

    private val accountPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val email = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) ?: ""
            if (email.isNotEmpty()) {
                val rawName = email.substringBefore("@").replace(".", " ").replace("_", " ")
                val displayName = rawName.split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }.ifEmpty { "Royal Star" }
                val key = email.replace(Regex("[^a-zA-Z0-9]"), "_")
                completeUserLogin(key, displayName, email)
            } else {
                Toast.makeText(this, "Please select a Google account", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Login Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("GupShupPrefs", MODE_PRIVATE)
        if (prefs.getBoolean("is_logged_in", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#1D0B38"), Color.parseColor("#3B1663"), Color.parseColor("#120724"))
            )
            setPadding(52, 48, 52, 48)
        }

        val tvBadgeTop = TextView(this).apply {
            text = "2X Double Coins & XP Edition 🔥"
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        root.addView(tvBadgeTop)

        val topSpace = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val logoBox = TextView(this).apply {
            text = "👑"
            textSize = 56f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 26f * resources.displayMetrics.density
            }
            layoutParams = LinearLayout.LayoutParams(210, 210)
        }

        val tvTitle = TextView(this).apply {
            text = "GupShup Pro"
            setTextColor(Color.WHITE)
            textSize = 34f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 8)
        }

        val tvBonus = TextView(this).apply {
            text = "🎁 Join Now & Get 🪙 5,00,000 FREE Coins\n⚡ +1,00,000 Starter XP (2X Double Rewards!)"
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        topSpace.addView(logoBox)
        topSpace.addView(tvTitle)
        topSpace.addView(tvBonus)
        root.addView(topSpace)

        val badgeLastUsed = TextView(this).apply {
            text = "  Last used  "
            setTextColor(Color.WHITE)
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#00C49A"))
                cornerRadius = 12f * resources.displayMetrics.density
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.END
                bottomMargin = 8
            }
        }
        root.addView(badgeLastUsed)

        val btnGoogle = Button(this).apply {
            text = "G    Sign in with Google"
            setTextColor(Color.parseColor("#1A1D26"))
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 30f * resources.displayMetrics.density
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 144).apply { bottomMargin = 26 }
        }

        val btnVipGuest = Button(this).apply {
            text = "⚡   Instant VIP Guest Entry (5,00,000 Coins)"
            setTextColor(Color.WHITE)
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1877F2"))
                cornerRadius = 30f * resources.displayMetrics.density
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 144).apply { bottomMargin = 28 }
        }

        val tvTerms = TextView(this).apply {
            text = "Log In means you agree to the Terms of Service & Privacy Policy"
            setTextColor(Color.parseColor("#C4B5FD"))
            textSize = 11f
            gravity = Gravity.CENTER
        }

        root.addView(btnGoogle)
        root.addView(btnVipGuest)
        root.addView(tvTerms)
        setContentView(root)

        btnGoogle.setOnClickListener {
            try {
                val intent = AccountManager.newChooseAccountIntent(
                    null, null, arrayOf("com.google"), null, null, null, null
                )
                accountPickerLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Account Picker Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        btnVipGuest.setOnClickListener {
            val etName = EditText(this).apply {
                hint = "Enter Your Royal Name"
                setText("Its Meroyal")
            }
            AlertDialog.Builder(this)
                .setTitle("👑 Instant VIP Entry (+5,00,000 Coins)")
                .setView(etName)
                .setPositiveButton("Enter Party ➔") { _, _ ->
                    val name = etName.text.toString().trim().ifEmpty { "Royal Host" }
                    completeUserLogin("guest_${System.currentTimeMillis()}", name, "")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun completeUserLogin(accountKey: String, displayName: String, email: String) {
        val isOwner = email.equals(OWNER_EMAIL, ignoreCase = true)
        if (isOwner) {
            saveAndGoMain("100001", displayName, email, 99999999, 99999999, true)
            return
        }

        val dbRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).reference
        dbRef.child("email_map").child(accountKey).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val existingUid = snapshot.getValue(String::class.java)
                if (existingUid != null) {
                    dbRef.child("users").child(existingUid).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnap: DataSnapshot) {
                            val savedCoins = userSnap.child("coins").getValue(Int::class.java) ?: NEW_USER_COINS
                            val savedXp = userSnap.child("xp").getValue(Int::class.java) ?: NEW_USER_XP
                            val savedName = userSnap.child("name").getValue(String::class.java) ?: displayName
                            saveAndGoMain(existingUid, savedName, email, savedCoins, savedXp, false)
                        }
                        override fun onCancelled(error: DatabaseError) {
                            saveAndGoMain(existingUid, displayName, email, NEW_USER_COINS, NEW_USER_XP, false)
                        }
                    })
                } else {
                    val newUid = Random.nextInt(100002, 999999).toString()
                    dbRef.child("email_map").child(accountKey).setValue(newUid)
                    saveAndGoMain(newUid, displayName, email, NEW_USER_COINS, NEW_USER_XP, false)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                saveAndGoMain(Random.nextInt(100002, 999999).toString(), displayName, email, NEW_USER_COINS, NEW_USER_XP, false)
            }
        })
    }

    private fun saveAndGoMain(uid: String, name: String, email: String, coins: Int, xp: Int, isAdmin: Boolean) {
        getSharedPreferences("GupShupPrefs", MODE_PRIVATE).edit()
            .putBoolean("is_logged_in", true)
            .putString("user_uid", uid)
            .putString("user_name", name)
            .putString("user_email", email)
            .putInt("user_coins", coins)
            .putInt("user_xp", xp)
            .putBoolean("is_super_admin", isAdmin)
            .apply()

        FirebaseDatabase.getInstance(FIREBASE_DB_URL).reference.child("users").child(uid).updateChildren(
            mapOf("uid" to uid, "name" to name, "email" to email, "coins" to coins, "xp" to xp, "isSuperAdmin" to isAdmin)
        )

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
