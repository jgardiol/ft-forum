import play.api._
import play.api.mvc._
import models._
import models.ranking._

object Global extends GlobalSettings {
  override def onStart(app: Application) {
    if(Forum.all.isEmpty) createAdmin()
    if(Boss.all.isEmpty) createBosses()
  }
  
  def createBosses() {
    // Create some objects
    for(boss <- controllers.Ranking.bosses) {
      for(difficulty <- controllers.Ranking.difficulties) {
        Boss.create(boss, difficulty)
      }
    }
  }
  
  def createAdmin() {
    User.create("Admin", "password", "", "admin")
  }
}