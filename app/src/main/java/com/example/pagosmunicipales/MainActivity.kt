package com.example.pagosmunicipales

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
// para la fecha
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnProfile: ImageView
    private lateinit var textWelcome: TextView
    private lateinit var textUserEmail: TextView
    // FECHA
    private lateinit var textFechaActual: TextView
    private lateinit var cardPredial: CardView
    private lateinit var cardMultas: CardView
    private lateinit var cardLicencias: CardView

    // ÚLTIMO PAGO
    private lateinit var textUltimoPagoConcepto: TextView
    private lateinit var textUltimoPagoMonto: TextView
    private lateinit var textUltimoPagoFecha: TextView
    private lateinit var textUltimoPagoEstatus: TextView

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar Firebase
        auth = Firebase.auth

        // Verificar si hay usuario logueado
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        // Inicializar vistas
        toolbar = findViewById(R.id.toolbar)
        btnProfile = findViewById(R.id.btnProfile)
        textWelcome = findViewById(R.id.textWelcome)
        textUserEmail = findViewById(R.id.textUserEmail)
        // Inicializar el TextView de fecha
        textFechaActual = findViewById(R.id.textFechaActual)
        cardPredial = findViewById(R.id.cardPredial)
        cardMultas = findViewById(R.id.cardMultas)
        cardLicencias = findViewById(R.id.cardLicencias)
        // VISTAS DEL ÚLTIMO PAGO
        textUltimoPagoConcepto = findViewById(R.id.textUltimoPagoConcepto)
        textUltimoPagoMonto = findViewById(R.id.textUltimoPagoMonto)
        textUltimoPagoFecha = findViewById(R.id.textUltimoPagoFecha)
        textUltimoPagoEstatus = findViewById(R.id.textUltimoPagoEstatus)
        setSupportActionBar(toolbar)

        // LLAMAR a la función para mostrar la fecha
        mostrarFechaActual()
        // Cargar datos del usuario
        loadUserData()
        // CARGAR ÚLTIMO PAGO
        cargarUltimoPago()
        // Después de cargarUltimoPago()
        textUltimoPagoConcepto.post {
            cargarUltimoPago()
        }
        // Configurar listeners
        btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        cardPredial.setOnClickListener {
            startActivity(Intent(this, PagoPredialActivity::class.java))
        }

        cardMultas.setOnClickListener {
            startActivity(Intent(this, PagoMultasActivity::class.java))
        }

        cardLicencias.setOnClickListener {
            startActivity(Intent(this, PagoLicenciasActivity::class.java))
        }
    }
    // FUNCIÓN NUEVA para mostrar la fecha actual
    private fun mostrarFechaActual() {
        // Crear formato de fecha en español (ejemplo: "21 de abril 2026")
        val formatoFecha = SimpleDateFormat("dd 'de' MMMM yyyy", Locale("es", "MX"))
        val fechaActual = formatoFecha.format(Date())
        textFechaActual.text = " $fechaActual"
    }
    //  FUNCIÓN NUEVA: Cargar el último pago desde Firestore
    private fun cargarUltimoPago() {
        val currentUser = auth.currentUser
        if (currentUser == null) return

        val emailUsuario = currentUser.email ?: return

        db.collection("PAGOS")
            .whereEqualTo("usuario", emailUsuario)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val pago = documents.documents[0]
                    textUltimoPagoConcepto.text = pago.getString("concepto") ?: "Sin concepto"
                    textUltimoPagoMonto.text = String.format(" $%,.2f", pago.getDouble("monto") ?: 0.0)

                    val timestamp = pago.getTimestamp("timestamp")
                    if (timestamp != null) {
                        val fecha = timestamp.toDate()
                        val formato = SimpleDateFormat("dd MMM yyyy", Locale("es", "MX"))
                        textUltimoPagoFecha.text = " ${formato.format(fecha)}"
                    }
                    textUltimoPagoEstatus.text = " Estado: Pagado"
                    textUltimoPagoEstatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                } else {
                    mostrarSinPagos()
                }
            }
            .addOnFailureListener {
                mostrarSinPagos()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            }
    }


    // ✅ FUNCIÓN para mostrar cuando no hay pagos
    private fun mostrarSinPagos() {
        textUltimoPagoConcepto.text = "No hay pagos registrados"
        textUltimoPagoMonto.text = " $0.00"
        textUltimoPagoFecha.text = " Sin pagos aún"
        textUltimoPagoEstatus.text = "⚪ Realiza tu primer pago"
        textUltimoPagoEstatus.setTextColor(android.graphics.Color.parseColor("#FF9800"))
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            textUserEmail.text = user.email

            // Obtener nombre de usuario de Firestore
            val docRef = db.collection("users").document(user.uid)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("username")
                        textWelcome.text = "Bienvenid@, $username"
                    } else {
                        textWelcome.text = "Bienvenid@"
                    }
                }
                .addOnFailureListener {
                    textWelcome.text = "Bienvenid@"
                }
        }
    }
}