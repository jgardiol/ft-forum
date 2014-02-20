package models

import java.util.{ Date }

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current

case class Post(id: Long, content: String, created: Date, threadId: Long, userId: Long, modified: Option[Date], modifiedBy: Option[Long])

object Post {

  val simple = {
    get[Long]("id") ~
      get[String]("content") ~
      get[Date]("created") ~
      get[Long]("thread_id") ~
      get[Long]("user_id") ~
      get[Option[Date]]("modified") ~ 
      get[Option[Long]]("modified_by") map {
        case id ~ content ~ created ~ thread_id ~ user_id ~ modified ~ modified_by => 
          Post(id, content, created, thread_id, user_id, modified, modified_by)
      }
  }

  def create(content: String, threadId: Long, userId: Long) {
    DB.withConnection { implicit c =>
      SQL("INSERT INTO posts(content, created, thread_id, user_id) VALUES({content}, {created}, {thread_id}, {user_id})").on(
        'content -> content,
        'created -> new java.util.Date,
        'thread_id -> threadId,
        'user_id -> userId).executeUpdate()
    }
  }

  def getById(id: Long): Option[Post] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM posts WHERE id={id}").on('id -> id).as(simple.singleOpt)
    }
  }

  def isOwner(id: Long, userId: Long): Boolean = {
    getById(id) match {
      case Some(post) => post.userId == userId
      case None => false
    }
  }

  def update(id: Long, content: String, userId: Long) {
    DB.withConnection { implicit c =>
      SQL("UPDATE posts SET content={content}, modified={modified}, modified_by={user_id} WHERE id={id}").on(
        'id -> id, 
        'content -> content,
        'modified -> new java.util.Date(),
        'user_id -> userId).executeUpdate()
    }
  }
  
  def delete(id: Long) {
    DB.withConnection { implicit c => 
     SQL("DELETE FROM posts WHERE id={id}").on('id -> id).executeUpdate()  
    }
  }

}