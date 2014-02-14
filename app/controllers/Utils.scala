package controllers

import play.api._
import play.api.mvc._

import models._

case class Paging(current: Int, numPages: Int)

trait Utils extends Controller {
  def user(implicit request: RequestHeader) = {
    request.session.get("username") match {
      case Some(name) => User.getByName(name)
      case None => None
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

}