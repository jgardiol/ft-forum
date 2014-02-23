package crawler

import play.api.libs.ws._
import scala.concurrent.Future
import scala.util.{Success, Failure}
import play.api.libs.json._

import models.ranking._

object Armory {

  // See http://blizzard.github.io/api-wow-docs/#character-profile-api/guild
  val URL = "http://eu.battle.net/api/wow/character/illidan/CHAR_NAME"

  private def getResponse(charname: String) =
    WS.url(URL.replaceAll("CHAR_NAME", charname)).withQueryString("fields" -> "guild").get()

  def processPlayer(name: String) {
    import play.api._
    Logger.info("Processing " + name)

    val response = getResponse(name)

    implicit val context = scala.concurrent.ExecutionContext.Implicits.global

    val futureResult: Future[String] = getResponse(name).map {
      response => (response.json \ "guild" \ "name").as[String]
    }
    
    futureResult onComplete {
      case Success(guildname) => Logger.info("guildname: " + guildname)
      case Failure(t) => Logger.error("Error while getting info for " + name + ", message: " + t.getMessage())
    }

    Logger.info("Done processing " + name)
  }

}