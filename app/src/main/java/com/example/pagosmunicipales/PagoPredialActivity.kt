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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class PagoPredialActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var editTextClave: TextInputEditText
    private lateinit var btnConsultar: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var cardResultado: CardView
    private lateinit var textClaveResultado: TextView
    private lateinit var textPropietario: TextView
    private lateinit var textValorCatastral: TextView
    private lateinit var textDireccion: TextView
    private lateinit var textSuperficie: TextView
    private lateinit var textMontoPagar: TextView
    private lateinit var toggleGroupAnio: MaterialButtonToggleGroup
    private lateinit var btnPagar: MaterialButton
    private lateinit var btn2024: MaterialButton
    private lateinit var btn2025: MaterialButton
    private lateinit var btn2026: MaterialButton

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var claveActual: String = ""
    private var datosPredio: MutableMap<String, Any>? = null
    private var montos: Map<String, Double> = mapOf()
    private var aniosSeleccionados = mutableListOf<String>()
    private val colorVino = "#8B1E3F"
    private val colorDorado = "#FFD700"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pago_predial)

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Inicializar vistas
        toolbar = findViewById(R.id.toolbar)
        editTextClave = findViewById(R.id.editTextClave)
        btnConsultar = findViewById(R.id.btnConsultar)
        progressBar = findViewById(R.id.progressBar)
        cardResultado = findViewById(R.id.cardResultado)
        textClaveResultado = findViewById(R.id.textClaveResultado)
        textPropietario = findViewById(R.id.textPropietario)
        textValorCatastral = findViewById(R.id.textValorCatastral)
        textDireccion = findViewById(R.id.textDireccion)
        textSuperficie = findViewById(R.id.textSuperficie)
        textMontoPagar = findViewById(R.id.textMontoPagar)
        toggleGroupAnio = findViewById(R.id.toggleGroupAnio)
        btnPagar = findViewById(R.id.btnPagar)
        btn2024 = findViewById(R.id.btnAnio2024)
        btn2025 = findViewById(R.id.btnAnio2025)
        btn2026 = findViewById(R.id.btnAnio2026)

        // Configurar toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Configurar selección múltiple
        toggleGroupAnio.isSingleSelection = false

        // ===== NUEVO: Listener para cambio visual de botones =====
        toggleGroupAnio.addOnButtonCheckedListener { group, checkedId, isChecked ->
            val button = findViewById<MaterialButton>(checkedId)

            // ===== AGREGAR ESTAS LÍNEAS PARA ACTUALIZAR aniosSeleccionados =====
            val anio = when (checkedId) {
                R.id.btnAnio2024 -> "2024"
                R.id.btnAnio2025 -> "2025"
                R.id.btnAnio2026 -> "2026"
                else -> ""
            }

            if (isChecked) {
                aniosSeleccionados.add(anio)

                // ESTADO SELECCIONADO: Fondo vino + texto blanco + check dorado
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    Color.parseColor(colorVino)
                )
                button.setTextColor(Color.WHITE)
                button.strokeWidth = 0
                button.setIconResource(R.drawable.ic_check)
                button.iconTint = android.content.res.ColorStateList.valueOf(
                    Color.parseColor(colorDorado)
                )
                button.iconPadding = 8
                button.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_END
            } else {
                aniosSeleccionados.remove(anio)

                // ESTADO NO SELECCIONADO: Fondo blanco + texto vino + borde dorado
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    Color.WHITE
                )
                button.setTextColor(Color.parseColor(colorVino))
                button.strokeWidth = 2
                button.strokeColor = android.content.res.ColorStateList.valueOf(
                    Color.parseColor(colorDorado)
                )
                button.setIconResource(0)
            }

            actualizarMontoTotal()
        }

        // Botón Consultar
        btnConsultar.setOnClickListener {
            val clave = editTextClave.text.toString().trim()
            if (clave.isEmpty()) {
                editTextClave.error = "Ingresa una clave catastral"
                return@setOnClickListener
            }
            consultarPredio(clave)
        }

        // Botón Pagar
        btnPagar.setOnClickListener {
            if (aniosSeleccionados.isEmpty()) {
                Toast.makeText(this, "Selecciona al menos un año para pagar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mostrarDialogoMetodoPago()
        }
    }

    private fun consultarPredio(clave: String) {
        progressBar.visibility = View.VISIBLE
        btnConsultar.isEnabled = false
        cardResultado.visibility = View.GONE
        aniosSeleccionados.clear()

        db.collection("predios")
            .whereEqualTo("claveCatastral", clave)
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE
                btnConsultar.isEnabled = true

                if (documents.isEmpty) {
                    Toast.makeText(this, "❌ Clave catastral no encontrada", Toast.LENGTH_LONG).show()
                    cardResultado.visibility = View.GONE
                } else {
                    val documento = documents.first()
                    claveActual = documento.id
                    datosPredio = documento.data

                    // Extraer datos
                    val propietario = documento.getString("propietario") ?: "N/A"
                    val valorCatastral = documento.getDouble("valorCatastral") ?: 0.0
                    val direccion = documento.getString("direccion") ?: "N/A"
                    val superficie = documento.getString("superficie") ?: "N/A"

                    // ===== GUARDAR MONTOS EN EL MAPA =====
                    val monto2024 = documento.getDouble("adeudo2024") ?: 0.0
                    val monto2025 = documento.getDouble("adeudo2025") ?: 0.0
                    val monto2026 = documento.getDouble("adeudo2026") ?: 0.0

                    montos = mapOf(
                        "2024" to monto2024,
                        "2025" to monto2025,
                        "2026" to monto2026
                    )

                    // Formateador de moneda
                    val formatter = NumberFormat.getCurrencyInstance(Locale("es", "MX"))

                    // ===== 1. PRIMERO: Asignar textos a los botones =====
                    btn2024.text = "2024\n${formatter.format(monto2024)}"
                    btn2025.text = "2025\n${formatter.format(monto2025)}"
                    btn2026.text = "2026\n${formatter.format(monto2026)}"

                    // ===== 2. SEGUNDO: Deshabilitar botones con monto 0 =====
                    btn2024.isEnabled = monto2024 > 0
                    btn2025.isEnabled = monto2025 > 0
                    btn2026.isEnabled = monto2026 > 0

                    // ===== 3. TERCERO: Mostrar datos generales =====
                    textClaveResultado.text = "Clave: $clave"
                    textPropietario.text = propietario
                    textValorCatastral.text = formatter.format(valorCatastral)
                    textDireccion.text = direccion
                    textSuperficie.text = superficie

                    // ===== 4. CUARTO: Limpiar selecciones y resetear estilos =====
                    toggleGroupAnio.clearChecked()

                    // Resetear estilos SOLO de botones habilitados
                    val botones = listOf(btn2024, btn2025, btn2026)
                    for (button in botones) {
                        if (button.isEnabled) {
                            button.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
                            button.setTextColor(Color.parseColor(colorVino))
                            button.strokeWidth = 2
                            button.strokeColor = android.content.res.ColorStateList.valueOf(
                                Color.parseColor(colorDorado)
                            )
                            button.setIconResource(0)
                        } else {
                            button.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.LTGRAY)
                            button.setTextColor(Color.GRAY)
                            button.strokeWidth = 1
                            button.strokeColor = android.content.res.ColorStateList.valueOf(Color.GRAY)
                            button.setIconResource(0)
                        }
                    }

                    // Mostrar card
                    cardResultado.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnConsultar.isEnabled = true
                Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun actualizarMontoTotal() {
        var total = 0.0
        for (anio in aniosSeleccionados) {
            total += montos[anio] ?: 0.0  // AHORA montos SÍ tiene valores
        }

        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        textMontoPagar.text = formatter.format(total)
        btnPagar.isEnabled = total > 0
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
        // Obtener monto total de años seleccionados
        var montoTotal = 0.0
        for (anio in aniosSeleccionados) {
            montoTotal += montos[anio] ?: 0.0
        }

        if (montoTotal <= 0) {
            Toast.makeText(this, "Error: Monto a pagar no válido", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_pago_efectivo, null)
        val editMontoRecibido = dialogView.findViewById<TextInputEditText>(R.id.editMontoRecibido)
        val textTotalPagar = dialogView.findViewById<TextView>(R.id.textTotalPagar)
        val textCambio = dialogView.findViewById<TextView>(R.id.textCambio)
        val btnCalcularCambio = dialogView.findViewById<Button>(R.id.btnCalcularCambio)
        val btnConfirmarPago = dialogView.findViewById<Button>(R.id.btnConfirmarPago)
        val layoutCambio = dialogView.findViewById<LinearLayout>(R.id.layoutCambio)

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
            procesarPagoEfectivo(montoRecibido, cambio, montoTotal)
        }

        dialog.show()
    }

    private fun mostrarDialogoTarjeta() {
        // Obtener monto total de años seleccionados
        var montoTotal = 0.0
        for (anio in aniosSeleccionados) {
            montoTotal += montos[anio] ?: 0.0
        }

        if (montoTotal <= 0) {
            Toast.makeText(this, "Error: Monto a pagar no válido", Toast.LENGTH_LONG).show()
            return
        }

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
                        procesarPagoTarjeta(tipoTarjeta, tarjetaEnmascarada, montoTotal)
                    }, 2000)
                }
            }
        }

        dialog.show()
    }

    private fun procesarPagoEfectivo(montoRecibido: Double, cambio: Double, montoTotal: Double) {
        progressBar.visibility = View.VISIBLE
        btnPagar.isEnabled = false

        val clave = editTextClave.text.toString().trim()
        val usuarioActual = auth.currentUser?.email ?: "usuario_desconocido"
        val propietario = datosPredio?.get("propietario") as? String ?: "N/A"
        val anios = aniosSeleccionados.joinToString(", ")
        val concepto = "Pago predial - Años: $anios"

        val pago = hashMapOf(
            "tipoPagoId" to 3,  // 3 = Predial
            "folio" to clave,
            "monto" to montoTotal,
            "metodoPago" to "Efectivo",
            "efectivoRecibido" to montoRecibido,
            "cambio" to cambio,
            "fechaPago" to System.currentTimeMillis(),
            "usuario" to usuarioActual,
            "concepto" to concepto,
            "infractor" to propietario,
            "placas" to "N/A",
            "timestamp" to Timestamp.now()
        )

        db.collection("PAGOS")
            .add(pago)
            .addOnSuccessListener { documentReference ->
                actualizarAdeudosPredio(clave, documentReference.id)
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnPagar.isEnabled = true
                Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun procesarPagoTarjeta(tipoTarjeta: String, tarjetaEnmascarada: String, montoTotal: Double) {
        progressBar.visibility = View.VISIBLE
        btnPagar.isEnabled = false

        val clave = editTextClave.text.toString().trim()
        val usuarioActual = auth.currentUser?.email ?: "usuario_desconocido"
        val propietario = datosPredio?.get("propietario") as? String ?: "N/A"
        val anios = aniosSeleccionados.joinToString(", ")
        val concepto = "Pago predial - Años: $anios"

        val pago = hashMapOf(
            "tipoPagoId" to 3,  // 3 = Predial
            "folio" to clave,
            "monto" to montoTotal,
            "metodoPago" to "$tipoTarjeta ($tarjetaEnmascarada)",
            "efectivoRecibido" to 0.0,
            "cambio" to 0.0,
            "fechaPago" to System.currentTimeMillis(),
            "usuario" to usuarioActual,
            "concepto" to concepto,
            "infractor" to propietario,
            "placas" to "N/A",
            "timestamp" to Timestamp.now()
        )

        db.collection("PAGOS")
            .add(pago)
            .addOnSuccessListener { documentReference ->
                actualizarAdeudosPredio(clave, documentReference.id)
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnPagar.isEnabled = true
                Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun actualizarAdeudosPredio(clave: String, pagoId: String) {
        // Crear un mapa SOLO con los años pagados para ponerlos en 0
        val updates = mutableMapOf<String, Any>()
        for (anio in aniosSeleccionados) {
            when (anio) {
                "2024" -> updates["adeudo2024"] = 0.0
                "2025" -> updates["adeudo2025"] = 0.0
                "2026" -> updates["adeudo2026"] = 0.0
            }
        }

        db.collection("predios")
            .whereEqualTo("claveCatastral", clave)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val documento = documents.first()
                    documento.reference.update(updates)  // SOLO actualiza los años
                        .addOnSuccessListener {
                            // Después de actualizar, obtenemos el pago para mostrar comprobante
                            obtenerPagoYMostrarComprobante(pagoId)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "❌ Error al actualizar adeudos: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        "fechaPago" to (documento.getLong("fechaPago") ?: System.currentTimeMillis()),
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

        textFolio.text = "Clave: ${pago["folio"]}"
        textFecha.text = "Fecha: ${dateFormat.format(Date(pago["fechaPago"] as Long))}"
        textConcepto.text = "Concepto: ${pago["concepto"]}"
        textInfractor.text = "Propietario: ${pago["infractor"]}"
        textPlacas.visibility = View.GONE
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
            val fileName = "Comprobante_Predial_${pago["folio"]}_${System.currentTimeMillis()}.pdf"

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)

            val canvas: Canvas = page.canvas
            val paint = Paint()
            val titlePaint = Paint()
            val labelPaint = Paint()
            val valuePaint = Paint()
            val borderPaint = Paint()

            val colorVino = Color.rgb(139, 30, 63)
            val colorDorado = Color.rgb(255, 215, 0)
            val colorGrisClaro = Color.rgb(245, 245, 245)

            paint.color = Color.BLACK
            paint.textSize = 12f

            titlePaint.color = colorVino
            titlePaint.textSize = 24f
            titlePaint.isFakeBoldText = true

            labelPaint.color = Color.DKGRAY
            labelPaint.textSize = 10f

            valuePaint.color = Color.BLACK
            valuePaint.textSize = 12f
            valuePaint.isFakeBoldText = true

            borderPaint.color = colorDorado
            borderPaint.style = Paint.Style.STROKE
            borderPaint.strokeWidth = 2f

            // ===== ENCABEZADO =====
            paint.color = colorVino
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, 595f, 80f, paint)

            paint.color = Color.WHITE
            paint.textSize = 20f
            paint.isFakeBoldText = true
            canvas.drawText("COMPROBANTE DE PAGO", 40f, 45f, paint)

            paint.textSize = 14f
            paint.isFakeBoldText = false
            canvas.drawText("PREDIAL", 40f, 70f, paint)

            // ===== LÍNEA DORADA =====
            paint.color = colorDorado
            paint.style = Paint.Style.FILL
            canvas.drawRect(40f, 90f, 555f, 92f, paint)

            var yPos = 120f

            titlePaint.color = colorVino
            titlePaint.textSize = 32f
            val formatter = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            val montoStr = formatter.format(pago["monto"])
            canvas.drawText(montoStr, 40f, yPos, titlePaint)
            yPos += 40f

            // ===== CUADRO DE INFORMACIÓN =====
            paint.color = colorGrisClaro
            paint.style = Paint.Style.FILL
            canvas.drawRect(40f, yPos, 555f, yPos + 200f, paint)
            canvas.drawRect(40f, yPos, 555f, yPos + 200f, borderPaint)

            var innerY = yPos + 25f
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "MX"))

            labelPaint.textSize = 10f
            canvas.drawText("ID DE TRANSACCIÓN:", 60f, innerY, labelPaint)
            innerY += 15f
            valuePaint.textSize = 12f
            canvas.drawText("${pago["id"]}", 60f, innerY, valuePaint)
            innerY += 25f

            canvas.drawText("CLAVE CATASTRAL:", 60f, innerY, labelPaint)
            innerY += 15f
            canvas.drawText("${pago["folio"]}", 60f, innerY, valuePaint)
            innerY += 25f

            canvas.drawText("MÉTODO DE PAGO:", 60f, innerY, labelPaint)
            innerY += 15f
            canvas.drawText("${pago["metodoPago"]}", 60f, innerY, valuePaint)
            innerY += 25f

            canvas.drawText("FECHA:", 60f, innerY, labelPaint)
            innerY += 15f
            canvas.drawText("${dateFormat.format(Date(pago["fechaPago"] as Long))}", 60f, innerY, valuePaint)

            yPos += 220f

            // ===== TABLA DE DETALLES =====
            paint.color = colorVino
            paint.style = Paint.Style.FILL
            canvas.drawRect(40f, yPos, 555f, yPos + 25f, paint)

            paint.color = Color.WHITE
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("DETALLE DEL PAGO", 60f, yPos + 17f, paint)

            yPos += 35f

            val detalles = listOf(
                Pair("PROPIETARIO", pago["infractor"] as String),
                Pair("CONCEPTO", pago["concepto"] as String)
            )

            for ((index, detalle) in detalles.withIndex()) {
                if (index % 2 == 0) {
                    paint.color = colorGrisClaro
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(40f, yPos, 555f, yPos + 25f, paint)
                }

                borderPaint.strokeWidth = 1f
                canvas.drawLine(40f, yPos + 25f, 555f, yPos + 25f, borderPaint)

                paint.color = Color.BLACK
                paint.textSize = 11f
                paint.isFakeBoldText = true
                canvas.drawText(detalle.first, 60f, yPos + 17f, paint)

                paint.isFakeBoldText = false
                canvas.drawText(detalle.second, 200f, yPos + 17f, paint)

                yPos += 25f
            }

            yPos += 10f

            // ===== TOTAL =====
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

            // Guardar PDF
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