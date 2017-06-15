package services

import com.github.kittinunf.fuel.httpGet
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.obj
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonParser
import models.SunInfo
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import java.nio.charset.Charset
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class SunService {
  fun getSunInfo(lat: Double, lon: Double): Promise<SunInfo, Exception> = task {
    val url = "http://api.sunrise-sunset.org/json?lat=$lat&lng=$lon&formatted=0"
    val (_, response) = url.httpGet().responseString()
    val jsonStr = String(response.data, Charset.forName("UTF-8"))
    val json = JsonParser().parse(jsonStr).obj
    val sunrise = json["results"]["sunrise"].string
    val sunset = json["results"]["sunset"].string

    val sunriseTime = ZonedDateTime.parse(sunrise)
    val sunsetTime = ZonedDateTime.parse(sunset)
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss").
        withZone(ZoneId.of("Australia/Sydney"))
    SunInfo(sunriseTime.format(formatter), sunsetTime.format(formatter))
  }
}