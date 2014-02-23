package crawler

import play.api.libs.ws._
import scala.concurrent.Future
import play.api.libs.json._

import models.ranking._

object Armory {

  // See http://blizzard.github.io/api-wow-docs/#character-profile-api/guild
  val URL = "/api/wow/character/illidan/CHAR_NAME"

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
    
    futureResult.map(value => Logger.info("guildname: " + value))

    Logger.info("Done processing " + name)
  }

}