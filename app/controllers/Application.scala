package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Future

import models._

case class Paging(current: Int, numPages: Int)

object Application extends Controller {

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

  def index = Action { implicit request =>
    val forums = Forum.all() map { forum =>
      (forum, Forum.getInfo(forum.id))
    }
    
    WithUri(views.html.index(forums, user))
  }
  

  def forum(id: Long, page: Int = 0) = Action { implicit request =>
    Forum.getById(id) match {
      case Some(forum) => {
        val threads = Forum.getThreads(id, page, pageSize).map(t => (t, Thread.getThreadInfo(t.id)))
        val paging = Paging(page, (Forum.numThreads(id).toInt - 1) / pageSize)
        WithUri(views.html.forum(forum, threads, paging, user))
      }
      case None => NotFound
    }
  }

  def thread(id: Long, page: Int = 0) = Action { implicit request =>
    Thread.getById(id) match {
      case Some(thread) => {
        val forum = Forum.getById(thread.forumId).get
        val posts = Thread.getPosts(id, page, pageSize) map { post => (post, User.getById(post.userId).get) }
        val paging = Paging(page, (Thread.numPosts(id).toInt - 1) / pageSize)
        WithUri(views.html.thread(forum, thread, posts, paging, user))
      }
      case None => NotFound
    }
  }

  val loginForm = Form(
    tuple(
      "name" -> nonEmptyText,
      "password" -> nonEmptyText))

  def login = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => {
        Redirect(currentUri).flashing("error" -> "Vous devez saisir un nom d'utilisateur et un mot de passe.")
      },
      userData => {
        val user = User.authenticate(userData._1, userData._2)
        user match {
          case Some(user) => Redirect(currentUri).withSession("username" -> user.name)
          case None => Redirect(currentUri).flashing("error" -> "Mauvais nom d'utilisateur ou mot de passe")
        }
      })
  }

  val signupForm = Form(
    tuple(
      "name" -> nonEmptyText(maxLength = 16),
      "password" -> nonEmptyText(minLength = 6, maxLength = 24),
      "password2" -> nonEmptyText(minLength = 6, maxLength = 24),
      "charname" -> text))

  def signup = Action { implicit request =>
    signupForm.bindFromRequest.fold(
      formWithErrors => {
        Redirect(currentUri).flashing("error" -> "Formulaire invalide, veuillez réessayer.")
      },
      data => {
        if (data._2 != data._3)
          Redirect(currentUri).flashing("error" -> "Les mots de passe ne correspondent pas.")
        else {
          val user = User.create(data._1, data._2, data._4, "-")
          user match {
            case Some(user) => {
              Redirect(currentUri).withSession("username" -> user.name).flashing("success" -> "Compte créé.")
            }
            case None => Redirect(currentUri).flashing("error" -> "Ce nom d'utilisateur existe déjà")
          }
        }
      })
  }

  def logout = Action { implicit request =>
    Redirect(currentUri).withNewSession
  }

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
                Redirect(routes.Application.forumadmin).flashing("error" -> "Formulaire incorrect.")
              },
              data => {
                Forum.create(data._1, data._2)
                Redirect(routes.Application.forumadmin).flashing("success" -> "Forum créé.")
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
                Redirect(routes.Application.forumadmin).flashing("error" -> "Formulaire incorrect.")
              },
              data => {
                Forum.update(id, data._1, data._2)
                Redirect(routes.Application.forumadmin).flashing("success" -> "Forum bien mis à jour.")
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
            Redirect(routes.Application.forumadmin).flashing("success" -> "Forum supprimé.")
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
                Redirect(routes.Application.admin).flashing("error" -> "Formulaire incorrect.")
              },
              data => {
                User.update(id, data._1, data._2)
                Redirect(routes.Application.admin).flashing("success" -> "Utilisateur bien mis à jour.")
              })
          }
          case _ => unauthedAction
        }
      }
      case None => unauthedAction
    }
  }

  val newthreadForm = Form(
    tuple(
      "title" -> nonEmptyText,
      "content" -> nonEmptyText))

  def newthread(forumId: Long) = Action { implicit request =>
    user match {
      case Some(user) => {
        newthreadForm.bindFromRequest.fold(
          formWithErrors => {
            Redirect(routes.Application.forum(forumId, 1)).flashing("error" -> "Vous devez remplir les champs !")
          },
          threadData => {
            val threadId = Thread.create(threadData._1, user.id, forumId)
            Post.create(threadData._2, new java.util.Date(), threadId, user.id)
            Redirect(routes.Application.thread(threadId, 1)).flashing("success" -> "Votre sujet a bien été créé.")
          })
      }
      case None => unauthedAction
    }
  }

  val newpostForm = Form(single("content" -> nonEmptyText))

  def newpost(threadId: Long) = Action { implicit request =>
    user match {
      case Some(user) => {
        newpostForm.bindFromRequest.fold(
          formWithErrors => {
            Redirect(routes.Application.thread(threadId, 1)).flashing("error" -> "Vous devez remplir les champs !")
          },
          postData => {
            Post.create(postData, new java.util.Date(), threadId, user.id)
            Redirect(routes.Application.thread(threadId, 1)).flashing("success" -> "Votre post a bien été créé.")
          })
      }
      case None => Unauthorized("Vous devez être connecté!")
    }
  }
}
