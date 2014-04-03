package spracebook

import scala.concurrent.Future
import akka.actor.{Actor, ActorSystem, ActorRef}


import spray.http._
import spray.json.DefaultJsonProtocol
import spray.httpx.encoding.{Gzip, Deflate}
import spray.httpx.SprayJsonSupport._
import spray.client.pipelining._


import spray.httpx.encoding.{Gzip, Deflate}
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling.FromResponseUnmarshaller
import spray.httpx.TransformerAux._


import FacebookGraphApiJsonProtocol._
import spray.json._
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps
import spray.http.HttpRequest
import scala.Some
import spray.http.HttpResponse

//import TransformerAux._
//import grizzled.slf4j.Logging


import scala.util.{Success, Failure}
import scala.concurrent.duration._
import akka.actor.ActorSystem

import spray.can.Http


case class debugToken(appAccessToken: String, userAccessToken: String)

//: Future[TokenData]

case class extendToken(appId: String, appSecret: String, accessToken: String)

//: Future[AccessToken]

case class getAccessToken(appId: String, appSecret: String, code: String, redirectUri: String)

//: Future[AccessToken]

case class newPhotos(accessToken: String, after: Option[String])

//: Future[Response[Photo]]

case class getUser(accessToken: String)

//: Future[User]

case class getPage(pageId: String)

//: Future[Page]

case class getTab(pageId: String, appId: String, token: String)

//: Future[Response[Tab]]

case class createStory(action: String, objectName: String, objectId: String, imageUrl: String, message: Option[String], accessToken: String)

//: Future[CreatedStory]

case class createComment(photoId: String, message: String, accessToken: String)

//: Future[CreatedComment]

case class getFriends(accessToken: String)

//: Future[Seq[User]]

case class getComments(objectId: String, accessToken: String)

//: Future[Seq[Comment]]

case class getLikes(objectId: String, accessToken: String)

//: Future[Seq[User]]

case class getSharedPosts(objectId: String, accessToken: String)

//: Future[Seq[Share]]

//Facebook Insights
case class getApplicationOpenGraphActionCreate(appId: String, accessToken: String, since: Long, until: Long)

//: Future[Seq[Insight]]

case class getApplicationOpenGraphActionClick(appId: String, accessToken: String, since: Long, until: Long)

//: Future[Seq[Insight]]

case class getApplicationOpenGraphActionImpressions(appId: String, accessToken: String, since: Long, until: Long)

//: Future[Seq[Insight]]


//class SprayClientFacebookGraphApi(httpUrl: String) extends Actor with Logging {
class SprayClientFacebookGraphApi(httpUrl: String) extends Actor {

  //  class SprayClientFacebookGraphApi(httpUrl:String) extends FacebookGraphApi  {
  implicit val system = ActorSystem()

  import system.dispatcher


  //  val hbaseClients: HTablePool = new HTablePool(HbaseConstants.conf, 24)
  implicit val timeout = Timeout(5 seconds)

  val userFieldParams = "id,username,name,first_name,middle_name,last_name,email,link,gender,picture"


  def receive = {

    case debugToken(appAccessToken: String, userAccessToken: String) => {


      //      val um: HttpResponse => TokenDataWrapper = unmarshal[TokenDataWrapper]

      val pipeline: HttpRequest => Future[TokenDataWrapper] = (

        addHeader("Accept", "application/json")
          ~> sendReceive
          //          ~> mapErrors
          ~> unmarshal[TokenDataWrapper]

        )
      val url = httpUrl + "/debug_token?input_token=%s&access_token=%s" format(userAccessToken, appAccessToken)
      sender ! pipeline(Get(url))


    }


    case extendToken(appId: String, appSecret: String, accessToken: String) => {
      val pipeline: HttpRequest => Future[AccessToken] = (

        sendReceive
          ~> unmarshal[AccessToken]
        )
      val url = httpUrl + "/oauth/access_token?grant_type=fb_exchange_token&client_id=%s&client_secret=%s&fb_exchange_token=%s" format(appId, appSecret, accessToken)
      sender ! pipeline(Get(url))
    }

    case getAccessToken(appId: String, appSecret: String, code: String, redirectUri: String) => {
      //TODO this is basically the same as extendToken() request, just different query params, so extract some reusable function
      val pipeline: HttpRequest => Future[AccessToken] = (
        sendReceive
          ~> mapErrors
          ~> unmarshal[AccessToken]
        )
      val url = httpUrl + "/oauth/access_token?client_id=%s&client_secret=%s&code=%s&redirect_uri=%s" format(appId, appSecret, code, redirectUri)
      sender ! pipeline(Get(url))
    }


    case newPhotos(accessToken: String, after: Option[String]) => {
      import system.dispatcher

      val pipeline: HttpRequest => Future[Response[Photo]] = (
        addHeader("Authorization", "Bearer " + accessToken)
          ~> addHeader("Accept", "application/json")
          ~> sendReceive
          ~> mapErrors
          ~> unmarshal[Response[Photo]]
        )
      //TODO should really get multiple recent photos, then check IDs for previous processing, dates for recency, etc
      //there have been cases where we miss photos because this query only gets the 1 most recent photo
      val url = httpUrl + "/me/photos/uploaded?fields=id,name,images,place,tags&" + (after.map(a => "after=" + a).getOrElse("limit=1"))
      sender ! pipeline(Get(url))
    }

    //This works now, via manual testing
    case getUser(accessToken: String) => {
      val pipeline: HttpRequest => Future[User] = (

        addHeader("Authorization", "Bearer " + accessToken)
          ~> addHeader("Accept", "application/json")
          ~> sendReceive
          ~> mapErrors
          ~> unmarshal[User]
        )
      sender ! pipeline(Get(httpUrl + "/me?fields=%s" format userFieldParams))
    }

    case getPage(pageId: String) => {
      val pipeline: HttpRequest => Future[Page] = (

        addHeader("Accept", "application/json")
          ~> sendReceive
          ~> mapErrors
          ~> unmarshal[Page]
        )
      sender ! pipeline(Get(httpUrl + "/" + pageId))
    }

    case getTab(pageId: String, appId: String, token: String) => {
      val pipeline: HttpRequest => Future[Response[Tab]] = (

        addHeader("Authorization", "Bearer " + token)
          ~> addHeader("Accept", "application/json")
          ~> sendReceive
          ~> mapErrors
          ~> unmarshal[Response[Tab]]

        )
      sender ! pipeline(Get(httpUrl + "/%s/tabs/%s" format(pageId, appId)))
    }

    //This works now, via manual testing
    case createStory(action: String, objectName: String, objectId: String, imageUrl: String, message: Option[String], accessToken: String) => {
      val pipeline: HttpRequest => Future[CreatedStory] = (

        addHeader("Authorization", "Bearer " + accessToken)
          ~> addHeader("Accept", "application/json")
          ~> sendReceive
          ~> mapErrors
          ~> unmarshal[CreatedStory]
        )
      val data = List(
        objectName -> objectId,
        "image[0][url]" -> imageUrl,
        "image[0][user_generated]" -> "true"
      ) ++ (message.toList.map(m => "message" -> m))
      sender ! pipeline(Post(httpUrl + "/me/" + action, FormData(data.toMap)))
    }

    case createComment(photoId: String, message: String, accessToken: String) => {
      val pipeline: HttpRequest => Future[CreatedComment] = (

        addHeader("Authorization", "Bearer " + accessToken)
          ~> addHeader("Accept", "application/json")
          ~> sendReceive
          ~> mapErrors
          ~> unmarshal[CreatedComment]
        )
      sender ! pipeline(Post(httpUrl + "/%s/comments" format photoId, FormData(Map("message" -> message))))
    }

    case getFriends(accessToken: String) => {
      val pipeline: HttpRequest => Future[FacebookFriends] = (

        addHeader("Authorization", "Bearer " + accessToken)
          ~> addHeader("Accept", "application/json")
          ~> sendReceive
          ~> mapErrors
          ~> unmarshal[FacebookFriends]
        )
      sender ! pipeline(Get(httpUrl + "/me/friends?fields=%s" format userFieldParams)).map(_.data)
    }

    case getComments(objectId: String, accessToken: String) => {
      val pipeline: HttpRequest => Future[Response[Comment]] = (

        addHeader("Authorization", "Bearer " + accessToken)
          ~> addHeader("Accept", "application/json")
          ~> sendReceive
          ~> mapErrors
          ~> unmarshal[Response[Comment]]
        )
      sender ! pipeline(Get(httpUrl + "/%s/comments" format objectId)).map(_.data)
    }

    case getLikes(objectId: String, accessToken: String) => {
      val pipeline: HttpRequest => Future[Response[User]] = (

        addHeader("Authorization", "Bearer " + accessToken)
          ~> addHeader("Accept", "application/json")
          ~> sendReceive
          ~> mapErrors
          ~> unmarshal[Response[User]]
        )
      sender ! pipeline(Get(httpUrl + "/%s/likes" format objectId)).map(_.data)
    }

    case getSharedPosts(objectId: String, accessToken: String) => {
      val pipeline: HttpRequest => Future[Response[Share]] = (

        addHeader("Authorization", "Bearer " + accessToken)
          ~> addHeader("Accept", "application/json")
          ~> sendReceive
          ~> mapErrors
          ~> unmarshal[Response[Share]]
        )
      sender ! pipeline(Get(httpUrl + "/%s/sharedposts" format objectId)).map(_.data)
    }

    //Insight stuff
    case getApplicationOpenGraphActionCreate(appId: String, accessToken: String, since: Long, until: Long) => {
      val pipeline: HttpRequest => Future[Response[Insight]] = (

        addHeader("Authorization", "Bearer " + accessToken)
          ~> addHeader("Accept", "application/json")
          ~> sendReceive
          ~> mapErrors
          ~> unmarshal[Response[Insight]]
        )
      sender ! pipeline(Get(httpUrl + "/%s/insights/application_opengraph_action_create?since=%s&until=%s" format(appId, since, until))).map(_.data)
    }

    case getApplicationOpenGraphActionClick(appId: String, accessToken: String, since: Long, until: Long) => {
      val pipeline: HttpRequest => Future[Response[Insight]] = (

        addHeader("Authorization", "Bearer " + accessToken)
          ~> addHeader("Accept", "application/json")
          ~> sendReceive
          ~> mapErrors
          ~> unmarshal[Response[Insight]]
        )
      sender ! pipeline(Get(httpUrl + "/%s/insights/application_opengraph_story_click?since=%s&until=%s" format(appId, since, until))).map(_.data)
    }

    case getApplicationOpenGraphActionImpressions(appId: String, accessToken: String, since: Long, until: Long) => {
      val pipeline: HttpRequest => Future[Response[Insight]] = (

        addHeader("Authorization", "Bearer " + accessToken)
          ~> addHeader("Accept", "application/json")
          ~> sendReceive
          ~> mapErrors
          ~> unmarshal[Response[Insight]]
        )
      sender ! pipeline(Get(httpUrl + "/%s/insights/application_opengraph_story_impressions?since=%s&until=%s" format(appId, since, until))).map(_.data)
    }
  }

  val mapErrors = (response: HttpResponse) => {
    import Exceptions._
    if (response.status.isSuccess) response
    else {

      // https://developers.facebook.com/docs/reference/api/errors/
      response.entity.asString.asJson.convertTo[ErrorResponse].error match {
        case e if e.error_subcode == Some(458) => throw DeAuthorizedException(e.message, e.`type`, e.code, e.error_subcode)
        case e if e.error_subcode == Some(459) => throw NoSessionException(e.message, e.`type`, e.code, e.error_subcode)
        case e if e.error_subcode == Some(460) => throw PasswordChangedException(e.message, e.`type`, e.code, e.error_subcode)
        case e if e.error_subcode == Some(463) => throw AccessTokenExpiredException(e.message, e.`type`, e.code, e.error_subcode)
        case e if e.error_subcode == Some(464) => throw NoSessionException(e.message, e.`type`, e.code, e.error_subcode)
        case e if e.error_subcode == Some(467) => throw InvalidAccessTokenException(e.message, e.`type`, e.code, e.error_subcode)

        case e if e.code == 10 || e.code > 199 && e.code < 300 => throw new FacebookPermissionException(e.message, e.`type`, e.code, e.error_subcode)

        // TODO: extend to cover the most common exceptions
        case e => throw FacebookException(e.message, e.`type`, e.code, e.error_subcode)
      }
    }
  }

}
