package com.example.geofence

/**
 * Representa uma zona geográfica de alta demanda de pedidos (Hotspot / Polo Gastronômico)
 * monitorada via Google Location Geofencing API.
 */
data class DemandZone(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 1200f,
    val surgeMultiplier: Double = 1.45,
    val primaryApps: List<String> = listOf("iFood", "Rappi", "Uber Direct", "99 Food"),
    val description: String = "Polo de alta demanda gastronômica e multi-pedidos"
)

object DemandZonesCatalog {
    val DEFAULT_ZONES = listOf(
        DemandZone(
            id = "zone_paulista_jardins",
            name = "Polo Paulista & Jardins",
            latitude = -23.561414,
            longitude = -46.655881,
            radiusMeters = 1500f,
            surgeMultiplier = 1.65,
            primaryApps = listOf("iFood", "Rappi", "Uber Direct"),
            description = "Forte concentração de restaurantes gourmet, fast-food e alta taxa de gorjetas"
        ),
        DemandZone(
            id = "zone_faria_lima_itaim",
            name = "Polo Faria Lima & Itaim Bibi",
            latitude = -23.585556,
            longitude = -46.680556,
            radiusMeters = 1800f,
            surgeMultiplier = 1.75,
            primaryApps = listOf("iFood", "Rappi", "Uber Direct"),
            description = "Alto volume de pedidos corporativos e ticket médio elevado"
        ),
        DemandZone(
            id = "zone_pinheiros_madalena",
            name = "Polo Pinheiros & Vila Madalena",
            latitude = -23.560124,
            longitude = -46.691456,
            radiusMeters = 1400f,
            surgeMultiplier = 1.55,
            primaryApps = listOf("iFood", "99 Food", "Rappi"),
            description = "Intensa atividade noturna e restaurantes delivery 24h"
        ),
        DemandZone(
            id = "zone_moema_ibirapuera",
            name = "Polo Moema & Ibirapuera",
            latitude = -23.602778,
            longitude = -46.662778,
            radiusMeters = 1600f,
            surgeMultiplier = 1.50,
            primaryApps = listOf("iFood", "Uber Direct", "99 Food"),
            description = "Região residencial de alta densidade com pedidos encadeados frequentes"
        ),
        DemandZone(
            id = "zone_morumbi_berrini",
            name = "Polo Shopping Morumbi & Berrini",
            latitude = -23.622500,
            longitude = -46.698611,
            radiusMeters = 1500f,
            surgeMultiplier = 1.60,
            primaryApps = listOf("iFood", "Rappi", "Uber Direct"),
            description = "Hub de shoppings e centros empresariais com demanda em horários de pico"
        ),
        DemandZone(
            id = "zone_tatuape_analia",
            name = "Polo Tatuapé & Anália Franco",
            latitude = -23.541667,
            longitude = -46.575000,
            radiusMeters = 1700f,
            surgeMultiplier = 1.40,
            primaryApps = listOf("iFood", "99 Food"),
            description = "Principal polo gastronômico da Zona Leste com rotas curtas"
        ),
        DemandZone(
            id = "zone_santana_zn",
            name = "Polo Santana & Zona Norte",
            latitude = -23.504167,
            longitude = -46.626389,
            radiusMeters = 1600f,
            surgeMultiplier = 1.35,
            primaryApps = listOf("iFood", "Uber Direct"),
            description = "Corredor gastronômico da Av. Braz Leme e imediações"
        )
    )
}
