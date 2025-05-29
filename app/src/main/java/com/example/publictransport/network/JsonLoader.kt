package com.example.publictransport.network

import android.content.Context
import com.example.publictransport.model.Route
import com.example.publictransport.model.Stop
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object JsonLoader {
    fun loadRoutes(context: Context): List<Route> {
        val json = context.assets.open("route.json").bufferedReader().use { it.readText() }
        val listType = object : TypeToken<List<Route>>() {}.type
        return Gson().fromJson(json, listType)
    }

    fun loadStops(context: Context): List<Stop> {
        val json = context.assets.open("stop.json").bufferedReader().use { it.readText() }
        val listType = object : TypeToken<List<Stop>>() {}.type
        return Gson().fromJson(json, listType)
    }
}
