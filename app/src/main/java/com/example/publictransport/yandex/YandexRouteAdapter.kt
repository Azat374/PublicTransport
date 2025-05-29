//package com.example.publictransport.yandex
//
//import com.example.publictransport.ui.PathResponse
//import com.example.publictransport.ui.TripResponse
//
//// Адаптер для преобразования ответа Яндекса в ваш формат
//object YandexRouteAdapter {
//    fun toTripResponses(resp: YandexRouteResponse): List<TripResponse> {
//        return resp.features.map { feature ->
//            val props = feature.properties
//            val startWalk = props.segments.firstOrNull { it.type.contains("pede") }
//            val endWalk   = props.segments.lastOrNull  { it.type.contains("pede") }
//
//            val paths = props.segments
//                .filter { it.type == "transport" }
//                .map { seg ->
//                    val line = seg.transport!!.line
//                    line.short_name?.let {
//                        PathResponse(
//                            routeId       = line.short_name?.toLongOrNull() ?: 0L,
//                            directionIndex= 0,
//                            walkDistance  = 0,
//                            walkDuration  = "00:00:00",
//                            rideDistance  = seg.distance.value,
//                            rideDuration  = formatDuration(seg.duration.value),
//                            stops         = emptyList(),
//                            name          = line.name
//                        )
//                    }
//                }
//
//            TripResponse(
//                startWalkDistance  = startWalk?.distance?.value ?: 0,
//                startWalkDuration  = formatDuration(startWalk?.duration?.value ?: 0),
//                endWalkDistance    = endWalk?.distance?.value ?: 0,
//                endWalkDuration    = formatDuration(endWalk?.duration?.value ?: 0),
//                paths              = paths
//            )
//        }
//    }
//
//    private fun formatDuration(seconds: Int): String =
//        String.format("%02d:%02d:%02d",
//            seconds / 3600,
//            (seconds % 3600) / 60,
//            seconds % 60)
//}