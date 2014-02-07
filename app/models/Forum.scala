package models

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current

case class Forum(id: Long, name: String, description: String)

case class ForumInfo(id: Long, numThreads: Long, lastThread: Option[Thread], lastThreadAuthor: Option[User], created: java.util.Date)

object Forum {
  def all(): List[Forum] = DB.withConnection { implicit c =>
    SQL("SELECT * FROM forums").as(forum *)
  }

  def getById(id: Long): Option[Forum] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM forums WHERE id = {id}").on(
        'id -> id).as(forum.singleOpt)
    }
  }

  def create(name: String, description: String) {
    DB.withConnection { implicit c =>
      SQL("INSERT INTO forums(name, description) VALUES ({name}, {description})").on(
        'name -> name,
        'description -> description).executeUpdate()
    }
  }
  
  def update(id: Long, name: String, description: String) {
    DB.withConnection { implicit c =>
      SQL("UPDATE forums SET name={name}, description={description} WHERE id={id}").on(
        'id -> id,
        'name -> name,
        'description -> description).executeUpdate()
    }
  }

  def delete(id: Long) {
    DB.withConnection { implicit c =>
      SQL("DELETE FROM forums WHERE id={id}").on(
        'id -> id).executeUpdate()
    }
  }

  def getThreads(id: Long, page: Int, pageSize: Int): List[Thread] = {
    val offset = (page-1) * pageSize

    DB.withConnection { implicit c =>
      SQL("""
          SELECT t.*
          FROM threads t 
          INNER JOIN posts p
    		  ON t.id = p.thread_id
          INNER JOIN
          (
    		  SELECT thread_id, MAX(created) last
    		  FROM posts
    		  GROUP BY thread_id
          ) temp ON p.thread_id = temp.thread_id AND p.created = temp.last
          WHERE t.forum_id = {id}
          ORDER BY temp.last DESC
          LIMIT {pageSize} OFFSET {offset}
          """).on(
        'id -> id,
        'pageSize -> pageSize,
        'offset -> offset).as(Thread.simple*)
    }
  }

  def getThreads(id: Long): List[Thread] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM threads WHERE forum_id = {id}").on(
        'id -> id).as(Thread.simple *)
    }
  }

  def getLastThread(forumId: Long): Option[Thread] = {
    DB.withConnection { implicit c =>
      SQL("""
          SELECT threads.id, threads.title, threads.user_id, threads.forum_id 
          FROM threads INNER JOIN posts ON threads.id = posts.thread_id 
          WHERE threads.forum_id = {forum_id}
          ORDER BY posts.created DESC
          """).on(
        'forum_id -> forumId).as(Thread.simple *).headOption
    }
  }

  def numThreads(id: Long): Long = {
    DB.withConnection { implicit c =>
      SQL("""
          SELECT COUNT(*) AS num_threads
          FROM threads
          WHERE forum_id = {id}
          """).on(
        'id -> id).as(get[Long]("num_threads").single)
    }
  }

  def getInfo(id: Long): ForumInfo = {
    val num = numThreads(id)
    val lastThread = getLastThread(id)

    lastThread match {
      case Some(thread) => {
        val created = Thread.getLastPost(thread.id).created
        ForumInfo(id, num, Some(thread), User.getById(thread.userId), created)
      }
      case None => ForumInfo(id, num, None, None, new java.util.Date())
    }
  }

  val forum = {
    get[Long]("id") ~
      get[String]("name") ~
      get[String]("description") map {
        case id ~ name ~ description => Forum(id, name, description)
      }
  }
}