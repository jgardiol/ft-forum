package controllers

import play.api._
import play.api.mvc._

import models._

case class Paging(current: Int, numPages: Int)

trait Utils extends Controller {
  def user(request: RequestHeader) = {
    request.session.get("username") match {
      case Some(name) => User.getByName(name)
      case None => Some(User.Guest)
    }
  }

  def currentUri(implicit request: RequestHeader) = {
    request.session.get("uri").getOrElse("/")
  }

  def WithUri(content: play.api.templates.HtmlFormat.Appendable)(implicit request: RequestHeader) = {
    request.session.get("username") match {
      case Some(name) => Ok(content).withSession("uri" -> request.uri, "username" -> name)
      case None => Ok(content).withSession("uri" -> request.uri)
    }
  }

  val pageSize = 10

  val unauthedAction = Redirect(routes.Application.index).flashing("error" -> "Vous n'avec pas le droit de faire cela.")

  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.index).flashing("error" -> "Vous n'avec pas le droit de faire cela.")

  def IsAuthenticated(f: => User => Request[AnyContent] => Result) =
    Security.Authenticated(user, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }

  def AdminAction(action: => User => Request[AnyContent] => Result) = IsAuthenticated { user =>
    request =>
      if (Role.isAdmin(user.roleId))
        action(user)(request)
      else
        unauthedAction
  }

  def ReadAction(forumId: Long)(action: => User => Request[AnyContent] => Result) = IsAuthenticated { user =>
    request =>
      if (Role.canRead(user.roleId, forumId))
        action(user)(request)
      else
        unauthedAction
  }

  def UpdateAction(postId: Long)(action: => User => Request[AnyContent] => Result) = IsAuthenticated { user =>
    request =>
      Post.getById(postId) match {
        case Some(post) => {
          if (Role.isModerator(user.roleId, Thread.getById(post.threadId).get.forumId)
              || post.userId == user.id) {
            action(user)(request)
          } else {
            unauthedAction
          }
        }
        case None => NotFound
      }
  }

}