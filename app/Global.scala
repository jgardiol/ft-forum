import play.api._
import play.api.mvc._
import models._

object Global extends GlobalSettings {
  override def onStart(app: Application) {
    if(Forum.all.isEmpty) DummyData.insert()
  }
}

object DummyData {
  def insert() {
    User.create("Admin", "password", "", "admin")
  }
}