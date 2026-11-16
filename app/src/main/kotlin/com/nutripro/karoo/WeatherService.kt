package com.nutripro.karoo

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.HttpResponseState
import io.hammerhead.karooext.models.OnHttpResponse
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import kotlin.coroutines.resume

/** Fetches current ambient temperature from Open-Meteo (spec 3.3), free and keyless. */
class WeatherService(private val karooSystem: KarooSystemService) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchTemperatureC(lat: Double, lng: Double): Double? = suspendCancellableCoroutine { cont ->
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lng&current=temperature_2m"

        var consumerId: String? = null
        consumerId = karooSystem.addConsumer<OnHttpResponse>(
            params = OnHttpResponse.MakeHttpRequest(method = "GET", url = url),
            onError = { message ->
                Timber.w("Weather request failed: %s", message)
                if (cont.isActive) cont.resume(null)
            },
            onEvent = { event ->
                when (val state = event.state) {
                    is HttpResponseState.Complete -> {
                        val temp = parseTemperature(state)
                        if (cont.isActive) cont.resume(temp)
                        consumerId?.let { karooSystem.removeConsumer(it) }
                    }
                    else -> Unit // Queued / InProgress — keep waiting
                }
            },
        )
        cont.invokeOnCancellation { consumerId?.let { karooSystem.removeConsumer(it) } }
    }

    private fun parseTemperature(state: HttpResponseState.Complete): Double? {
        val body = state.body
        if (state.statusCode !in 200..299 || body == null) {
            Timber.w("Weather request returned status %d: %s", state.statusCode, state.error)
            return null
        }
        return try {
            json.decodeFromString<OpenMeteoResponse>(body.decodeToString()).current.temperatureC
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse weather response")
            null
        }
    }

    @Serializable
    private data class OpenMeteoResponse(val current: CurrentWeather)

    @Serializable
    private data class CurrentWeather(val temperature_2m: Double) {
        val temperatureC: Double get() = temperature_2m
    }
}
