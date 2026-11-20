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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
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
            val idToken = account.idToken
            if (idToken != null) {
                firebaseAuthWithGoogle(idToken)
            } else {
                tvLoginStatus.text = "Google Token नहीं मिला, कृपया दोबारा दबाएं।"
            }
        } catch (e: ApiException) {
            tvLoginStatus.text = "Login Error (${e.statusCode}): Firebase में SHA-1 और Google Sign-In चेक करें।"
            Toast.makeText(this, "Google Sign-In Failed: ${e.statusCode}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        tvLoginStatus = findViewById(R.id.tvLoginStatus)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            setupUserAndGoHome(
                currentUser.uid,
                currentUser.email ?: "",
                currentUser.displayName ?: "Royal User",
                currentUser.photoUrl?.toString() ?: ""
            )
            return
        }

        findViewById<Button>(R.id.btnGoogleSignIn).setOnClickListener {
            tvLoginStatus.text = "Google अकाउंट से कनेक्ट हो रहा है..."
            signInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser ?: return@addOnCompleteListener
                    setupUserAndGoHome(
                        user.uid,
                        user.email ?: "",
                        user.displayName ?: "Royal User",
                        user.photoUrl?.toString() ?: ""
                    )
                } else {
                    tvLoginStatus.text = "Firebase Auth Error: ${task.exception?.message}"
                }
            }
    }

    private fun setupUserAndGoHome(firebaseUid: String, email: String, name: String, photoUrl: String) {
        val dbRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).reference
        val prefs = getSharedPreferences("GupShupPrefs", MODE_PRIVATE)
        val isSuperAdmin = email.trim().equals(OWNER_EMAIL, ignoreCase = true)

        dbRef.child("auth_accounts").child(firebaseUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val shortUid: String
                    val initialCoins: Int
                    val isFirstTime: Boolean

                    if (snapshot.exists()) {
                        shortUid = snapshot.child("shortUid").getValue(String::class.java)
                            ?: (100000 + Random.nextInt(899999)).toString()
                        initialCoins = snapshot.child("coins").getValue(Int::class.java) ?: 50000
                        isFirstTime = false
                    } else {
                        shortUid = if (isSuperAdmin) "100001" else (100000 + Random.nextInt(899999)).toString()
                        initialCoins = if (isSuperAdmin) 9999999 else 50000
                        isFirstTime = true

                        val newUserMap = mapOf(
                            "firebaseUid" to firebaseUid,
                            "shortUid" to shortUid,
                            "email" to email,
                            "name" to name,
                            "photoUrl" to photoUrl,
                            "coins" to initialCoins,
                            "diamonds" to if (isSuperAdmin) 99999 else 0,
                            "role" to if (isSuperAdmin) "SUPER_ADMIN" else "USER",
                            "greedyHintEnabled" to isSuperAdmin,
                            "createdAt" to System.currentTimeMillis()
                        )
                        dbRef.child("auth_accounts").child(firebaseUid).setValue(newUserMap)
                        dbRef.child("users").child(shortUid).setValue(newUserMap)
                    }

                    if (isSuperAdmin) {
                        dbRef.child("auth_accounts").child(firebaseUid).child("role").setValue("SUPER_ADMIN")
                        dbRef.child("auth_accounts").child(firebaseUid).child("greedyHintEnabled").setValue(true)
                    }

                    prefs.edit()
                        .putString("firebase_uid", firebaseUid)
                        .putString("user_uid", shortUid)
                        .putString("user_email", email)
                        .putString("user_name", name)
                        .putInt("user_coins", initialCoins)
                        .putBoolean("is_super_admin", isSuperAdmin)
                        .putBoolean("show_welcome_popup", isFirstTime)
                        .apply()

                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@LoginActivity, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
