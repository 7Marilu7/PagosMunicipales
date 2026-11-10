package com.example.pagosmunicipales

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AlertDialog

class HistoryActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: LinearLayout
    private lateinit var btnVolver: MaterialButton

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: PagosAdapter
    private val pagosList = mutableListOf<PagoItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Inicializar vistas
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        btnVolver = findViewById(R.id.btnVolver)

        // Configurar toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PagosAdapter(pagosList) { pago ->
            mostrarDetallePago(pago)
        }
        recyclerView.adapter = adapter

        // Botón Volver
        btnVolver.setOnClickListener {
            finish()
        }

        // Cargar pagos
        cargarHistorialPagos()
    }

    private fun cargarHistorialPagos() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
        recyclerView.visibility = View.GONE

        db.collection("PAGOS")
            .whereEqualTo("usuario", currentUser.email)
            .orderBy("fechaPago", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE
                pagosList.clear()

                if (documents.isEmpty) {
                    tvEmptyState.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    tvEmptyState.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE

                    for (document in documents) {
                        val pago = PagoItem(
                            id = document.id,
                            tipoPagoId = document.getLong("tipoPagoId")?.toInt() ?: 0,
                            folio = document.getString("folio") ?: "",
                            monto = document.getDouble("monto") ?: 0.0,
                            metodoPago = document.getString("metodoPago") ?: "",
                            fechaPago = document.getLong("fechaPago") ?: 0,
                            concepto = document.getString("concepto") ?: "",
                            infractor = document.getString("infractor") ?: "",
                            placas = document.getString("placas") ?: ""
                        )
                        pagosList.add(pago)
                    }
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al cargar historial: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun mostrarDetallePago(pago: PagoItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_detalle_pago, null)

        val tvTitulo = dialogView.findViewById<TextView>(R.id.tvTitulo)
        val tvFecha = dialogView.findViewById<TextView>(R.id.tvFecha)
        val tvFolio = dialogView.findViewById<TextView>(R.id.tvFolio)
        val tvConcepto = dialogView.findViewById<TextView>(R.id.tvConcepto)
        val tvInfractor = dialogView.findViewById<TextView>(R.id.tvInfractor)
        val tvPlacas = dialogView.findViewById<TextView>(R.id.tvPlacas)
        val tvMonto = dialogView.findViewById<TextView>(R.id.tvMonto)
        val tvMetodo = dialogView.findViewById<TextView>(R.id.tvMetodo)
        val btnCerrar = dialogView.findViewById<Button>(R.id.btnCerrar)

        // Formateadores
        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("es", "MX"))

        // Título según tipo
        tvTitulo.text = when (pago.tipoPagoId) {
            1 -> "Detalle de Multa"
            2 -> "Detalle de Licencia"
            3 -> "Detalle de Predial"
            else -> "Detalle de Pago"
        }

        // Datos comunes
        tvFecha.text = "Fecha: ${dateFormat.format(Date(pago.fechaPago))}"
        tvFolio.text = "Folio: ${pago.folio}"
        tvConcepto.text = "Concepto: ${pago.concepto}"
        tvInfractor.text = when (pago.tipoPagoId) {
            1 -> "Infractor: ${pago.infractor}"
            2 -> "Titular: ${pago.infractor}"
            3 -> "Propietario: ${pago.infractor}"
            else -> "Nombre: ${pago.infractor}"
        }

        // Mostrar placas solo si es multa y no es N/A
        if (pago.tipoPagoId == 1 && pago.placas != "N/A") {
            tvPlacas.text = "Placas: ${pago.placas}"
            tvPlacas.visibility = View.VISIBLE
        } else {
            tvPlacas.visibility = View.GONE
        }

        tvMonto.text = "Monto: ${formatter.format(pago.monto)}"
        tvMetodo.text = "Método: ${pago.metodoPago}"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCerrar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // Data class para los items del historial
    data class PagoItem(
        val id: String,
        val tipoPagoId: Int,
        val folio: String,
        val monto: Double,
        val metodoPago: String,
        val fechaPago: Long,
        val concepto: String,
        val infractor: String,
        val placas: String
    )

    // Adaptador para el RecyclerView
    class PagosAdapter(
        private val pagos: List<PagoItem>,
        private val onItemClick: (PagoItem) -> Unit
    ) : RecyclerView.Adapter<PagosAdapter.PagoViewHolder>() {

        class PagoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val cardPago: CardView = itemView.findViewById(R.id.cardPago)
            val tvTipo: TextView = itemView.findViewById(R.id.tvTipo)
            val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
            val tvMonto: TextView = itemView.findViewById(R.id.tvMonto)
            val tvConcepto: TextView = itemView.findViewById(R.id.tvConcepto)
            val ivIcono: ImageView = itemView.findViewById(R.id.ivIcono)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pago, parent, false)
            return PagoViewHolder(view)
        }

        override fun onBindViewHolder(holder: PagoViewHolder, position: Int) {
            val pago = pagos[position]
            val context = holder.itemView.context

            // Formateadores
            val formatter = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "MX"))

            // Tipo de pago
            holder.tvTipo.text = when (pago.tipoPagoId) {
                1 -> "MULTA"
                2 -> "LICENCIA"
                3 -> "PREDIAL"
                else -> "PAGO"
            }

            // Icono según tipo
            val iconRes = when (pago.tipoPagoId) {
                1 -> R.drawable.ic_multa
                2 -> R.drawable.ic_license
                3 -> R.drawable.ic_home
                else -> R.drawable.ic_payment
            }
            holder.ivIcono.setImageResource(iconRes)

            // Fecha
            holder.tvFecha.text = dateFormat.format(Date(pago.fechaPago))

            // Monto
            holder.tvMonto.text = formatter.format(pago.monto)

            // Concepto (truncado)
            val concepto = pago.concepto
            holder.tvConcepto.text = if (concepto.length > 30) {
                concepto.substring(0, 27) + "..."
            } else {
                concepto
            }

            // Click listener
            holder.cardPago.setOnClickListener {
                onItemClick(pago)
            }
        }

        override fun getItemCount() = pagos.size
    }
}