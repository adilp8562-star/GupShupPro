package com.gupshup.pro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var tvLoginStatus: TextView

    private val OWNER_EMAIL = "adilp8562@gmail.com"
    private val FIREBASE_DB_URL = "https://gupshuppro-9ab9e-default-rtdb.firebaseio.com"

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account.email ?: ""
            val name = account.displayName ?: "Royal User"
            val googleKey = (account.id ?: email).replace(Regex("[^a-zA-Z0-9]"), "_")

            tvLoginStatus.text = "✅ Welcome $name! Setting up Royal ID..."
            setupUserAndGoHome(googleKey, name, email)
        } catch (e: ApiException) {
            tvLoginStatus.text = "Login Cancelled (${e.statusCode}) - कृपया दोबारा दबाएं"
            Toast.makeText(this, "Google Sign-In Error: ${e.statusCode}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        tvLoginStatus = findViewById(R.id.tvLoginStatus)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val prefs = getSharedPreferences("GupShupPrefs", MODE_PRIVATE)
        val savedUid = prefs.getString("user_uid", null)
        if (savedUid != null) {
            goToMain()
            return
        }

        findViewById<Button>(R.id.btnGoogleSignIn).setOnClickListener {
            tvLoginStatus.text = "Opening Google Accounts..."
            signInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun setupUserAndGoHome(googleKey: String, name: String, email: String) {
        val dbRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).reference
        val mappingRef = dbRef.child("google_uid_map").child(googleKey)
        val isSuperAdmin = email.equals(OWNER_EMAIL, ignoreCase = true)

        mappingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val existingShortUid = snapshot.getValue(String::class.java)
                if (existingShortUid != null) {
                    dbRef.child("users").child(existingShortUid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnap: DataSnapshot) {
                                val coins = userSnap.child("coins").getValue(Int::class.java) ?: 50000
                                saveLocalAndOpen(existingShortUid, name, email, coins, isSuperAdmin, false)
                            }
                            override fun onCancelled(error: DatabaseError) {
                                saveLocalAndOpen(existingShortUid, name, email, 50000, isSuperAdmin, false)
                            }
                        })
                } else {
                    val newShortUid = Random.nextInt(100000, 999999).toString()
                    val initialCoins = if (isSuperAdmin) 999999 else 50000

                    mappingRef.setValue(newShortUid)
                    val userMap = mapOf(
                        "uid" to newShortUid,
                        "name" to name,
                        "email" to email,
                        "coins" to initialCoins,
                        "isSuperAdmin" to isSuperAdmin
                    )
                    dbRef.child("users").child(newShortUid).setValue(userMap)
                    saveLocalAndOpen(newShortUid, name, email, initialCoins, isSuperAdmin, true)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                val fallbackUid = Random.nextInt(100000, 999999).toString()
                saveLocalAndOpen(fallbackUid, name, email, 50000, isSuperAdmin, true)
            }
        })
    }

    private fun saveLocalAndOpen(
        shortUid: String,
        name: String,
        email: String,
        coins: Int,
        isSuperAdmin: Boolean,
        isNewUser: Boolean
    ) {
        getSharedPreferences("GupShupPrefs", MODE_PRIVATE).edit()
            .putString("user_uid", shortUid)
            .putString("user_name", name)
            .putString("user_email", email)
            .putInt("user_coins", coins)
            .putBoolean("is_super_admin", isSuperAdmin)
            .putBoolean("show_welcome_popup", isNewUser)
            .apply()

        goToMain()
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
