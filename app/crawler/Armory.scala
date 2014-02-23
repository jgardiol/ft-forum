package crawler

import play.api.libs.ws._
import scala.concurrent.Future
import scala.util.{ Success, Failure }
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

    getResponse(name) onComplete {
      case Success(response) => {
        Logger.info("status: " + response.status)
        if (response.status == 200) {
          val guildname = (response.json \ "guild" \ "name").as[String]
          Logger.info("guildname: " + guildname)
        }
      }
      case Failure(t) => Logger.error("Error while getting info for " + name + ", message: " + t.getMessage())
    }
  }

  // For each player, get guild name.
  // On error:
  // - 404: remove player from DB
  // - 5xx: log, ignore, retry later

}