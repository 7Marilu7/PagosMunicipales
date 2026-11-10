package com.example.pagosmunicipales

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var textRegister: TextView
    private lateinit var textForgotPassword: TextView  // 🔴 NUEVO
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()

        // Verificar si ya hay usuario logueado
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Inicializar vistas
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        btnLogin = findViewById(R.id.btnLogin)
        textRegister = findViewById(R.id.textRegister)
        textForgotPassword = findViewById(R.id.textForgotPassword)  // 🔴 NUEVO
        progressBar = findViewById(R.id.progressBar)

        btnLogin.setOnClickListener {
            loginUser()
        }

        textRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        // 🔴 NUEVO - Listener para restablecer contraseña
        textForgotPassword.setOnClickListener {
            mostrarDialogoRestablecerPassword()
        }
    }

    private fun loginUser() {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        // Validaciones
        when {
            email.isEmpty() -> {
                editTextEmail.error = "Ingresa tu correo electrónico"
                editTextEmail.requestFocus()
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                editTextEmail.error = "Ingresa un correo válido"
                editTextEmail.requestFocus()
                return
            }
            password.isEmpty() -> {
                editTextPassword.error = "Ingresa tu contraseña"
                editTextPassword.requestFocus()
                return
            }
        }

        progressBar.visibility = View.VISIBLE
        btnLogin.isEnabled = false

        // Iniciar sesión con Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressBar.visibility = View.GONE
                btnLogin.isEnabled = true

                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Inicio de sesión exitoso",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Redirigir a pantalla principal
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Error: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    // 🔴 NUEVA FUNCIÓN
    private fun mostrarDialogoRestablecerPassword() {
        // Inflar el layout del diálogo
        val dialogView = layoutInflater.inflate(R.layout.dialog_restablecer_password, null)

        // Crear el diálogo
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Inicializar vistas del diálogo
        val editTextEmail = dialogView.findViewById<TextInputEditText>(R.id.editTextEmail)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val btnCancelar = dialogView.findViewById<MaterialButton>(R.id.btnCancelar)
        val btnEnviar = dialogView.findViewById<MaterialButton>(R.id.btnEnviar)

        // Botón Cancelar
        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        // Botón Enviar
        btnEnviar.setOnClickListener {
            val email = editTextEmail.text.toString().trim()

            // Validación
            when {
                email.isEmpty() -> {
                    editTextEmail.error = "Ingresa tu correo electrónico"
                    editTextEmail.requestFocus()
                    return@setOnClickListener
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    editTextEmail.error = "Ingresa un correo válido"
                    editTextEmail.requestFocus()
                    return@setOnClickListener
                }
            }

            // Mostrar progress, deshabilitar botón
            progressBar.visibility = View.VISIBLE
            btnEnviar.isEnabled = false
            btnCancelar.isEnabled = false

            // Enviar correo de restablecimiento
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.GONE
                    btnEnviar.isEnabled = true
                    btnCancelar.isEnabled = true

                    if (task.isSuccessful) {
                        // ✅ Éxito
                        dialog.dismiss()
                        AlertDialog.Builder(this)
                            .setTitle("📧 Correo enviado")
                            .setMessage("Hemos enviado un enlace para restablecer tu contraseña a:\n\n$email")
                            .setPositiveButton("Aceptar", null)
                            .show()
                    } else {
                        // ❌ Error
                        val error = task.exception?.message ?: "Error desconocido"
                        Toast.makeText(this, "❌ Error: $error", Toast.LENGTH_LONG).show()
                    }
                }
        }

        dialog.show()
    }
}