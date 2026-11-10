package com.example.pagosmunicipales

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        findViewById<MaterialButton>(R.id.btnRegister).setOnClickListener {
            val email = findViewById<TextInputEditText>(R.id.editTextEmail).text.toString().trim()
            val username = findViewById<TextInputEditText>(R.id.editTextUsername).text.toString().trim()
            val password = findViewById<TextInputEditText>(R.id.editTextPassword).text.toString().trim()

            when {
                email.isEmpty() -> {
                    findViewById<TextInputEditText>(R.id.editTextEmail).error = "Ingresa tu correo"
                    return@setOnClickListener
                }
                username.isEmpty() -> {
                    findViewById<TextInputEditText>(R.id.editTextUsername).error = "Ingresa tu usuario"
                    return@setOnClickListener
                }
                password.isEmpty() -> {
                    findViewById<TextInputEditText>(R.id.editTextPassword).error = "Ingresa tu contraseña"
                    return@setOnClickListener
                }
                password.length < 8 -> {
                    findViewById<TextInputEditText>(R.id.editTextPassword).error = "Mínimo 8 caracteres"
                    return@setOnClickListener
                }
            }

            findViewById<ProgressBar>(R.id.progressBar).visibility = android.view.View.VISIBLE
            findViewById<MaterialButton>(R.id.btnRegister).isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val userData = hashMapOf(
                            "username" to username,
                            "email" to email,
                            "createdAt" to System.currentTimeMillis()
                        )

                        user?.let {
                            db.collection("users").document(it.uid)
                                .set(userData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "usuario creado exitosamente", Toast.LENGTH_SHORT).show()

                                    // 🔐 CERRAR SESIÓN PARA QUE OBLIGUE A LOGIN MANUAL
                                    auth.signOut()

                                    val intent = Intent(this, LoginActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "⚠️ Registro OK pero error al guardar: ${e.message}", Toast.LENGTH_LONG).show()

                                    // 🔐 CERRAR SESIÓN TAMBIÉN AQUÍ
                                    auth.signOut()

                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                }
                        }
                    } else {
                        val error = task.exception?.message ?: "Error desconocido"
                        Toast.makeText(this, "❌ $error", Toast.LENGTH_LONG).show()
                    }

                    findViewById<ProgressBar>(R.id.progressBar).visibility = android.view.View.GONE
                    findViewById<MaterialButton>(R.id.btnRegister).isEnabled = true
                }
        }

        findViewById<TextView>(R.id.textLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}