package crawler

import org.jsoup._
import scala.collection.mutable
import play.api._
import java.text.SimpleDateFormat
import scala.collection.JavaConversions.asScalaBuffer


object Crawler {
  case class Boss(name: String, difficulty: String)
  case class Player(name: String, spec: String, dps: Map[Boss, Double])
  case class Guild(name: String, guildId: String)
  case class Report(reportId: String, date: java.util.Date)

  val Illidan_URL = "http://worldoflogs.com/realms/453/"
  val Guild_URL = "http://worldoflogs.com/guilds/GUILD_ID/reports/"
  val RankInfo_URL = "http://worldoflogs.com/reports/REPORT_ID/rankinfo/"

  val SoO = "Siege of Orgrimmar"

  def getGuilds(doc: org.jsoup.nodes.Document): List[Guild] = {
    val rows = doc.select("#guilds-active").first().select("tr")

    import scala.collection.JavaConversions._

    var result: List[Guild] = Nil

    for (row <- rows) {
      val link = row.select("td").first().select("a")
      val id = link.attr("href").split("/").last
      val name = link.text()
      result = Guild(name, id) :: result
    }

    result
  }

  def getReports(doc: org.jsoup.nodes.Document): List[Report] = {
    val rows = doc.select("#reportTable").first().select("tr")

    // 1st td contains date, 4th td contains report url

    import scala.collection.JavaConversions._
    import java.text.SimpleDateFormat
    val format = new SimpleDateFormat("dd-MM-yy hh:mm")

    var result: List[Report] = Nil

    for (row <- rows) {
      val cells = row.select("td")
      if (!cells.isEmpty && cells.size() > 1) {
        val date = cells.first().text()
        val id = cells.get(3).select("a").attr("href").split("/").last
        val zone = cells.get(3).select("a").text()
        val isExpired = cells.get(8).select("td").attr("class") == "expexpired2"

        // Only store reports for relevant raids
        if (zone.contains(SoO) && !isExpired) {
          result = Report(id, format.parse(date)) :: result
        }
      }
    }

    result
  }

  // Returns a list of Players
  def getDps(doc: org.jsoup.nodes.Document, bosses: List[Boss]): List[Player] = {
    val tables = doc.select(".playerdata")
    import scala.collection.JavaConversions._

    var result: List[Player] = Nil

    // boss, player, spec, dps
    var temp: List[(Boss, String, String, String)] = Nil

    for (table <- tables) {
      val bossname = table.previousElementSibling().text()
      val boss = bosses.find(b => b.name == bossname).getOrElse(Boss(bossname, "10N"))
      val rows = table.select("tr")
      for (row <- rows) {
        val cells = row.select("td")
        if (!cells.isEmpty) {
          val playername = cells.first().select("span").text()
          val spec = cells.get(1).text()
          val dps = if (cells.get(6).text() != "-")
            cells.get(6).text()
          else
            cells.get(7).text()

          temp = (boss, playername, spec, dps) :: temp
        }
      }
    }

    val byPlayer = temp.groupBy(g => (g._2, g._3))

    for (player <- byPlayer) {
      val name = player._1._1
      val spec = player._1._2
      val map: mutable.Map[Boss, Double] = mutable.Map.empty
      for (data <- player._2) {
        map.put(data._1, data._4.toDouble)
      }
      val p = Player(name, spec, map.toMap)
      result = p :: result
    }

    result
  }

  // Returns a list of bossnames with their difficulty from the document.
  def getBosses(doc: org.jsoup.nodes.Document): List[Boss] = {
    val rows = doc.select(".rankinfo").first().select("tr")

    var result: List[Boss] = Nil

    import scala.collection.JavaConversions._

    for (row <- rows) {
      val cells = row.select("td")
      if (!cells.isEmpty) {
        // Retrieve contents
        val text = cells.first().text()
        // Find index of last space
        val lastSpace = text.lastIndexOf(" ")
        // Split bossname and difficulty
        val split = text.splitAt(lastSpace)
        result = Boss(split._1.trim, split._2.trim) :: result
      }
    }

    result
  }

}