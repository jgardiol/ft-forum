package models

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current

case class Thread(id: Long, title: String, userId: Long, forumId: Long)

case class ThreadInfo(id: Long, numPosts: Long, lastAuthor: String, lastCreated: java.util.Date)

object Thread {
  val simple = {
    get[Long]("id") ~
      get[String]("title") ~
      get[Long]("user_id") ~
      get[Long]("forum_id") map {
        case id ~ title ~ user_id ~ forum_id => Thread(id, title, user_id, forum_id)
      }
  }

  def create(title: String, userId: Long, forumId: Long): Long = {
    DB.withConnection { implicit c =>
      SQL("INSERT INTO threads(title, user_id, forum_id) VALUES ({title}, {userId}, {forumId})").on(
        'title -> title,
        'userId -> userId,
        'forumId -> forumId).executeUpdate()
        
        
      SQL("SELECT * FROM threads WHERE user_id = {user_id} AND forum_id = {forum_id} ORDER BY id DESC").on(
          'user_id -> userId,
          'forum_id -> forumId).as(simple *).head.id
    }
  }

  def getPosts(threadId: Long): List[Post] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM posts WHERE post.thread_id = {thread_id} ORDER BY created ASC").on(
          'thread_id -> threadId).as(Post.simple *)
    }
  }
  
  def getPosts(threadId: Long, page: Int, pageSize: Int): List[Post] = {
    val offset = (page-1) * pageSize

    DB.withConnection { implicit c =>
      SQL("""
          SELECT *
          FROM posts
          WHERE posts.thread_id = {thread_id}
          ORDER BY posts.created ASC
          LIMIT {pageSize} OFFSET {offset}
          """).on(
        'thread_id -> threadId,
        'pageSize -> pageSize,
        'offset -> offset).as(Post.simple*)
    }
  }
  
  def getById(id: Long): Option[Thread] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM threads WHERE threads.id = {id}").on(
          'id -> id).as(simple.singleOpt)
      
    }
  }
  
  def getLastPost(threadId: Long): Post = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM posts WHERE thread_id = {thread_id} ORDER BY posts.created DESC").on(
          'thread_id -> threadId).as(Post.simple *).head
      }
  }
  
  def numPosts(id: Long): Long = {
    DB.withConnection { implicit c =>
      SQL("""
          SELECT COUNT(*) AS num_posts
          FROM posts
          WHERE thread_id = {id}
          """).on(
              'id -> id).as(get[Long]("num_posts").single)
      }
  }
  
  def getThreadInfo(id: Long): ThreadInfo = {
    val lastPost = Thread.getLastPost(id)
    ThreadInfo(id, numPosts(id), User.getById(lastPost.userId).get.name, lastPost.created)
  }

}