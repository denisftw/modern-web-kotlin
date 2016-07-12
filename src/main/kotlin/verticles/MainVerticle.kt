package verticles


import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.templ.ThymeleafTemplateEngine
import models.SunWeatherInfo
import nl.komponents.kovenant.functional.bind
import nl.komponents.kovenant.functional.map
import services.SunService
import services.WeatherService


class MainVerticle : AbstractVerticle() {
  override fun start(startFuture: Future<Void>?) {
    val server = vertx.createHttpServer()
    val router = Router.router(vertx)
    val templateEngine = ThymeleafTemplateEngine.create()
    val logger = LoggerFactory.getLogger("VertxServer")
    val weatherService = WeatherService()
    val sunService = SunService()

    router.get("/home").handler { ctx ->
      val lat = -33.8830
      val lon = 151.2167
      val sunInfoP = sunService.getSunInfo(lat, lon)
      val temperatureP = weatherService.getTemperature(lat, lon)
      val sunWeatherInfoP = sunInfoP.bind { sunInfo ->
        temperatureP.map { temp -> SunWeatherInfo(sunInfo, temp) }
      }
      sunWeatherInfoP.success { info ->
        ctx.put("sunrise", info.sunInfo.sunrise)
        ctx.put("sunset", info.sunInfo.sunset)
        ctx.put("temperature", info.temperature)
        templateEngine.render(ctx, "public/templates/index.html", { buf ->
          if (buf.failed()) {
            logger.error("Template rendering failed", buf.cause())
          } else {
            val response = ctx.response()
            response.end(buf.result())
          }
        })
      }
      sunWeatherInfoP.fail { error ->
        logger.error("Remote server querying failed", error)
      }
    }
    router.get("/").handler { routingContext ->
      val response = routingContext.response()
      response.end("Hello World")
    }


    server.requestHandler{ router.accept(it) }.listen(8080, { handler ->
      if (!handler.succeeded()) {
        System.err.println("Failed to listen on port 8080")
      }
    })
  }
}