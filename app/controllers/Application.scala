package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Future

import models._

object Application extends Utils {
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
