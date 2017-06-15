package verticles


import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariDataSource
import io.vertx.core.*
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.*
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.ext.web.templ.ThymeleafTemplateEngine
import nl.komponents.kovenant.functional.bind
import nl.komponents.kovenant.functional.map
import services.MigrationService
import services.SunService
import services.WeatherService
import models.*
import services.DatabaseAuthProvider
import uy.klutter.vertx.VertxInit


class MainVerticle : AbstractVerticle() {

  private var maybeDataSource: HikariDataSource? = null
  private val logger = LoggerFactory.getLogger(this.javaClass.name)

  private fun initDataSource(config: DataSourceConfig): HikariDataSource {
    val hikariDS = HikariDataSource()
    hikariDS.username = config.user
    hikariDS.password = config.password
    hikariDS.jdbcUrl = config.jdbcUrl
    maybeDataSource = hikariDS
    return hikariDS
  }

  override fun stop(stopFuture: Future<Void>?) {
    maybeDataSource?.close()
  }

  override fun start(startFuture: Future<Void>?) {
    logger.info("Starting the server")
    VertxInit.ensure()
    val server = vertx.createHttpServer()
    val router = Router.router(vertx)

    val templateEngine = ThymeleafTemplateEngine.create()

    val weatherService = WeatherService()
    val sunService = SunService()
    val jsonMapper = jacksonObjectMapper()

    val serverConfig = jsonMapper.readValue(config().
        getJsonObject("server").encode(), ServerConfig::class.java)
    val serverPort = serverConfig.port
    val enableCaching = serverConfig.caching

    val dataSourceConfig = jsonMapper.readValue(config().
      getJsonObject("dataSource").encode(), DataSourceConfig::class.java)
    val dataSource = initDataSource(dataSourceConfig)

    val migrationService = MigrationService(dataSource)
    val migrationResult = migrationService.migrate()
    migrationResult.fold({ exc ->
      logger.fatal("Exception occurred while performing migration", exc)
      vertx.close()
    },{ _ ->
      logger.debug("Migration successful or not needed")
    })

    val staticHandler = StaticHandler.create().
        setWebRoot("public").setCachingEnabled(enableCaching)

    val authProvider = DatabaseAuthProvider(dataSource, jsonMapper)
    router.route().handler(CookieHandler.create())
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)))
    router.route().handler(UserSessionHandler.create(authProvider))
    router.route("/hidden/*").handler(RedirectAuthHandler.create(authProvider))
    router.route("/login").handler(BodyHandler.create());
    router.route("/login").handler(FormLoginHandler.create(authProvider))
    router.route("/public/*").handler(staticHandler)
    router.get("/api/data").handler { ctx ->
      val lat = -33.8830
      val lon = 151.2167
      val sunInfoP = sunService.getSunInfo(lat, lon)
      val temperatureP = weatherService.getTemperature(lat, lon)
      val sunWeatherInfoP = sunInfoP.bind { sunInfo ->
        temperatureP.map { temp -> SunWeatherInfo(sunInfo, temp) }
      }
      sunWeatherInfoP.success { info ->
        val json = jsonMapper.writeValueAsString(info)
        val response = ctx.response()
        response.end(json)
      }
    }
    fun renderTemplate(ctx: RoutingContext, template: String) {
      templateEngine.render(ctx, template, { buf ->
        val response = ctx.response()
        if (buf.failed()) {
          logger.error("Template rendering failed", buf.cause())
          response.setStatusCode(500).end()
        } else {
          response.end(buf.result())
        }
      })
    }
    router.get("/hidden/admin").handler { ctx ->
      renderTemplate(ctx.put("username",
          ctx.user().principal().getString("username")),
          "public/templates/admin.html")
    }
    router.get("/loginpage").handler { ctx ->
      renderTemplate(ctx,"public/templates/login.html" ) }
    router.get("/home").handler { ctx ->
      renderTemplate(ctx, "public/templates/index.html") }
    router.get("/").handler { routingContext ->
      val response = routingContext.response()
      response.end("Hello World")
    }

    server.requestHandler{ router.accept(it) }.listen(serverPort, { handler ->
      if (!handler.succeeded()) {
        System.err.println("Failed to listen on port $serverPort")
      }
    })
  }
}