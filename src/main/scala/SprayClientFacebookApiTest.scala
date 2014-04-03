package spracebook

/* REPL commands for manual testing:
import akka.dispatch.Future
import akka.actor._
import akka.dispatch.Await
import akka.util.duration._
import akka.util.Timeout
import spray.can.client.HttpClient
import spray.client.HttpConduit
import spray.io._
import spray.util._
import spray.http._
import HttpMethods._
import HttpConduit._
import spracebook._
implicit val system = ActorSystem()
val ioBridge = IOExtension(system).ioBridge()
val httpClient = system.actorOf(Props(new HttpClient(ioBridge)))
val conduit = system.actorOf(
  props = Props(new HttpConduit(httpClient, "graph.facebook.com", 443, sslEnabled = true)),
  name = "http-conduit"
)
val fb = new SprayClientFacebookGraphApi(conduit)

fb.getUser(token)

val token = "TODO"

fb.extendToken("TODO", "TODO", "TODO")

fb.debugToken("TODO", "TODO")

fb.newPhotos(token, None)

Await.result(fb.getFriends(token), 1 minutes)

fb.createStory("cuppofjoe:photograph", "cup", "380730112032825", "https://fbcdn-sphotos-e-a.akamaihd.net/hphotos-ak-frc1/481514_10151657606201011_1061656805_n.jpg", "TODO")

fb.createComment("4286226694008", "Check it out! http://cuppofjoe.com", token)

*/

import scala.concurrent.Future

import akka.actor._
import scala.concurrent.Await
import scala.concurrent.duration
import akka.util.Timeout

import akka.io.IO
import akka.pattern.ask
import spracebook.FacebookGraphApiJsonProtocol.User
import spray.can.Http
import spray.http._
import spray.client.pipelining._


import scala.util.{Success, Failure}
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.event.Logging
import spray.util._
import akka.routing.FromConfig
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps


object SprayCientFacebookApiTest {
//  implicit val system = ActorSystem()
//  val ioBridge = IOExtension(system).ioBridge()
//  val httpClient = system.actorOf(Props(new HttpClient(ioBridge)))
//
//  val facebookApiConduit = system.actorOf(
//    props = Props(new HttpConduit(httpClient, "graph.facebook.com", 443, sslEnabled = true)),
//    name = "facebook-api-conduit"
//  )
//
//  val token = "TODO"
//
//  val fbApi = new SprayClientFacebookGraphApi(facebookApiConduit)
//
//  def main(args: Array[String]) {
//    val users = Await.result(fbApi.getLikes("487217224648173", token), 1 minutes)
//    println("Result : " + users)
//    system.shutdown
//  }

  implicit val timeout = Timeout(5 seconds)

  implicit val system = ActorSystem()
  import system.dispatcher // execution context for futures
  val fbApi = system.actorOf(Props(new SprayClientFacebookGraphApi("https://graph.facebook.com")), name = "facebookActor")

  val token = "TODO"

    def main(args: Array[String]) {
        val users = fbApi ? getLikes("487217224648173", token)


//      users onComplete {
//        case Success(Seq[User]) =>
//
//          shutdown()
//
//
//
//        case Failure(error) =>
//          println(error)
//          shutdown()
//      }






      }

  def shutdown(): Unit = {
    IO(Http).ask(Http.CloseAll)(1.second).await
    system.shutdown()
  }



}
