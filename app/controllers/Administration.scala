package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import play.api.libs.iteratee.Enumerator
import play.api.libs.Comet

import models._
import crawler._

object Administration extends Utils {

  var isCrawling = false

  def admin = AdminAction { user =>
    implicit request =>
      WithUri(views.html.admin(User.all(), Some(user)))
  }

  def forumadmin = AdminAction { user =>
    implicit request =>
      WithUri(views.html.forumadmin(Forum.all(), Some(user)))
  }

  val forumForm = Form(tuple("name" -> nonEmptyText, "description" -> nonEmptyText))

  def createForum = AdminAction { user =>
    implicit request =>
      forumForm.bindFromRequest.fold(
        formWithErrors => {
          Redirect(routes.Administration.forumadmin).flashing("error" -> "Formulaire incorrect.")
        },
        data => {
          Forum.create(data._1, data._2)
          Redirect(routes.Administration.forumadmin).flashing("success" -> "Forum créé.")
        })
  }

  def updateForum(id: Long) = AdminAction { user =>
    implicit request =>
      forumForm.bindFromRequest.fold(
        formWithErrors => {
          Redirect(routes.Administration.forumadmin).flashing("error" -> "Formulaire incorrect.")
        },
        data => {
          Forum.update(id, data._1, data._2)
          Redirect(routes.Administration.forumadmin).flashing("success" -> "Forum bien mis à jour.")
        })
  }

  def deleteForum(id: Long) = AdminAction { user =>
    implicit request =>
      Forum.delete(id)
      Redirect(routes.Administration.forumadmin).flashing("success" -> "Forum supprimé.")
  }

  val userForm = Form(tuple("main" -> text, "role" -> number))

  def updateUser(id: Long) = AdminAction { user =>
    implicit request =>
      userForm.bindFromRequest.fold(
        formWithErrors => {
          Redirect(routes.Administration.admin).flashing("error" -> "Formulaire incorrect.")
        },
        data => {
          User.update(id, data._1, Role.getById(data._2).getOrElse(Role.Default))
          Redirect(routes.Administration.admin).flashing("success" -> "Utilisateur bien mis à jour.")
        })
  }

  def ranking = AdminAction { user =>
    implicit request =>
      WithUri(views.html.ranking.rankingadmin(CrawlError.all(), Some(user)))
  }

  def crawlStatus = Action {
    Ok.chunked(stillCrawling &> Comet(callback = "parent.stillCrawling"))
  }

  lazy val stillCrawling: Enumerator[String] = {
    import scala.concurrent.duration._

    Enumerator.generateM {
      Promise.timeout(Some(isCrawling.toString), 100 milliseconds)
    }
  }

  def crawlErrors = AdminAction { user =>
    implicit request =>
      import play.api.libs.concurrent.Execution.Implicits._
      import crawler._

      if (!isCrawling) {
        isCrawling = true

        val f = scala.concurrent.Future {
          Crawler.processErrors
        }

        f onComplete {
          case _ => {
            Logger.info("Finished processing errors")
            isCrawling = false
          }
        }
        Redirect(currentUri).flashing("success" -> "Crawl started")
      } else {
        Redirect(currentUri).flashing("error" -> "A crawl is already in progress")
      }
  }

  def startCrawl = AdminAction { _ =>
    implicit request =>
      import play.api.libs.concurrent.Execution.Implicits._
      import crawler._

      if (!isCrawling) {
        isCrawling = true

        val f = scala.concurrent.Future { Crawler.crawl }

        f onComplete {
          case _ => {
            Logger.info("crawling finished")
            isCrawling = false
          }
        }
        Redirect(currentUri).flashing("success" -> "Crawl started")
      } else {
        Redirect(currentUri).flashing("error" -> "A crawl is already in progress")
      }
  }

  def deleteError(id: Long) = AdminAction { _ =>
    implicit request =>
      CrawlError.delete(id)
      Redirect(currentUri).flashing("success" -> ("Error " + id + " deleted."))
  }
}
