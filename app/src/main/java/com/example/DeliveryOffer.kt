package com.example

data class DeliveryOffer(
    val id: String,
    val nomeRestaurante: String,
    val valor: Double,
    val distancia: Double,
    val tempoEstimado: Int,
    val latitude: Double = -23.561684,
    val longitude: Double = -46.655981,
    val poloGastronomico: String = "Polo Paulista",
    val appOrigem: String = "iFood"
) {
    val ganhoPorKm: Double
        get() = if (distancia > 0) valor / distancia else valor

    val isAltaRentabilidade: Boolean
        get() = ganhoPorKm >= 5.0

    /**
     * Calcula a distância geodésica (Haversine em km) entre a localização do piloto e o ponto de coleta desta oferta.
     */
    fun distanceTo(driverLat: Double, driverLng: Double): Double {
        val r = 6371.0 // Raio médio da Terra em km
        val dLat = Math.toRadians(latitude - driverLat)
        val dLon = Math.toRadians(longitude - driverLng)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(driverLat)) * Math.cos(Math.toRadians(latitude)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return Math.round(r * c * 100.0) / 100.0
    }
}
