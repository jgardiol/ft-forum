import play.api._
import play.api.mvc._
import models._
import models.ranking._
import crawler.CrawlInfo

object Global extends GlobalSettings {
  override def onStart(app: Application) {
    if(Forum.all.isEmpty) createAdmin()
    if(Boss.all.isEmpty) createBosses()
    if(Forum.all.isEmpty) createForums()
    if(CrawlInfo.all.isEmpty) createInfo()
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
    User.create("Admin", "password", "")
    User.update(1, "", Role.getById(0).get)
  }
  
  def createForums() {
    Forum.create("Général", "Discussion Générale")
    Forum.create("Recrutement", "On recrute")
  }
  
  def createInfo() {
    CrawlInfo.create(new java.util.Date)
  }
}