package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

import org.mindrot.jbcrypt.BCrypt

case class User(id: Long, name: String, password: String, main: String, role: String)

object User {
  val simple = {
    get[Long]("id") ~
      get[String]("name") ~
      get[String]("password") ~
      get[String]("main") ~
      get[String]("role") map {
        case id ~ name ~ password ~ main ~ role => User(id, name, password, main, role)
      }
  }

  def all(): List[User] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM users").as(simple*)
    }
  }

  // Returns a user matching the name and password, if it exists.
  def authenticate(name: String, password: String): Option[User] = {
    getByName(name) match {
      case Some(user) => {
        if (BCrypt.checkpw(password, user.password))
          Some(user)
        else
          None
      }
      case None => None
    }
  }

  // Adds a user to the database
  def create(name: String, password: String, main: String, role: String): Option[User] = {
    // Check if username is available
    getByName(name) match {
      case Some(user) => None
      case None => {
        val hashed = BCrypt.hashpw(password, BCrypt.gensalt())

        DB.withConnection { implicit c =>
          SQL("INSERT INTO users(name, password, main, role) VALUES({name}, {password}, {main}, {role})").on(
            'name -> name,
            'password -> hashed,
            'main -> main,
            'role -> role).executeUpdate()
        }

        getByName(name)
      }
    }
  }

  def update(id: Long, main: String, role: String) = {
    DB.withConnection { implicit c =>
      SQL("UPDATE users SET main={main}, role={role} WHERE id={id}").on(
        'id -> id,
        'main -> main,
        'role -> role).executeUpdate()
    }
  }

  def getById(id: Long): Option[User] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM users WHERE users.id = {id}").on(
        'id -> id).as(simple.singleOpt)
    }
  }

  // Gets a user matching the name, if it exists
  def getByName(name: String): Option[User] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM users WHERE LOWER(name) = LOWER({name})").on(
        'name -> name).as(simple.singleOpt)
    }
  }

}