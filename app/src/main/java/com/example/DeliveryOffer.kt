package com.example

data class DeliveryOffer(
    val id: String,
    val nomeRestaurante: String,
    val valor: Double,
    val distancia: Double,
    val tempoEstimado: Int
) {
    val ganhoPorKm: Double
        get() = if (distancia > 0) valor / distancia else valor

    val isAltaRentabilidade: Boolean
        get() = ganhoPorKm >= 5.0
}
