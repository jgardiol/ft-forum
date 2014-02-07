package models

import java.util.{Date}

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current

case class Post(id: Long, content: String, created: Date, threadId: Long, userId: Long)

object Post {
  
  val simple = {
    get[Long]("id")~
    get[String]("content")~
    get[Date]("created")~
    get[Long]("thread_id")~
    get[Long]("user_id") map {
      case id~content~created~thread_id~user_id => Post(id, content, created, thread_id, user_id)
    }
  }
  
  def create(content: String, created: Date, threadId: Long, userId: Long) {
    DB.withConnection { implicit c =>
      SQL("INSERT INTO posts(content, created, thread_id, user_id) VALUES({content}, {created}, {thread_id}, {user_id})").on(
          'content -> content,
          'created -> created,
          'thread_id -> threadId,
          'user_id -> userId).executeUpdate()
    }
  }

}