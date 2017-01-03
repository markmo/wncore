import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{MalformedRequestContentRejection, RejectionHandler}
import akka.stream.ActorMaterializer
import com.google.inject.Guice
import com.typesafe.config.Config
import controllers._
import modules.akkaguice.{AkkaModule, GuiceAkkaExtension}
import modules.config.ConfigModule
import modules.conversation.ConversationModule
import modules.logging.LoggingModule
import net.codingwell.scalaguice.InjectorExtensions._
import services.LiveEngageChatActor

/**
  * Created by markmo on 16/07/2016.
  */
object Main extends App with CorsSupport {

  import StatusCodes._

  val injector = Guice.createInjector(
    new ConfigModule(),
    new LoggingModule(),
    new AkkaModule(),
    new ConversationModule()
  )

  val config = injector.instance[Config]
  val logger = injector.instance[LoggingAdapter]

  val interface = config.getString("http.interface")
  val port = config.getInt("http.port")

  implicit val system = injector.instance[ActorSystem]
  implicit val fm = ActorMaterializer()

  val facebookController = injector.instance[FacebookController]
  val addressController = injector.instance[ValidationController]
  val smsController = injector.instance[SMSController]
  val emailController = injector.instance[EmailController]
  val wvaChatController = injector.instance[WvaChatController]

  val routes =
    facebookController.routes ~
      addressController.routes ~
      smsController.routes ~
      emailController.routes ~
      wvaChatController.routes

  implicit def myRejectionHandler = {
    val handler = RejectionHandler.newBuilder().handle {
      case MalformedRequestContentRejection(message, e) =>
        logger.error(message)
        extractRequest { request =>
          logger.error(request._4.toString)
          complete(BadRequest)
        }
      case e =>
        logger.error(e.toString)
        complete(BadRequest)
    }
    handler.result()
  }

  val bindingFuture = Http().bindAndHandle(corsHandler(routes), interface, port)

  facebookController.setupWelcomeGreeting()

  if (config.getString("settings.fallback-engine") == "LP") {
    system.actorOf(GuiceAkkaExtension(system).props(LiveEngageChatActor.name))
  }
}
