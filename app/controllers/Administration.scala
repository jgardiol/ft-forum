package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import crawler.CrawlError
import play.api.libs.iteratee.Enumerator
import play.api.libs.Comet
import models._

object Administration extends Utils {

  var isCrawling = false

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

  def ranking = Action { implicit request =>
    user match {
      case Some(user) => user.role match {
        case "admin" => {
          WithUri(views.html.ranking.rankingadmin(crawler.CrawlError.all(), Some(user)))
        }
        case _ => unauthedAction
      }
      case None => unauthedAction
    }
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

  def parseReport(id: Long, url: String) = Action { implicit request =>
    import play.api.libs.concurrent.Execution.Implicits._
    import crawler._

    val pattern = """^http://worldoflogs.com/reports/(.*)""".r
    val pattern(suffix) = url

    val wolId: String = suffix.split("/").head

    user match {
      case Some(user) => user.role match {
        case "admin" => {
          if (!isCrawling) {
            isCrawling = true

            val f = scala.concurrent.Future {
              Crawler.processReport(Crawler.Report(wolId, new java.util.Date))
            }

            f onComplete {
              case _ => {
                CrawlError.delete(id)
                isCrawling = false
              }
            }
            Redirect(currentUri).flashing("success" -> "Crawl started")
          } else {
            Redirect(currentUri).flashing("error" -> "A crawl is already in progress")
          }

        }
        case _ => unauthedAction
      }
      case None => unauthedAction
    }
  }

  def startCrawl = Action { implicit request =>
    import play.api.libs.concurrent.Execution.Implicits._
    import crawler._

    user match {
      case Some(user) => user.role match {
        case "admin" => {
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
        case _ => unauthedAction
      }
      case None => unauthedAction
    }
  }
}
