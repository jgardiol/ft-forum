package crawler

import org.jsoup._
import scala.collection.mutable
import play.api._
import java.text.SimpleDateFormat
import scala.collection.JavaConversions.asScalaBuffer
import java.net.MalformedURLException
import java.text.ParseException

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

  def getReports(doc: org.jsoup.nodes.Document, since: java.util.Date): List[Report] = {
    val rows = doc.select("#reportTable").first().select("tr")

    // 1st td contains date, 4th td contains report url

    import scala.collection.JavaConversions._
    import java.text.SimpleDateFormat
    val format = new SimpleDateFormat("dd-MM-yy hh:mm")

    var result: List[Report] = Nil

    for (row <- rows) {
      val cells = row.select("td")
      if (!cells.isEmpty && cells.size() > 1) {
        try {
          val date = format.parse(cells.first().text())
          val id = cells.get(3).select("a").attr("href").split("/").last
          val zone = cells.get(3).select("a").text()
          val isExpired = cells.get(8).select("td").attr("class") == "expexpired2"

          // Only store reports for relevant raids
          if (zone.contains(SoO) && !isExpired && date.after(since)) {
            result = Report(id, date) :: result
          }
        } catch {
          case e: ParseException => Logger.error("Date format exception: " + e.getMessage())
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

          // Check if spec is valid before adding the record
          if (controllers.Ranking.specs.contains(spec))
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

  def crawl = {

    try {
      // Crawl list of guilds
      val illidanDoc = Jsoup.connect(Illidan_URL).get
      val guilds = getGuilds(illidanDoc)

      // For each guild, crawl reports
      for (guild <- guilds) {
        // Add an entry to the db. Date is set to 1970 to ensure we get all reports.
        models.ranking.Guild.create(guild.name, guild.guildId, new java.util.Date(0L))
        val g = models.ranking.Guild.getByWolId(guild.guildId).get

        // Crawl reports
        val guildUrl = Guild_URL.replaceAll("GUILD_ID", guild.guildId)
        try {
          for (i <- 1 to 10) {
            val guildDoc = Jsoup.connect(guildUrl + i).get
            val reports = getReports(guildDoc, g.lastReport)
            processReports(reports)
          }
        } catch {
          case e: HttpStatusException => // Expected.
        }

        models.ranking.Guild.update(models.ranking.Guild(g.id, g.name, g.wolId, new java.util.Date))

        Thread.sleep(10000L)
      }
    } catch {
      case e: HttpStatusException => CrawlError.create(e.getMessage(), Illidan_URL)
      case e: java.net.SocketTimeoutException => CrawlError.create(e.getMessage(), Illidan_URL)
      case e: java.io.IOException => CrawlError.create(e.getMessage(), Illidan_URL)
      case e: Throwable => Logger.error("Fatal exception... " + e.getMessage())
    }
  }

  def processReports(reports: List[Report]) {
    for (report <- reports) {
      processReport(report)
      Thread.sleep(10000L)
    }
  }

  def processReport(report: Report) {
    val reportUrl = RankInfo_URL.replaceAll("REPORT_ID", report.reportId)

    try {
      val reportDoc = Jsoup.connect(reportUrl).get
      val bosses = getBosses(reportDoc)
      val dps = getDps(reportDoc, bosses)

      for (player <- dps) {
        for (boss <- player.dps) {
          models.ranking.Boss.getByName(boss._1.name, boss._1.difficulty) match {
            case Some(b) => models.ranking.Report.create(boss._2, player.spec, report.reportId, player.name, b.id)
            case None =>
          }
        }
      }
    } catch {
      case e: HttpStatusException => CrawlError.create(e.getMessage(), reportUrl)
      case e: java.net.SocketTimeoutException => CrawlError.create(e.getMessage(), reportUrl)
      case e: java.io.IOException => CrawlError.create(e.getMessage(), reportUrl)
    }
  }
}

case class CrawlError(id: Long, message: String, url: String)

object CrawlError {
  import play.api.db._
  import play.api.Play.current

  import anorm._
  import anorm.SqlParser._

  val simple =
    get[Long]("id") ~
      get[String]("message") ~
      get[String]("url") map {
        case id ~ message ~ url => CrawlError(id, message, url)
      }

  def all(): List[CrawlError] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM crawl_errors").as(simple *)
    }
  }

  def create(message: String, url: String) {
    DB.withConnection { implicit c =>
      SQL("INSERT INTO crawl_errors(message, url) VALUES ({message}, {url})").on(
        'message -> message,
        'url -> url).executeUpdate()
    }
  }

  def delete(id: Long) {
    DB.withConnection { implicit c =>
      SQL("DELETE FROM crawl_errors WHERE id={id}").on('id -> id).executeUpdate()
    }
  }
}