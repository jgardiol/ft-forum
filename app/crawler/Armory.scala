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

    val response = getResponse(name)

    implicit val context = scala.concurrent.ExecutionContext.Implicits.global

    getResponse(name) onComplete {
      case Success(response) => {
        if (response.status == 200) {
          val guildname = (response.json \ "guild" \ "name").asOpt[String].getOrElse("")
          val playername = (response.json \ "name").asOpt[String]

          playername match {
            case Some(_) => {
              PlayerInfo.getByName(name) match {
                case Some(player) => if (player.guild != guildname) PlayerInfo.updateGuild(name, guildname)
                case None => PlayerInfo.create(name, guildname)
              }
            }
            case None => {
              // No player name means the player isn't on the server
              PlayerInfo.getByName(name).map(p => PlayerInfo.delete(p.id))
            }
          }

        } else if (response.status == 404) {
          PlayerInfo.getByName(name).map(p => PlayerInfo.delete(p.id))
        } else {
          try {
            val message = (response.json \ "reason").as[String]
            Logger.warn("Error: " + message)
          } catch {
            case e: Throwable => Logger.warn("woopsie! (for " + name + ") " + e.getMessage())
          }
        }
      }
      case Failure(t) => Logger.error("Error while getting info for " + name + ", message: " + t.getMessage())
    }
  }

  def processPlayers(names: List[String]) {
    for (name <- names) {
      processPlayer(name)
      Thread.sleep(1000)
    }
  }
}