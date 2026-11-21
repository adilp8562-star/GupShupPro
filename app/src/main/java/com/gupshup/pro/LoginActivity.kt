package com.gupshup.pro

import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class LoginActivity : AppCompatActivity() {

    private lateinit var tvLoginStatus: TextView

    private val OWNER_EMAIL = "adilp8562@gmail.com"
    private val FIREBASE_DB_URL = "https://gupshuppro-9ab9e-default-rtdb.firebaseio.com"

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

                val googleKey = email.replace(Regex("[^a-zA-Z0-9]"), "_")
                tvLoginStatus.text = "✨ Welcome $displayName! Crediting 2,00,000 Coins..."
                setupUserAndGoHome(googleKey, displayName, email)
            } else {
                tvLoginStatus.text = "कृपया एक Google Account चुनें"
            }
        } else {
            tvLoginStatus.text = "Login Cancelled - कृपया दोबारा दबाएं"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        tvLoginStatus = findViewById(R.id.tvLoginStatus)

        val prefs = getSharedPreferences("GupShupPrefs", MODE_PRIVATE)
        val savedUid = prefs.getString("user_uid", null)
        if (savedUid != null) {
            goToMain()
            return
        }

        findViewById<Button>(R.id.btnGoogleSignIn).setOnClickListener {
            tvLoginStatus.text = "Opening Google Accounts..."
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
                accountPickerLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Account Picker Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupUserAndGoHome(googleKey: String, defaultName: String, email: String) {
        val dbRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).reference
        val isOwner = email.equals(OWNER_EMAIL, ignoreCase = true)

        dbRef.child("google_links").child(googleKey)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val existingUid = snapshot.getValue(String::class.java)
                    if (existingUid != null) {
                        dbRef.child("users").child(existingUid)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userSnap: DataSnapshot) {
                                    val name = userSnap.child("name").getValue(String::class.java) ?: defaultName
                                    val bio = userSnap.child("bio").getValue(String::class.java) ?: "Welcome to GupShup Pro! 👑✨"
                                    val gender = userSnap.child("gender").getValue(String::class.java) ?: "Male"
                                    val avatar = userSnap.child("avatar").getValue(String::class.java) ?: "👑"
                                    val coins = userSnap.child("coins").getValue(Int::class.java) ?: if (isOwner) 9999999 else 200000

                                    if (isOwner) {
                                        dbRef.child("users").child(existingUid).child("isSuperAdmin").setValue(true)
                                    }

                                    saveLocalAndGo(existingUid, name, bio, gender, avatar, coins, isOwner)
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    saveLocalAndGo(existingUid, defaultName, "Welcome to GupShup Pro! 👑", "Male", "👑", if (isOwner) 9999999 else 200000, isOwner)
                                }
                            })
                    } else {
                        val newShortUid = if (isOwner) "100001" else Random.nextInt(100002, 999999).toString()
                        val startCoins = if (isOwner) 9999999 else 200000
                        val startAvatar = if (isOwner) "👑" else "🦁"
                        val startBio = if (isOwner) "Founder & Super Admin 👑" else "Royal VIP Member ✨"

                        val userMap = mapOf(
                            "uid" to newShortUid,
                            "name" to defaultName,
                            "email" to email,
                            "bio" to startBio,
                            "gender" to "Male",
                            "avatar" to startAvatar,
                            "coins" to startCoins,
                            "isSuperAdmin" to isOwner
                        )

                        dbRef.child("google_links").child(googleKey).setValue(newShortUid)
                        dbRef.child("users").child(newShortUid).setValue(userMap)

                        saveLocalAndGo(newShortUid, defaultName, startBio, "Male", startAvatar, startCoins, isOwner)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    val fallbackUid = if (isOwner) "100001" else Random.nextInt(100002, 999999).toString()
                    saveLocalAndGo(fallbackUid, defaultName, "Royal VIP Member ✨", "Male", "👑", if (isOwner) 9999999 else 200000, isOwner)
                }
            })
    }

    private fun saveLocalAndGo(
        uid: String,
        name: String,
        bio: String,
        gender: String,
        avatar: String,
        coins: Int,
        isAdmin: Boolean
    ) {
        val prefs = getSharedPreferences("GupShupPrefs", MODE_PRIVATE)
        prefs.edit()
            .putString("user_uid", uid)
            .putString("user_name", name)
            .putString("user_bio", bio)
            .putString("user_gender", gender)
            .putString("user_avatar", avatar)
            .putInt("user_coins", coins)
            .putBoolean("is_super_admin", isAdmin)
            .apply()

        goToMain()
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
