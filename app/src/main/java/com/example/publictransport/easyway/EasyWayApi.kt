// EasyWayApi.kt
package com.example.publictransport.easyway

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

interface EasyWayApi {
    /**
     * 1) Получить возможные варианты проезда (ways).
     *
     * curl --location 'https://kz.easyway.info/ajax/ru/almaty/compile' \
     *   --header 'content-type: application/x-www-form-urlencoded; charset=UTF-8' \
     *   --header 'x-requested-with: XMLHttpRequest' \
     *   --header 'Cookie: full_version=1' \
     *   --data 'start_lat=43.226869&start_lng=76.923396&stop_lat=43.220059&stop_lng=76.896944&direct=false&way_type=optimal&transports=metro%2Ctrol%2Cbus&enable_walk_ways=0'
     */
    @FormUrlEncoded
    @Headers(
        "content-type: application/x-www-form-urlencoded; charset=UTF-8",
        "x-requested-with: XMLHttpRequest",
        "Cookie: full_version=1"
    )
    @POST("ajax/ru/almaty/compile")
    suspend fun compile(
        @Field("start_lat")   startLat: Double,
        @Field("start_lng")   startLng: Double,
        @Field("stop_lat")    stopLat: Double,
        @Field("stop_lng")    stopLng: Double,
        @Field("direct")      direct: Boolean       = false,
        @Field("way_type")    wayType: String       = "optimal",
        @Field("transports")  transports: String,      // e.g. "metro,trol,bus"
        @Field("enable_walk_ways") enableWalkWays: Int = 0
    ): CompileResponse

    /**
     * 2) Получить полную геометрию (compile_points + route_points).
     *
     * curl --location 'https://kz.easyway.info/ajax/ru/almaty/getCompileRoute' \
     *   --header 'content-type: application/x-www-form-urlencoded; charset=UTF-8' \
     *   --header 'x-requested-with: XMLHttpRequest' \
     *   --header 'Cookie: full_version=1' \
     *   --data 'ids=60&starts=1201&stops=1259&a=43.226869%2C76.923396&b=43.220059%2C76.896944'
     */
    @FormUrlEncoded
    @Headers(
        "content-type: application/x-www-form-urlencoded; charset=UTF-8",
        "x-requested-with: XMLHttpRequest",
        "Cookie: full_version=1"
    )
    @POST("ajax/ru/almaty/getCompileRoute")
    suspend fun getCompileRoute(
        @Field("ids")    ids: String,    // ID маршрута, например "60"
        @Field("starts") starts: String, // e.g. "1201"
        @Field("stops")  stops: String,  // e.g. "1259"
        @Field("a")      a: String,      // "43.226869,76.923396"
        @Field("b")      b: String       // "43.220059,76.896944"
    ): CompileRouteResponse
}
