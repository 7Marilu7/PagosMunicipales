package com.example.pagosmunicipales

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import com.example.pagosmunicipales.model.Pago
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class PagoMultasActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var editTextFolio: TextInputEditText
    private lateinit var btnConsultar: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var cardResultado: CardView
    private lateinit var textFolioResultado: TextView
    private lateinit var textInfractor: TextView
    private lateinit var textPlacas: TextView
    private lateinit var textConcepto: TextView
    private lateinit var textVehiculo: TextView
    private lateinit var textEstado: TextView
    private lateinit var textMontoPagar: TextView
    private lateinit var textFechaLimite: TextView
    private lateinit var btnPagar: MaterialButton

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var folioActual: String = ""
    private var datosMulta: MutableMap<String, Any>? = null
    private lateinit var textFechaExpirada: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pago_multas)

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Inicializar vistas
        toolbar = findViewById(R.id.toolbar)
        editTextFolio = findViewById(R.id.editTextFolio)
        btnConsultar = findViewById(R.id.btnConsultar)
        progressBar = findViewById(R.id.progressBar)
        cardResultado = findViewById(R.id.cardResultado)
        textFolioResultado = findViewById(R.id.textFolioResultado)
        textInfractor = findViewById(R.id.textInfractor)
        textPlacas = findViewById(R.id.textPlacas)
        textConcepto = findViewById(R.id.textConcepto)
        textVehiculo = findViewById(R.id.textVehiculo)
        textEstado = findViewById(R.id.textEstado)
        textMontoPagar = findViewById(R.id.textMontoPagar)
        textFechaLimite = findViewById(R.id.textFechaLimite)
        btnPagar = findViewById(R.id.btnPagar)
        textFechaExpirada = findViewById(R.id.textFechaExpirada)

        // Configurar toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Botón Consultar
        btnConsultar.setOnClickListener {
            val folio = editTextFolio.text.toString().trim()
            if (folio.isEmpty()) {
                editTextFolio.error = "Ingresa el folio de la multa"
                return@setOnClickListener
            }
            consultarMulta(folio)
        }

        // Botón Pagar
        btnPagar.setOnClickListener {
            mostrarDialogoMetodoPago()
        }
    }

    private fun consultarMulta(folio: String) {
        progressBar.visibility = View.VISIBLE
        btnConsultar.isEnabled = false
        cardResultado.visibility = View.GONE

        db.collection("multas")
            .whereEqualTo("folioMulta", folio)
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE
                btnConsultar.isEnabled = true

                if (documents.isEmpty) {
                    Toast.makeText(this, "❌ Folio de multa no encontrado", Toast.LENGTH_LONG).show()
                    cardResultado.visibility = View.GONE
                } else {
                    val documento = documents.first()
                    folioActual = documento.id
                    datosMulta = documento.data

                    // Extraer datos
                    val infractor = documento.getString("nombre") ?: "N/A"
                    val placas = documento.getString("placas") ?: "N/A"
                    val concepto = documento.getString("concepto") ?: "N/A"
                    val marcaModelo = documento.getString("marcaModelo") ?: "N/A"
                    val estado = documento.getString("estado") ?: "PENDIENTE"
                    val monto = documento.getDouble("monto") ?: 0.0
                    val fechaLimite = documento.getTimestamp("fechaLimite")

                    val formatter = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "MX"))

                    textFolioResultado.text = "Folio: $folio"
                    textInfractor.text = infractor
                    textPlacas.text = placas
                    textConcepto.text = concepto
                    textVehiculo.text = "$marcaModelo"
                    textEstado.text = estado

                    when (estado.uppercase()) {
                        "PENDIENTE" -> textEstado.setTextColor(
                            resources.getColor(
                                R.color.color_accent,
                                theme
                            )
                        )
                        "VENCIDO" -> textEstado.setTextColor(
                            resources.getColor(
                                R.color.color_secondary,
                                theme
                            )
                        )
                        "PAGADO" -> textEstado.setTextColor(
                            resources.getColor(
                                R.color.color_primary,
                                theme
                            )
                        )
                    }

                    textMontoPagar.text = formatter.format(monto)

                    // Mostrar fecha límite con hora
                    fechaLimite?.let {
                        val fechaHora = dateFormat.format(it.toDate())
                        textFechaLimite.text = "Válido hasta: $fechaHora"
                        textFechaLimite.visibility = View.VISIBLE
                    } ?: run {
                        textFechaLimite.visibility = View.GONE
                    }

                    // Validar estado y fecha límite
                    val estadoValido = estado.trim().uppercase() == "PENDIENTE"
                    val fechaValida = validarFechaLimite(fechaLimite)

                    btnPagar.isEnabled = estadoValido && fechaValida

                    // Mostrar mensaje si la fecha límite ya pasó
                    if (estadoValido && !fechaValida) {
                        textFechaExpirada.visibility = View.VISIBLE
                        textFechaLimite.setTextColor(Color.RED)
                    } else {
                        textFechaExpirada.visibility = View.GONE
                        textFechaLimite.setTextColor(resources.getColor(R.color.color_text_secondary, theme))
                    }

                    cardResultado.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnConsultar.isEnabled = true
                Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun validarFechaLimite(fechaLimite: com.google.firebase.Timestamp?): Boolean {
        if (fechaLimite == null) return true

        val fechaActual = Date()
        val fechaLimiteDate = fechaLimite.toDate()

        return fechaActual.before(fechaLimiteDate)
    }

    private fun mostrarDialogoMetodoPago() {
        val opciones = arrayOf("Efectivo", "Tarjeta de crédito/débito")

        AlertDialog.Builder(this)
            .setTitle("Selecciona método de pago")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> mostrarDialogoEfectivo()
                    1 -> mostrarDialogoTarjeta()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEfectivo() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pago_efectivo, null)
        val editMontoRecibido = dialogView.findViewById<TextInputEditText>(R.id.editMontoRecibido)
        val textTotalPagar = dialogView.findViewById<TextView>(R.id.textTotalPagar)
        val textCambio = dialogView.findViewById<TextView>(R.id.textCambio)
        val btnCalcularCambio = dialogView.findViewById<Button>(R.id.btnCalcularCambio)
        val btnConfirmarPago = dialogView.findViewById<Button>(R.id.btnConfirmarPago)
        val layoutCambio = dialogView.findViewById<LinearLayout>(R.id.layoutCambio)

        val montoTotal = datosMulta?.get("monto") as? Double ?: 0.0
        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        textTotalPagar.text = "Total a pagar: ${formatter.format(montoTotal)}"

        val dialog = AlertDialog.Builder(this)
            .setTitle("Pago en Efectivo")
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .create()

        btnCalcularCambio.setOnClickListener {
            val montoRecibidoStr = editMontoRecibido.text.toString()
            if (montoRecibidoStr.isEmpty()) {
                editMontoRecibido.error = "Ingresa el monto recibido"
                return@setOnClickListener
            }

            val montoRecibido = montoRecibidoStr.toDoubleOrNull() ?: 0.0
            if (montoRecibido < montoTotal) {
                Toast.makeText(this, "El monto recibido es insuficiente", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cambio = montoRecibido - montoTotal
            textCambio.text = "Cambio: ${formatter.format(cambio)}"
            layoutCambio.visibility = View.VISIBLE
            btnConfirmarPago.isEnabled = true
        }

        btnConfirmarPago.setOnClickListener {
            val montoRecibido = editMontoRecibido.text.toString().toDoubleOrNull() ?: 0.0
            val cambio = montoRecibido - montoTotal
            dialog.dismiss()
            procesarPagoEfectivo(montoRecibido, cambio)
        }

        dialog.show()
    }

    private fun mostrarDialogoTarjeta() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pago_tarjeta, null)
        val editNumeroTarjeta = dialogView.findViewById<TextInputEditText>(R.id.editNumeroTarjeta)
        val editNombreTitular = dialogView.findViewById<TextInputEditText>(R.id.editNombreTitular)
        val editMesExpiracion = dialogView.findViewById<TextInputEditText>(R.id.editMesExpiracion)
        val editAnioExpiracion = dialogView.findViewById<TextInputEditText>(R.id.editAnioExpiracion)
        val editCVV = dialogView.findViewById<TextInputEditText>(R.id.editCVV)
        val btnProcesarPago = dialogView.findViewById<Button>(R.id.btnProcesarPago)
        val progressBarTarjeta = dialogView.findViewById<ProgressBar>(R.id.progressBarTarjeta)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Pago con Tarjeta")
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .create()

        btnProcesarPago.setOnClickListener {
            val numeroTarjeta = editNumeroTarjeta.text.toString().replace(" ", "")
            val nombreTitular = editNombreTitular.text.toString()
            val mes = editMesExpiracion.text.toString()
            val anio = editAnioExpiracion.text.toString()
            val cvv = editCVV.text.toString()

            when {
                numeroTarjeta.length < 15 || numeroTarjeta.length > 16 -> {
                    editNumeroTarjeta.error = "Número de tarjeta inválido"
                }

                nombreTitular.isEmpty() -> {
                    editNombreTitular.error = "Ingresa el nombre del titular"
                }

                mes.toIntOrNull() !in 1..12 -> {
                    editMesExpiracion.error = "Mes inválido"
                }

                anio.length != 2 -> {
                    editAnioExpiracion.error = "Año inválido (2 dígitos)"
                }

                cvv.length !in 3..4 -> {
                    editCVV.error = "CVV inválido"
                }

                else -> {
                    btnProcesarPago.isEnabled = false
                    progressBarTarjeta.visibility = View.VISIBLE

                    btnProcesarPago.postDelayed({
                        progressBarTarjeta.visibility = View.GONE
                        dialog.dismiss()

                        val tipoTarjeta = when (numeroTarjeta.first()) {
                            '4' -> "Visa"
                            '5' -> "MasterCard"
                            '3' -> "American Express"
                            else -> "Tarjeta"
                        }

                        val tarjetaEnmascarada = "**** **** **** ${numeroTarjeta.takeLast(4)}"
                        procesarPagoTarjeta(tipoTarjeta, tarjetaEnmascarada)
                    }, 2000)
                }
            }
        }

        dialog.show()
    }

    private fun procesarPagoEfectivo(montoRecibido: Double, cambio: Double) {
        progressBar.visibility = View.VISIBLE
        btnPagar.isEnabled = false

        val montoTotal = datosMulta?.get("monto") as? Double ?: 0.0
        val folio = editTextFolio.text.toString().trim()
        val usuarioActual = auth.currentUser?.email ?: "usuario_desconocido"
        val infractor = datosMulta?.get("nombre") as? String ?: "N/A"
        val placas = datosMulta?.get("placas") as? String ?: "N/A"
        val concepto = datosMulta?.get("concepto") as? String ?: "N/A"

        val pago = hashMapOf(
            "tipoPagoId" to 1,
            "folio" to folio,
            "monto" to montoTotal,
            "metodoPago" to "Efectivo",
            "efectivoRecibido" to montoRecibido,
            "cambio" to cambio,
            "fechaPago" to System.currentTimeMillis(),
            "usuario" to usuarioActual,
            "concepto" to concepto,
            "infractor" to infractor,
            "placas" to placas,
            "timestamp" to Timestamp.now()
        )

        db.collection("PAGOS")
            .add(pago)
            .addOnSuccessListener { documentReference ->
                actualizarEstadoMulta(folio, documentReference.id)
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnPagar.isEnabled = true
                Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun procesarPagoTarjeta(tipoTarjeta: String, tarjetaEnmascarada: String) {
        progressBar.visibility = View.VISIBLE
        btnPagar.isEnabled = false

        val montoTotal = datosMulta?.get("monto") as? Double ?: 0.0
        val folio = editTextFolio.text.toString().trim()
        val usuarioActual = auth.currentUser?.email ?: "usuario_desconocido"
        val infractor = datosMulta?.get("nombre") as? String ?: "N/A"
        val placas = datosMulta?.get("placas") as? String ?: "N/A"
        val concepto = datosMulta?.get("concepto") as? String ?: "N/A"

        val pago = hashMapOf(
            "tipoPagoId" to 1,
            "folio" to folio,
            "monto" to montoTotal,
            "metodoPago" to "$tipoTarjeta ($tarjetaEnmascarada)",
            "efectivoRecibido" to 0.0,
            "cambio" to 0.0,
            "fechaPago" to System.currentTimeMillis(),
            "usuario" to usuarioActual,
            "concepto" to concepto,
            "infractor" to infractor,
            "placas" to placas,
            "timestamp" to Timestamp.now()
        )

        db.collection("PAGOS")
            .add(pago)
            .addOnSuccessListener { documentReference ->
                actualizarEstadoMulta(folio, documentReference.id)
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnPagar.isEnabled = true
                Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun actualizarEstadoMulta(folio: String, pagoId: String) {
        db.collection("multas")
            .whereEqualTo("folioMulta", folio)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val documento = documents.first()
                    documento.reference.update(
                        mapOf(
                            "estado" to "PAGADO",
                            "fechaPago" to Timestamp.now(),
                            "pagoId" to pagoId
                        )
                    ).addOnSuccessListener {
                        obtenerPagoYMostrarComprobante(pagoId)
                    }
                }
            }
    }

    private fun obtenerPagoYMostrarComprobante(pagoId: String) {
        db.collection("PAGOS").document(pagoId)
            .get()
            .addOnSuccessListener { documento ->
                if (documento.exists()) {
                    val pago: MutableMap<String, Any> = mutableMapOf(
                        "id" to documento.id,
                        "folio" to (documento.getString("folio") ?: ""),
                        "monto" to (documento.getDouble("monto") ?: 0.0),
                        "metodoPago" to (documento.getString("metodoPago") ?: ""),
                        "efectivoRecibido" to (documento.getDouble("efectivoRecibido") ?: 0.0),
                        "cambio" to (documento.getDouble("cambio") ?: 0.0),
                        "fechaPago" to (documento.getLong("fechaPago")
                            ?: System.currentTimeMillis()),
                        "concepto" to (documento.getString("concepto") ?: ""),
                        "infractor" to (documento.getString("infractor") ?: ""),
                        "placas" to (documento.getString("placas") ?: "")
                    )
                    mostrarDialogoComprobante(pago)
                }
            }
    }

    private fun mostrarDialogoComprobante(pago: MutableMap<String, Any>) {
        progressBar.visibility = View.GONE

        val dialogView = layoutInflater.inflate(R.layout.dialog_comprobante, null)
        val textFolio = dialogView.findViewById<TextView>(R.id.textFolio)
        val textFecha = dialogView.findViewById<TextView>(R.id.textFecha)
        val textConcepto = dialogView.findViewById<TextView>(R.id.textConcepto)
        val textInfractor = dialogView.findViewById<TextView>(R.id.textInfractor)
        val textPlacas = dialogView.findViewById<TextView>(R.id.textPlacas)
        val textMonto = dialogView.findViewById<TextView>(R.id.textMonto)
        val textMetodoPago = dialogView.findViewById<TextView>(R.id.textMetodoPago)
        val textRecibido = dialogView.findViewById<TextView>(R.id.textRecibido)
        val textCambio = dialogView.findViewById<TextView>(R.id.textCambio)
        val btnDescargar = dialogView.findViewById<Button>(R.id.btnDescargarComprobante)
        val btnCerrar = dialogView.findViewById<Button>(R.id.btnCerrar)

        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("es", "MX"))

        textFolio.text = "Folio: ${pago["folio"]}"
        textFecha.text = "Fecha: ${dateFormat.format(Date(pago["fechaPago"] as Long))}"
        textConcepto.text = "Concepto: ${pago["concepto"]}"
        textInfractor.text = "Infractor: ${pago["infractor"]}"
        textPlacas.text = "Placas: ${pago["placas"]}"
        textMonto.text = "Total: ${formatter.format(pago["monto"])}"
        textMetodoPago.text = "Método: ${pago["metodoPago"]}"

        if ((pago["metodoPago"] as String).startsWith("Efectivo")) {
            textRecibido.text = "Recibido: ${formatter.format(pago["efectivoRecibido"] as Double)}"
            textCambio.text = "Cambio: ${formatter.format(pago["cambio"] as Double)}"
            textRecibido.visibility = View.VISIBLE
            textCambio.visibility = View.VISIBLE
        } else {
            textRecibido.visibility = View.GONE
            textCambio.visibility = View.GONE
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("✅ Pago Exitoso")
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnDescargar.setOnClickListener {
            generarPDF(pago)
        }

        btnCerrar.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun generarPDF(pago: Map<String, Any>) {
        try {
            // Crear nombre del archivo
            val fileName = "Comprobante_Multa_${pago["folio"]}_${System.currentTimeMillis()}.pdf"

            // Crear documento PDF - tamaño carta
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Tamaño carta en puntos
            val page = document.startPage(pageInfo)

            val canvas: Canvas = page.canvas
            val paint = Paint()
            val titlePaint = Paint()
            val headerPaint = Paint()
            val labelPaint = Paint()
            val valuePaint = Paint()
            val borderPaint = Paint()

            // Configurar colores
            val colorVino = Color.rgb(139, 30, 63)      // #8B1E3F
            val colorDorado = Color.rgb(255, 215, 0)    // #FFD700
            val colorGrisClaro = Color.rgb(245, 245, 245) // #F5F5F5

            // Configurar estilos de texto
            paint.color = Color.BLACK
            paint.textSize = 12f

            titlePaint.color = colorVino
            titlePaint.textSize = 24f
            titlePaint.isFakeBoldText = true

            headerPaint.color = colorVino
            headerPaint.textSize = 16f
            headerPaint.isFakeBoldText = true

            labelPaint.color = Color.DKGRAY
            labelPaint.textSize = 10f

            valuePaint.color = Color.BLACK
            valuePaint.textSize = 12f
            valuePaint.isFakeBoldText = true

            borderPaint.color = colorDorado
            borderPaint.style = Paint.Style.STROKE
            borderPaint.strokeWidth = 2f

            // ===== ENCABEZADO =====
            // Rectángulo superior color vino
            paint.color = colorVino
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, 595f, 80f, paint)

            // Texto del encabezado
            paint.color = Color.WHITE
            paint.textSize = 20f
            paint.isFakeBoldText = true
            canvas.drawText("COMPROBANTE DE PAGO", 40f, 45f, paint)

            paint.textSize = 14f
            paint.isFakeBoldText = false
            canvas.drawText("MULTAS", 40f, 70f, paint)

            // ===== LÍNEA DORADA =====
            paint.color = colorDorado
            paint.style = Paint.Style.FILL
            canvas.drawRect(40f, 90f, 555f, 92f, paint)

            // ===== INFO SUPERIOR (estilo PSE) =====
            var yPos = 120f

            // Monto grande como en PSE
            titlePaint.color = colorVino
            titlePaint.textSize = 32f
            val formatter = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            val montoStr = formatter.format(pago["monto"])
            canvas.drawText(montoStr, 40f, yPos, titlePaint)
            yPos += 40f

            // ===== CUADRO DE INFORMACIÓN =====
            // Fondo gris claro
            paint.color = colorGrisClaro
            paint.style = Paint.Style.FILL
            canvas.drawRect(40f, yPos, 555f, yPos + 200f, paint)

            // Borde dorado
            canvas.drawRect(40f, yPos, 555f, yPos + 200f, borderPaint)

            // Información dentro del cuadro
            var innerY = yPos + 25f
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "MX"))

            // ID de transacción
            labelPaint.textSize = 10f
            canvas.drawText("ID DE TRANSACCIÓN:", 60f, innerY, labelPaint)
            innerY += 15f
            valuePaint.textSize = 12f
            canvas.drawText("${pago["id"]}", 60f, innerY, valuePaint)
            innerY += 25f

            // Referencia / Folio
            labelPaint.textSize = 10f
            canvas.drawText("REFERENCIA (FOLIO):", 60f, innerY, labelPaint)
            innerY += 15f
            valuePaint.textSize = 12f
            canvas.drawText("${pago["folio"]}", 60f, innerY, valuePaint)
            innerY += 25f

            // Método de pago
            labelPaint.textSize = 10f
            canvas.drawText("MÉTODO DE PAGO:", 60f, innerY, labelPaint)
            innerY += 15f
            valuePaint.textSize = 12f
            canvas.drawText("${pago["metodoPago"]}", 60f, innerY, valuePaint)
            innerY += 25f

            // Fecha
            labelPaint.textSize = 10f
            canvas.drawText("FECHA DE TRANSACCIÓN:", 60f, innerY, labelPaint)
            innerY += 15f
            valuePaint.textSize = 12f
            canvas.drawText("${dateFormat.format(Date(pago["fechaPago"] as Long))}", 60f, innerY, valuePaint)

            yPos += 220f

            // ===== TABLA DE DETALLES (estilo PSE) =====
            // Encabezado de tabla
            paint.color = colorVino
            paint.style = Paint.Style.FILL
            canvas.drawRect(40f, yPos, 555f, yPos + 25f, paint)

            paint.color = Color.WHITE
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("DETALLE DE LA MULTA", 60f, yPos + 17f, paint)

            yPos += 35f

            // Filas de la tabla
            val detalles = listOf(
                Pair("CONCEPTO", pago["concepto"] as String),
                Pair("INFRACTOR", pago["infractor"] as String),
                Pair("PLACAS", pago["placas"] as String)
            )

            for ((index, detalle) in detalles.withIndex()) {
                // Fondo gris claro para filas pares
                if (index % 2 == 0) {
                    paint.color = colorGrisClaro
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(40f, yPos, 555f, yPos + 25f, paint)
                }

                // Borde inferior dorado
                borderPaint.strokeWidth = 1f
                canvas.drawLine(40f, yPos + 25f, 555f, yPos + 25f, borderPaint)

                // Texto de la fila
                paint.color = Color.BLACK
                paint.textSize = 11f
                paint.isFakeBoldText = true
                canvas.drawText(detalle.first, 60f, yPos + 17f, paint)

                paint.isFakeBoldText = false
                canvas.drawText(detalle.second, 200f, yPos + 17f, paint)

                yPos += 25f
            }

            yPos += 10f

            // ===== LÍNEA DE TOTAL =====
            // Fondo vino para el total
            paint.color = colorVino
            paint.style = Paint.Style.FILL
            canvas.drawRect(40f, yPos, 555f, yPos + 40f, paint)

            paint.color = Color.WHITE
            paint.textSize = 14f
            paint.isFakeBoldText = true
            canvas.drawText("TOTAL PAGADO:", 60f, yPos + 25f, paint)

            paint.textSize = 18f
            canvas.drawText(montoStr, 350f, yPos + 25f, paint)

            yPos += 55f

            // ===== PIE DE PÁGINA =====
            paint.color = colorDorado
            paint.style = Paint.Style.FILL
            canvas.drawRect(40f, 790f, 555f, 792f, paint)

            paint.color = Color.DKGRAY
            paint.textSize = 8f
            paint.isFakeBoldText = false
            canvas.drawText("Este documento es un comprobante de pago oficial", 40f, 815f, paint)
            canvas.drawText("H. Ayuntamiento de Tlalnepantla de Baz - Sistema de Pagos en Línea", 40f, 830f, paint)

            document.finishPage(page)

            // Guardar el PDF (igual que antes)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        document.writeTo(outputStream)
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                document.writeTo(FileOutputStream(file))

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    mediaScanIntent.data = Uri.fromFile(file)
                    sendBroadcast(mediaScanIntent)
                }
            }

            document.close()

            runOnUiThread {
                Toast.makeText(this, "✅ PDF guardado en Descargas", Toast.LENGTH_LONG).show()
                mostrarMensajePDFGuardado()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarMensajePDFGuardado() {
        AlertDialog.Builder(this)
            .setTitle("✅ Comprobante Generado")
            .setMessage("El comprobante se guardó correctamente en tu carpeta DESCARGAS.\n\n" +
                    "📁 Para verlo:\n" +
                    "1. Abre la app 'Archivos' o 'My Files'\n" +
                    "2. Ve a 'Descargas'\n" +
                    "3. Toca el archivo PDF")
            .setPositiveButton("Entendido", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}