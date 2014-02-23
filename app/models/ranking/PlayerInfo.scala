package models.ranking

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class PlayerInfo(id: Long, name: String, guild: String)

object PlayerInfo {
  val simple =
    get[Long]("id") ~
      get[String]("name") ~
      get[String]("guild") map {
        case id ~ name ~ guild => PlayerInfo(id, name, guild)
      }

  def all(): List[PlayerInfo] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM player_info").as(simple*)
    }
  }

  def getById(id: Long): Option[PlayerInfo] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM player_info WHERE id={id}").on('id -> id).as(simple.singleOpt)
    }
  }

  def getByName(name: String): Option[PlayerInfo] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM player_info WHERE LOWER(name)=LOWER({name})").on('name -> name).as(simple.singleOpt)
    }
  }

  def create(name: String, guild: String) {
    DB.withConnection { implicit c =>
      SQL("""INSERT INTO player_info(name, guild)
    		 SELECT {name}, {guild}
    		 WHERE NOT EXISTS (SELECT * FROM player_info WHERE LOWER(name)=LOWER({name}) AND guild={guild})
        """).on('name -> name, 'guild -> guild).executeUpdate()
    }
  }

  def updateGuild(name: String, guild: String) {
    DB.withConnection { implicit c =>
      SQL("UPDATE player_info SET guild={guild} WHERE LOWER(name)=LOWER({name})").on(
        'name -> name,
        'guild -> guild).executeUpdate()
    }
  }

  def delete(id: Long) {
    DB.withConnection { implicit c =>
      SQL("DELETE FROM player_info WHERE id={id}").on('id -> id).executeUpdate()
    }
  }
}