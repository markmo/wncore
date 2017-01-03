package engines

import akka.actor.{Actor, ActorLogging}
import models.ConversationEngine._
import models.events._
import modules.akkaguice.NamedActor
import utils.RegexUtils

/**
  * Created by markmo on 21/11/2016.
  */
class CommandIntentActor extends Actor with ActorLogging {

  import CommandIntentActor._
  import RegexUtils._

  def receive = {

    case ev@TextResponse(_, from, text, _) =>
      log.debug("CommandIntentActor received TextResponse")

      if (SwitchEngineCommand matches text) {
        log.debug("switch conversation engine")
        if (text contains "watson") {
          log.debug("to Watson")
          sender() ! IntentVote(1.0, SetEngine(from, Watson))
        } else {
          log.debug("to Cooee")
          sender() ! IntentVote(1.0, SetEngine(from, Cooee))
        }

      } else if (HistoryCommand matches text) {
        log.debug("showing history")
        sender() ! IntentVote(1.0, ShowHistory(from))

      } else if (LoginCommand matches text) {
        log.debug("sending login card")
        sender() ! IntentVote(1.0, LoginCard(from))

      } else if (ResetCommand matches text) {
        log.debug("resetting")
        sender() ! IntentVote(1.0, Reset)

      } else {
        sender() ! IntentVote(0, ev)
      }

  }

  def formatKeywords(keywords: Map[String, Double]) =
    keywords map {
      case (keyword, relevance) => f"$keyword ($relevance%2.2f)"
    } mkString "\n"

}

object CommandIntentActor extends NamedActor {

  override final val name = "CommandIntentActor"

  val SwitchEngineCommand = command("engine")
  val HistoryCommand = command("history")
  val LoginCommand = command("login")
  val ResetCommand = command("reset")

  private def command(name: String) = s"""^[/:]$name.*""".r

}
