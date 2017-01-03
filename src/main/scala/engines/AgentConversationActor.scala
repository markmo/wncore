package engines

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.contrib.pattern.ReceivePipeline
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import engines.interceptors.LoggingInterceptor
import models.events.SetProvider
import modules.akkaguice.NamedActor

/**
  * Created by markmo on 10/09/2016.
  */
class AgentConversationActor @Inject()(@Assisted("defaultUserProvider") defaultUserProvider: ActorRef,
                                       @Assisted("defaultAgentProvider") defaultAgentProvider: ActorRef,
                                       @Assisted("historyActor") historyActor: ActorRef)
  extends Actor
    with ActorLogging
    with ReceivePipeline
    with LoggingInterceptor {

  // import execution context for service calls

  def receive = withProvider(defaultUserProvider, defaultAgentProvider)

  def withProvider(userProvider: ActorRef, agentProvider: ActorRef): Receive = {

    case SetProvider(_, _, ref, _, _, _) =>
      context become withProvider(ref, agentProvider)

  }

}

object AgentConversationActor extends NamedActor {

  override final val name = "AgentConversationActor"

  trait Factory {
    def apply(@Assisted("defaultUserProvider") defaultUserProvider: ActorRef,
              @Assisted("defaultAgentProvider") defaultAgentProvider: ActorRef,
              @Assisted("historyActor") historyActor: ActorRef): Actor
  }

}
