import com.github.kittinunf.fuel.core.Client
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import io.kotlintest.Duration
import io.kotlintest.Eventually
import io.kotlintest.specs.StringSpec
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import services.SunService
import java.util.concurrent.TimeUnit


class FuelTestClient(val testResponse: Response) : Client {
  override fun executeRequest(request: Request): Response {
    return testResponse
  }
}

class ApplicationSpec : StringSpec(), Eventually {
  init {
    "DateTimeFormat must return 1970 as the beginning of epoch" {
      val beginning = DateTime(0)
      val formattedYear = DateTimeFormat.forPattern("YYYY").print(beginning)
      formattedYear shouldBe "1970"
    }

    "SunService must retrieve correct sunset and sunrise information" {
      val json = """{
            "results":{
              "sunrise":"2016-04-14T20:18:12+00:00",
              "sunset":"2016-04-15T07:31:52+00:00"
            }
        }"""
      val testResponse = Response()
      testResponse.data = json.toByteArray()
      val testClient = FuelTestClient(testResponse)
      FuelManager.instance.client = testClient

      val lat = -33.8830
      val lon = 151.2167
      val sunService = SunService()
      val resultP = sunService.getSunInfo(lat, lon)

      eventually(Duration(5, TimeUnit.SECONDS)) {
        val sunInfo = resultP.get()
        sunInfo.sunrise shouldBe "06:18:12"
      }
    }
  }
}