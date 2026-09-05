package com.example.radar.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * REPOSITÓRIO DE CACHE OFFLINE (ROOM)
 *
 * Abstrai o acesso aos DAOs de ofertas e rotas, garantindo execução em threads de I/O
 * e fornecendo fluxos reativos Flow<T> para que a interface Compose atualize
 * automaticamente mesmo quando o motoboy estiver em túneis, garagens ou áreas sem rede.
 */
class RadarCacheRepository(private val database: RadarRoomDatabase) {

    private val offerDao = database.cachedOfferDao()
    private val routeDao = database.cachedRouteDao()

    // Fluxos reativos
    val recentOffers: Flow<List<CachedOfferEntity>> = offerDao.getRecentOffers(50)
    val acceptedHistory: Flow<List<CachedOfferEntity>> = offerDao.getAcceptedHistory()
    val pendingOffers: Flow<List<CachedOfferEntity>> = offerDao.getPendingOffers()
    val recentRoutes: Flow<List<CachedRouteEntity>> = routeDao.getRecentRoutes(40)
    val activeRoute: Flow<CachedRouteEntity?> = routeDao.getActiveRoute()

    /**
     * Armazena uma nova oferta recebida pelo radar em cache local
     */
    suspend fun cacheOffer(offer: CachedOfferEntity) = withContext(Dispatchers.IO) {
        offerDao.insertOffer(offer)
    }

    /**
     * Armazena múltiplas ofertas em lote
     */
    suspend fun cacheOffers(offers: List<CachedOfferEntity>) = withContext(Dispatchers.IO) {
        offerDao.insertOffers(offers)
    }

    /**
     * Atualiza o status da oferta quando o motoboy aceita ou recusa
     */
    suspend fun updateOfferStatus(offerId: String, status: String) = withContext(Dispatchers.IO) {
        offerDao.updateOfferStatus(offerId, status)
    }

    /**
     * Armazena ou atualiza uma rota de entrega
     */
    suspend fun cacheRoute(route: CachedRouteEntity) = withContext(Dispatchers.IO) {
        routeDao.insertRoute(route)
    }

    /**
     * Atualiza o status de uma rota
     */
    suspend fun updateRouteStatus(routeId: String, status: String) = withContext(Dispatchers.IO) {
        routeDao.updateRouteStatus(routeId, status)
    }

    /**
     * Limpa todo o cache de ofertas e rotas locais
     */
    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        offerDao.clearAllOffers()
        routeDao.clearAllRoutes()
    }

    /**
     * Inicializa dados de contingência offline se o banco local estiver vazio
     */
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val count = offerDao.getOfferCount()
        if (count == 0) {
            val now = System.currentTimeMillis()
            val initialOffers = listOf(
                CachedOfferEntity(
                    id = "off_cached_01",
                    appName = "iFood",
                    restaurant = "Burger King - Av. Paulista",
                    value = 33.00,
                    distanceKm = 4.2,
                    timeMinutes = 14,
                    gainPerKm = 7.86,
                    pickupAddress = "Av. Paulista, 1230 - Bela Vista",
                    deliveryAddress = "Rua Augusta, 1508 - Consolação",
                    neuralDecision = "ACCEPT",
                    neuralReason = "Ganho de R$ 7.86/km muito acima da média",
                    confidence = 0.98,
                    status = "ACCEPTED",
                    fuelCost = 0.71,
                    netProfit = 32.29,
                    timestamp = now - 3600000
                ),
                CachedOfferEntity(
                    id = "off_cached_02",
                    appName = "Rappi",
                    restaurant = "Pizza Hut - Jardins",
                    value = 18.00,
                    distanceKm = 2.4,
                    timeMinutes = 11,
                    gainPerKm = 7.50,
                    pickupAddress = "Alameda Santos, 980 - Cerqueira César",
                    deliveryAddress = "Rua Bela Cintra, 890 - Consolação",
                    neuralDecision = "ACCEPT",
                    neuralReason = "Distância curta compensa trajeto",
                    confidence = 0.88,
                    status = "ACCEPTED",
                    fuelCost = 0.40,
                    netProfit = 17.60,
                    timestamp = now - 7200000
                ),
                CachedOfferEntity(
                    id = "off_cached_03",
                    appName = "iFood + Rappi",
                    restaurant = "Starbucks - Frei Caneca",
                    value = 26.50,
                    distanceKm = 3.1,
                    timeMinutes = 12,
                    gainPerKm = 8.55,
                    pickupAddress = "Rua Frei Caneca, 569 - Consolação",
                    deliveryAddress = "Rua da Consolação, 2410 - Jardins",
                    neuralDecision = "ACCEPT",
                    neuralReason = "Multi-app otimizado pelo Jarvis",
                    confidence = 0.95,
                    status = "COMPLETED",
                    fuelCost = 0.52,
                    netProfit = 25.98,
                    timestamp = now - 10800000
                ),
                CachedOfferEntity(
                    id = "off_cached_04",
                    appName = "Uber Direct",
                    restaurant = "Madero Steak House",
                    value = 38.00,
                    distanceKm = 5.5,
                    timeMinutes = 17,
                    gainPerKm = 6.91,
                    pickupAddress = "Av. Brig. Faria Lima, 2232 - Pinheiros",
                    deliveryAddress = "Rua Aspicuelta, 450 - Vila Madalena",
                    neuralDecision = "ACCEPT",
                    neuralReason = "Ticket alto e rota fluida",
                    confidence = 0.92,
                    status = "ACCEPTED",
                    fuelCost = 0.93,
                    netProfit = 37.07,
                    timestamp = now - 14400000
                ),
                CachedOfferEntity(
                    id = "off_cached_05",
                    appName = "99 Food",
                    restaurant = "McDonald's - Rebouças",
                    value = 19.00,
                    distanceKm = 2.8,
                    timeMinutes = 10,
                    gainPerKm = 6.78,
                    pickupAddress = "Av. Rebouças, 1800 - Pinheiros",
                    deliveryAddress = "Rua Fradique Coutinho, 720 - Vila Madalena",
                    neuralDecision = "ACCEPT",
                    neuralReason = "Entrega expressa com excelente R$/km",
                    confidence = 0.91,
                    status = "COMPLETED",
                    fuelCost = 0.47,
                    netProfit = 18.53,
                    timestamp = now - 18000000
                )
            )
            offerDao.insertOffers(initialOffers)

            val initialRoutes = listOf(
                CachedRouteEntity(
                    routeId = "route_01",
                    offerId = "off_cached_01",
                    appName = "iFood",
                    originName = "Burger King - Paulista",
                    originAddress = "Av. Paulista, 1230",
                    destinationName = "Residencial Jardins",
                    destinationAddress = "Rua Augusta, 1508 - Apto 82",
                    totalDistanceKm = 4.2,
                    estimatedMinutes = 14,
                    waypointsSummary = "● Coleta: BK Paulista ➔ ● Entrega: R. Augusta 1508",
                    completedAt = now - 3600000,
                    status = "COMPLETED"
                ),
                CachedRouteEntity(
                    routeId = "route_02",
                    offerId = "off_cached_02",
                    appName = "Rappi",
                    originName = "Pizza Hut - Jardins",
                    originAddress = "Al. Santos, 980",
                    destinationName = "Edifício Bela Cintra",
                    destinationAddress = "R. Bela Cintra, 890",
                    totalDistanceKm = 2.4,
                    estimatedMinutes = 11,
                    waypointsSummary = "● Coleta: Pizza Hut ➔ ● Entrega: R. Bela Cintra 890",
                    completedAt = now - 7200000,
                    status = "COMPLETED"
                ),
                CachedRouteEntity(
                    routeId = "route_03",
                    offerId = "off_cached_04",
                    appName = "Uber Direct",
                    originName = "Madero - Faria Lima",
                    originAddress = "Av. Faria Lima, 2232",
                    destinationName = "Vila Madalena Loft",
                    destinationAddress = "R. Aspicuelta, 450",
                    totalDistanceKm = 5.5,
                    estimatedMinutes = 17,
                    waypointsSummary = "● Coleta: Madero Faria Lima ➔ ● Entrega: R. Aspicuelta 450",
                    completedAt = now - 14400000,
                    status = "COMPLETED"
                )
            )
            routeDao.insertRoutes(initialRoutes)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: RadarCacheRepository? = null

        fun getInstance(context: Context): RadarCacheRepository {
            return INSTANCE ?: synchronized(this) {
                val database = RadarRoomDatabase.getDatabase(context)
                val instance = RadarCacheRepository(database)
                INSTANCE = instance
                instance
            }
        }
    }
}
