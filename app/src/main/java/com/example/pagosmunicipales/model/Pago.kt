package com.example.pagosmunicipales.model

import com.google.firebase.Timestamp

data class Pago(
    val id: String = "",
    val tipoPagoId: Int = 0, // 1=Multas, 2=Licencias, 3=Predial
    val folio: String = "",
    val monto: Double = 0.0,
    val metodoPago: String = "",
    val efectivoRecibido: Double = 0.0,
    val cambio: Double = 0.0,
    val fechaPago: Long = System.currentTimeMillis(),
    val usuario: String = "",
    val concepto: String = "",
    val infractor: String = "",
    val placas: String = "",
    val timestamp: Timestamp = Timestamp.now()
)