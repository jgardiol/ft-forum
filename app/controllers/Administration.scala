package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Future

import models._

object Administration extends Utils {

  def admin = Action { implicit request =>
    user match {
      case Some(user) => {
        user.role match {
          case "admin" => WithUri(views.html.admin(User.all(), Some(user)))
          case _ => Redirect(routes.Application.index).flashing("error" -> "Vous n'avez pas le droit d'accéder à cette ressource.")
        }
      }
      case None => Redirect(routes.Application.index).flashing("error" -> "Vous n'avez pas le droit d'accéder à cette ressource.")
    }
  }

  def forumadmin = Action { implicit request =>
    user match {
      case Some(user) => {
        user.role match {
          case "admin" => WithUri(views.html.forumadmin(Forum.all(), Some(user)))
          case _ => unauthedAction
        }
      }
      case None => unauthedAction
    }
  }

  val forumForm = Form(tuple("name" -> nonEmptyText, "description" -> nonEmptyText))

  def createForum = Action { implicit request =>
    user match {
      case Some(user) => {
        user.role match {
          case "admin" => {
            forumForm.bindFromRequest.fold(
              formWithErrors => {
                Redirect(routes.Administration.forumadmin).flashing("error" -> "Formulaire incorrect.")
              },
              data => {
                Forum.create(data._1, data._2)
                Redirect(routes.Administration.forumadmin).flashing("success" -> "Forum créé.")
              })
          }
          case _ => unauthedAction
        }
      }
      case None => unauthedAction
    }
  }

  def updateForum(id: Long) = Action { implicit request =>
    user match {
      case Some(user) => {
        user.role match {
          case "admin" => {
            forumForm.bindFromRequest.fold(
              formWithErrors => {
                Redirect(routes.Administration.forumadmin).flashing("error" -> "Formulaire incorrect.")
              },
              data => {
                Forum.update(id, data._1, data._2)
                Redirect(routes.Administration.forumadmin).flashing("success" -> "Forum bien mis à jour.")
              })
          }
          case _ => unauthedAction
        }
      }
      case None => unauthedAction
    }
  }

  def deleteForum(id: Long) = Action { implicit request =>
    user match {
      case Some(user) => {
        user.role match {
          case "admin" => {
            Forum.delete(id)
            Redirect(routes.Administration.forumadmin).flashing("success" -> "Forum supprimé.")
          }
          case _ => unauthedAction
        }
      }
      case None => unauthedAction
    }
  }

  val userForm = Form(tuple("main" -> text, "role" -> text))

  def updateUser(id: Long) = Action { implicit request =>
    user match {
      case Some(user) => {
        user.role match {
          case "admin" => {
            userForm.bindFromRequest.fold(
              formWithErrors => {
                Redirect(routes.Administration.admin).flashing("error" -> "Formulaire incorrect.")
              },
              data => {
                User.update(id, data._1, data._2)
                Redirect(routes.Administration.admin).flashing("success" -> "Utilisateur bien mis à jour.")
              })
          }
          case _ => unauthedAction
        }
      }
      case None => unauthedAction
    }
  }
}
