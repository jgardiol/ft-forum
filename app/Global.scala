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
    User.create("Swamplord", "password", "Swamplord", "admin")
    User.create("Healena", "password", "Swamplord", "officier")

    Forum.create("Général", "Forum de discussion générale")
    Forum.create("Recrutement", "On recrute!")
    Forum.create("World of Warcraft", "Strats, opti, guilde, etc...")

    Thread.create("Salut tout le monde", 1, 1)
    Thread.create("C'est vraiment de la merde ce forum", 1, 1)
    
    for(num <- 1 to 99) {
      Thread.create("Flood", 1, 2)
      Post.create("meh", new java.util.Date(), num + 2, 2)
    }

    Post.create("Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
      new java.util.Date(), 1, 1)

    Post.create("Lorem ipsum dolor sit amet, consectetur adipisicing elit",
      new java.util.Date(), 1, 1)

    Post.create("Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. \n\nLorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
      new java.util.Date(), 1, 1)
      
    for(num <- 1 to 30) {
      Post.create("Plus de flood", new java.util.Date(), 2, 1)
    }
  }

}