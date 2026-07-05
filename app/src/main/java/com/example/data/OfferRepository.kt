package com.example.data

import kotlinx.coroutines.flow.Flow

class OfferRepository(private val offerDao: OfferDao) {
    val allOffers: Flow<List<OfferEntity>> = offerDao.getAllOffers()

    suspend fun insert(offer: OfferEntity) {
        offerDao.insertOffer(offer)
    }

    suspend fun delete(id: Int) {
        offerDao.deleteOffer(id)
    }

    suspend fun clearHistory() {
        offerDao.clearHistory()
    }
}
