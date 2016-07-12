package services

import com.github.kittinunf.fuel.httpGet
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.obj
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonParser
import models.SunInfo
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import java.nio.charset.Charset

class SunService {
  fun getSunInfo(lat: Double, lon: Double): Promise<SunInfo, Exception> = task {
    val url = "http://api.sunrise-sunset.org/json?lat=$lat&lng=$lon&formatted=0"
    val (request, response, result) = url.httpGet().responseString()
    val jsonStr = String(response.data, Charset.forName("UTF-8"))
    val json = JsonParser().parse(jsonStr).obj
    val sunrise = json["results"]["sunrise"].string
    val sunset = json["results"]["sunset"].string
    val sunriseTime = DateTime.parse(sunrise)
    val sunsetTime = DateTime.parse(sunset)
    val formatter = DateTimeFormat.forPattern("HH:mm:ss").
        withZone(DateTimeZone.forID("Australia/Sydney"))
    SunInfo(formatter.print(sunriseTime),
        formatter.print(sunsetTime))
  }
}