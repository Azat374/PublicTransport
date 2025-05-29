//package com.example.publictransport.yandex
//
//import com.example.publictransport.ui.TripResponse
//import com.example.publictransport.yandex.YandexRouteAdapter
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
///**
//* Сервис для получения маршрутов общественного транспорта через Яндекс Routing API.
//*/
//object RouteService {
//    // TODO: замените на ваш реальный API-ключ из Яндекс.Облака
//    private const val YANDEX_API_KEY = "YOUR_YANDEX_API_KEY"
//
//    /**
//     * Запрашивает у Яндекса маршрут от [fromLat],[fromLon] до [toLat],[toLon]
//     * с mode=masstransit и возвращает его в формате вашего TripResponse.
//     */
//    suspend fun getRoutesFromYandex(
//        fromLat: Double,
//        fromLon: Double,
//        toLat:   Double,
//        toLon:   Double
//    ): List<TripResponse> = withContext(Dispatchers.IO) {
//        try {
//            // В формате "lat,lon|lat,lon"
//            val waypoints = "$fromLat,$fromLon|$toLat,$toLon"
//            val response = YandexRetrofitInstance.api.getRoute(
//                apiKey    = YANDEX_API_KEY,
//                waypoints = waypoints
//            )
//            // Конвертируем GeoJSON-ответ в ваш формат
//            YandexRouteAdapter.toTripResponses(response)
//        } catch (e: Exception) {
//            // Оборачиваем любую ошибку в понятное сообщение
//            throw Exception("Ошибка при получении маршрутов от Яндекса: ${e.message}", e)
//        }
//    }
//}