package models

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current

case class Role(id: Long, title: String, isAdmin: Boolean)

object Role {
  val simple = {
    get[Long]("id") ~
      get[String]("title") ~
      get[Boolean]("is_admin") map {
        case id ~ title ~ is_admin => Role(id, title, is_admin)
      }
  }

  def all(): List[Role] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM roles").as(simple*)
    }
  }

  def create(title: String, isAdmin: Boolean) {
    DB.withConnection { implicit c =>
      SQL("INSERT INTO roles(title, is_admin) VALUES({title}, {is_admin})").on(
        'title -> title,
        'is_admin -> isAdmin).executeUpdate()
    }
  }

  def update(id: Long, title: String, isAdmin: Boolean) {
    DB.withConnection { implicit c =>
      SQL("UPDATE roles SET title={title}, is_admin={is_admin} WHERE id={id}").on(
        'id -> id, 'title -> title, 'is_admin -> isAdmin).executeUpdate()
    }
  }

  def delete(id: Long) {
    DB.withConnection { implicit c =>
      SQL("DELETE FROM roles WHERE id={id}").on('id -> id).executeUpdate()
    }
  }

  val Guest = Role(-2, "Guest", false)
  val Default = Role(-1, "", false)

  def canRead(role: Role, forumId: Long): Boolean = {
    getPermission(role.id, forumId, Permission.read)
  }

  def canWrite(role: Role, forumId: Long): Boolean = {
    getPermission(role.id, forumId, Permission.write)
  }

  def isModerator(role: Role, forumId: Long): Boolean = {
    getPermission(role.id, forumId, Permission.moderate)
  }

  def getById(id: Long): Option[Role] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM roles WHERE id={id}").on('id -> id).as(simple.singleOpt)
    }
  }

  private def getPermission(roleId: Long, forumId: Long, level: Permission.Level): Boolean = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM permissions WHERE role_id={role_id} AND forum_id={forum_id}").on(
        'role_id -> roleId,
        'forum_id -> forumId).as(Permission.simple.singleOpt)
    }.map(_.permission >= level).getOrElse(false)
  }
}

case class Permission(forumId: Long, roleId: Long, permission: Permission.Level)

object Permission {
  val simple = {
    get[Long]("forum_id") ~
      get[Long]("role_id") ~
      get[Long]("permission") map {
        case forum_id ~ role_id ~ permissions => Permission(forum_id, role_id, permissions)
      }
  }

  def all(): List[Permission] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM permissions").as(simple*)
    }
  }

  def create(forumId: Long, roleId: Long, permission: Level) {
    DB.withConnection { implicit c =>
      SQL("""INSERT INTO permissions(forum_id, role_id, permission)
          SELECT {forum_id}, {role_id}, {permission}
          WHERE NOT EXISTS
          (
    		  SELECT *
    		  FROM permissions
    		  WHERE forum_id={forum_id} AND role_id={role_id}
          )""").on(
        'forum_id -> forumId,
        'role_id -> roleId,
        'permission -> permission).executeUpdate()
    }
  }

  def update(roleId: Long, forumId: Long, permission: Level) {
    DB.withConnection { implicit c =>
      SQL("UPDATE permissions SET permission={permission} WHERE forum_id={forum_id} AND role_id={role_id}").on(
        'forum_id -> forumId,
        'role_id -> roleId,
        'permission -> permission)
    }
  }

  def delete(roleId: Long, forumId: Long) {
    DB.withConnection { implicit c =>
      SQL("DELETE FROM permissions WHERE forum_id={forum_id} AND role_id={role_id}").on(
        'forum_id -> forumId, 'role_id -> roleId).executeUpdate()
    }
  }

  type Level = Long

  val read: Level = 1
  val write: Level = 2
  val moderate: Level = 4

  val text = Map(read -> "read", write -> "write", moderate -> "moderate")
}