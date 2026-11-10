package com.example.pagosmunicipales

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.appbar.MaterialToolbar  // 🔴 NUEVA IMPORTACIÓN
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar  // 🔴 NUEVA VARIABLE
    private lateinit var textProfileName: TextView
    private lateinit var textProfileEmail: TextView
    private lateinit var textUsername: TextView
    private lateinit var textEmail: TextView
    private lateinit var textUserId: TextView
    private lateinit var cardChangePassword: CardView
    private lateinit var cardHistory: CardView
    private lateinit var btnLogout: MaterialButton

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // 🔴 INICIALIZAR TOOLBAR
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        // Inicializar vistas
        textProfileName = findViewById(R.id.textProfileName)
        textProfileEmail = findViewById(R.id.textProfileEmail)
        textUsername = findViewById(R.id.textUsername)
        textEmail = findViewById(R.id.textEmail)
        textUserId = findViewById(R.id.textUserId)
        cardChangePassword = findViewById(R.id.cardChangePassword)
        cardHistory = findViewById(R.id.cardHistory)
        btnLogout = findViewById(R.id.btnLogout)

        // Cargar datos del usuario
        loadUserData()

        // Configurar listeners
        cardChangePassword.setOnClickListener {
            mostrarDialogoCambiarPassword()
        }

        cardHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            textProfileEmail.text = user.email
            textEmail.text = user.email
            textUserId.text = user.uid.substring(0, 8) + "..."

            // Obtener datos de Firestore
            val docRef = db.collection("users").document(user.uid)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("username")
                        textProfileName.text = username
                        textUsername.text = username
                    } else {
                        textProfileName.text = user.email?.substringBefore("@")
                        textUsername.text = user.email?.substringBefore("@")
                    }
                }
                .addOnFailureListener {
                    textProfileName.text = user.email?.substringBefore("@")
                    textUsername.text = user.email?.substringBefore("@")
                }
        }
    }

    private fun mostrarDialogoCambiarPassword() {
        // Inflar el layout del diálogo
        val dialogView = layoutInflater.inflate(R.layout.dialog_cambiar_password, null)

        // Crear el diálogo
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Inicializar vistas del diálogo
        val editTextCurrentPassword = dialogView.findViewById<TextInputEditText>(R.id.editTextCurrentPassword)
        val editTextNewPassword = dialogView.findViewById<TextInputEditText>(R.id.editTextNewPassword)
        val editTextConfirmPassword = dialogView.findViewById<TextInputEditText>(R.id.editTextConfirmPassword)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val btnCancelar = dialogView.findViewById<MaterialButton>(R.id.btnCancelar)
        val btnCambiar = dialogView.findViewById<MaterialButton>(R.id.btnCambiar)

        // Botón Cancelar
        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        // Botón Cambiar
        btnCambiar.setOnClickListener {
            val currentPassword = editTextCurrentPassword.text.toString().trim()
            val newPassword = editTextNewPassword.text.toString().trim()
            val confirmPassword = editTextConfirmPassword.text.toString().trim()

            // Validaciones
            when {
                currentPassword.isEmpty() -> {
                    editTextCurrentPassword.error = "Ingresa tu contraseña actual"
                    editTextCurrentPassword.requestFocus()
                    return@setOnClickListener
                }
                newPassword.isEmpty() -> {
                    editTextNewPassword.error = "Ingresa la nueva contraseña"
                    editTextNewPassword.requestFocus()
                    return@setOnClickListener
                }
                newPassword.length < 8 -> {
                    editTextNewPassword.error = "Mínimo 8 caracteres"
                    editTextNewPassword.requestFocus()
                    return@setOnClickListener
                }
                confirmPassword.isEmpty() -> {
                    editTextConfirmPassword.error = "Confirma tu nueva contraseña"
                    editTextConfirmPassword.requestFocus()
                    return@setOnClickListener
                }
                newPassword != confirmPassword -> {
                    editTextConfirmPassword.error = "Las contraseñas no coinciden"
                    editTextConfirmPassword.requestFocus()
                    return@setOnClickListener
                }
                newPassword == currentPassword -> {
                    editTextNewPassword.error = "La nueva contraseña debe ser diferente"
                    editTextNewPassword.requestFocus()
                    return@setOnClickListener
                }
            }

            // Mostrar progress, deshabilitar botón
            progressBar.visibility = View.VISIBLE
            btnCambiar.isEnabled = false
            btnCancelar.isEnabled = false

            // CAMBIAR CONTRASEÑA EN FIREBASE
            cambiarPassword(currentPassword, newPassword, dialog, progressBar, btnCambiar, btnCancelar)
        }

        dialog.show()
    }

    private fun cambiarPassword(
        currentPassword: String,
        newPassword: String,
        dialog: AlertDialog,
        progressBar: ProgressBar,
        btnCambiar: MaterialButton,
        btnCancelar: MaterialButton
    ) {
        val user = auth.currentUser ?: return

        // 1. Reautenticar al usuario (necesario para cambiar contraseña)
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    // 2. Si la reautenticación es exitosa, cambiar la contraseña
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            progressBar.visibility = View.GONE
                            btnCambiar.isEnabled = true
                            btnCancelar.isEnabled = true

                            if (updateTask.isSuccessful) {
                                // ✅ Éxito
                                dialog.dismiss()
                                AlertDialog.Builder(this)
                                    .setTitle("✅ Contraseña actualizada")
                                    .setMessage("Tu contraseña se ha cambiado exitosamente.")
                                    .setPositiveButton("Aceptar", null)
                                    .show()
                            } else {
                                // ❌ Error al actualizar
                                val error = updateTask.exception?.message ?: "Error desconocido"
                                Toast.makeText(this, "❌ Error: $error", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    // ❌ Error en reautenticación (contraseña actual incorrecta)
                    progressBar.visibility = View.GONE
                    btnCambiar.isEnabled = true
                    btnCancelar.isEnabled = true

                    val error = reauthTask.exception?.message ?: ""
                    when {
                        error.contains("wrong-password") || error.contains("invalid-credential") -> {
                            Toast.makeText(this, "❌ Contraseña actual incorrecta", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(this, "❌ Error: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
    }
}