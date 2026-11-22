package com.gupshup.pro

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private val dbUrl = "https://gupshuppro-9ab9e-default-rtdb.firebaseio.com"

    private val googlePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val email = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME).orEmpty()
                if (email.isNotBlank()) {
                    val rawName = email.substringBefore("@").replace(".", " ").replace("_", " ")
                    val displayName = rawName.split(" ")
                        .filter { it.isNotBlank() }
                        .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
                        .ifBlank { "Its Meroyal" }
                    saveAndEnter(displayName, email)
                } else {
                    showFallbackAccounts()
                }
            } else {
                Toast.makeText(this, "Login cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("GupShupPrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_logged_in", false)) {
            goToMain()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.parseColor("#0B0719"))
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 28, 24, 28)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A103C"))
                setStroke(3, Color.parseColor("#FFD700"))
                cornerRadius = 24f
            }
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }

        card.addView(TextView(this).apply {
            text = "👑 ROYAL VIP LOGIN"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFD700"))
            gravity = Gravity.CENTER
        })

        card.addView(TextView(this).apply {
            text = "Select your Gmail account to enter"
            textSize = 12f
            setTextColor(Color.parseColor("#E0AAFF"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply {
                topMargin = 8
                bottomMargin = 20
            }
        })

        card.addView(Button(this).apply {
            text = "G  Continue with Google / Gmail"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A0B2E"))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFD700"))
                cornerRadius = 16f
            }
            layoutParams = LinearLayout.LayoutParams(-1, -2)
            setOnClickListener { openGoogleAccountPicker() }
        })

        root.addView(card)
        setContentView(root)
    }

    private fun openGoogleAccountPicker() {
        try {
            val intent = AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                null,
                null,
                null,
                null
            )
            googlePickerLauncher.launch(intent)
        } catch (_: Exception) {
            showFallbackAccounts()
        }
    }

    private fun showFallbackAccounts() {
        val options = arrayOf(
            "Its Meroyal (meroyal.vip@gmail.com)",
            "Adil Pathan (adil.pathan.official@gmail.com)"
        )
        AlertDialog.Builder(this)
            .setTitle("Choose Gmail Account")
            .setItems(options) { _, which ->
                val chosen = options[which]
                val name = chosen.substringBefore(" (")
                val email = chosen.substringAfter("(").substringBefore(")")
                saveAndEnter(name, email)
            }
            .show()
    }

    private fun saveAndEnter(name: String, email: String) {
        val safeKey = email.replace(Regex("[^A-Za-z0-9]"), "_")
        val uid = "777777"
        val coins = 500000
        val vip = 100

        getSharedPreferences("GupShupPrefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_logged_in", true)
            .putString("user_name", name)
            .putString("user_email", email)
            .putString("short_uid", uid)
            .putInt("user_coins", coins)
            .putInt("vip_level", vip)
            .apply()

        try {
            val db = FirebaseDatabase.getInstance(dbUrl).reference
            db.child("users").child(safeKey).child("name").setValue(name)
            db.child("users").child(safeKey).child("email").setValue(email)
            db.child("users").child(safeKey).child("coins").setValue(coins)
            db.child("users").child(safeKey).child("vipLevel").setValue(vip)
        } catch (_: Exception) {
        }

        goToMain()
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
