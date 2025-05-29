package com.example.publictransport.yandex

// Модели данных для Яндекс API
data class YandexRouteResponse(
    val type: String,
    val features: List<YandexFeature>
)

data class YandexFeature(
    val properties: YandexProperties,
    val geometry: YandexGeometry
)

data class YandexProperties(
    val summary: YandexRouteSummary,
    val segments: List<YandexSection>
)
data class YandexGeometry(
    val type: String,
    val coordinates: List<List<Double>>
)

data class YandexRoute(
    val summary: YandexRouteSummary,
    val sections: List<YandexSection>
)

data class YandexRouteSummary(
    val duration: YandexDuration,
    val distance: YandexDistance,
    val transfers: Int?
)

data class YandexDuration(
    val value: Int, // в секундах
    val text: String
)

data class YandexDistance(
    val value: Int, // в метрах
    val text: String
)

data class YandexSection(
    val type: String,                // e.g. "pedestrian" или "transport"
    val duration: YandexDuration,
    val distance: YandexDistance,
    val transport: YandexTransit?,   // только у transport-секций
    val geometry: YandexGeometry?    // у transport или pedestrian
)

data class YandexTransit(
    val line: YandexTransitLine
)

data class YandexTransitLine(
    val name: String,
    val short_name: String?,
    val color: String?,
    val vehicle: YandexVehicle
)

data class YandexVehicle(
    val type: String // "bus", "trolleybus", "tram", "metro", etc.
)

data class YandexPolyline(
    val points: String
)