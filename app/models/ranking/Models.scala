package models.ranking

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class Boss(id: Long, name: String, difficulty: String)

object Boss {
  val simple =
    get[Long]("id") ~
      get[String]("name") ~
      get[String]("difficulty") map {
        case id ~ name ~ difficulty => Boss(id, name, difficulty)
      }

  def all(): List[Boss] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM bosses").as(simple*)
    }
  }

  // Adds a boss to the database, if he doesn't already exist.
  def create(name: String, difficulty: String) {
    DB.withConnection { implicit c =>
      SQL("""
    			INSERT INTO bosses(name, difficulty) 
    			SELECT {name}, {difficulty} 
    			WHERE NOT EXISTS (SELECT * FROM bosses WHERE name={name} AND difficulty={difficulty})
			""").on(
        'name -> name,
        'difficulty -> difficulty).executeUpdate()
    }
  }

  def getByName(name: String, difficulty: String): Option[Boss] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM bosses WHERE name={name} AND difficulty={difficulty}").on(
        'name -> name,
        'difficulty -> difficulty).as(simple.singleOpt)
    }
  }
}

case class Report(playerName: String, spec: String, dps: Double, boss: Boss, reportId: String)

object Report {
  case class Dps(id: Long, value: Double, spec: String, role: Role, reportId: String, playerName: String, bossId: Long)
  val simple =
    get[Long]("id") ~
      get[Double]("value") ~
      get[String]("spec") ~
      get[Long]("role") ~ 
      get[String]("report_id") ~
      get[String]("player_name") ~
      get[Long]("boss_id") map {
        case id ~ value ~ spec ~ role ~ report_id ~ player_id ~ boss_id => Dps(id, value, spec, role, report_id, player_id, boss_id)
      }
  
  type Role = Long
  
  val DPS = 0
  val Healer = 1
  val Tank = 2
  
  val roles = Map("dps" -> DPS, "healers" -> Healer, "tanks" -> Tank)
  val reverse = Map(DPS -> "dps", Healer -> "healers", Tank -> "tanks")

  def all() = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM dps").as(simple*)
    }
  }

  // Adds a report to the database if one with the same reportId doesn't already exist
  def create(value: Double, spec: String, reportId: String, playerName: String, bossId: Long) {
    DB.withConnection { implicit c =>
      SQL("""
				INSERT INTO dps(value, spec, role, report_id, player_name, boss_id) 
				SELECT {value}, {spec}, {role}, {report_id}, {player_name}, {boss_id} 
				WHERE NOT EXISTS 
					(SELECT * 
					 FROM dps 
					 WHERE report_id={report_id} AND player_name={player_name} AND boss_id={boss_id} AND spec={spec})
			""").on(
        'value -> value,
        'spec -> spec,
        'role -> specRoles.get(spec).get,
        'report_id -> reportId,
        'player_name -> playerName,
        'boss_id -> bossId).executeUpdate()
    }
  }

  // Returns all reports for a given player on a given boss
  def getReports(boss: Boss, playerName: String): List[Report] = {
    DB.withConnection { implicit c =>
      SQL("""
          SELECT * 
          FROM dps 
          WHERE boss_id={boss_id} AND player_name={player_name}
          ORDER BY value DESC
          """).on(
        'boss_id -> boss.id,
        'player_name -> playerName).as(simple *)
    } map { el =>
      Report(playerName, el.spec, el.value, boss, el.reportId)
    }
  }

  // Returns the best report for a given player on a given boss
  def getBestReport(boss: Boss, playerName: String, spec: String): Option[Report] = {
    DB.withConnection { implicit c =>
      SQL("""
		  SELECT DISTINCT ON(d.value) d.*
		  FROM dps d
		  INNER JOIN
		  (
		      SELECT DISTINCT MAX(value) AS value, player_name, boss_id, spec
		      FROM dps
		      WHERE player_name={player_name} AND spec={spec}
		      GROUP BY player_name, boss_id, spec
		  ) temp ON d.player_name=temp.player_name AND d.value=temp.value
		  WHERE d.boss_id={boss_id} AND d.player_name={player_name} AND d.spec={spec}
    	""").on(
        'boss_id -> boss.id,
        'player_name -> playerName,
        'spec -> spec).as(simple.singleOpt)
    } map {
      el =>
        Report(playerName, el.spec, el.value, boss, el.reportId)
    }
  }

  def getBestReports(boss: Boss, limit: Int, role: Int): List[Report] = {
    DB.withConnection { implicit c =>
      SQL("""
        		SELECT DISTINCT ON(d.value) d.*
        		FROM dps d
        		INNER JOIN
        		(
        		    SELECT DISTINCT MAX(value) AS value, player_name, spec
        		    FROM dps
        		    WHERE boss_id={boss_id}
        		    GROUP BY player_name, spec
        		) temp ON d.player_name=temp.player_name AND d.spec=temp.spec AND d.value=temp.value
    		  	INNER JOIN player_info
    		  	ON d.player_name=player_info.name
        		WHERE d.boss_id={boss_id} AND role={role}
        		ORDER BY value DESC
    		    LIMIT {limit}
        	""").on(
        'boss_id -> boss.id,
        'limit -> limit,
        'role -> role).as(simple *)
    } map {
      el => Report(el.playerName, el.spec, el.value, boss, el.reportId)
    }
  }

  // Returns the top _limit_ reports for a given boss and a given spec.
  def getBestReports(boss: Boss, limit: Int, spec: String): List[Report] = {
    DB.withConnection { implicit c =>
      SQL("""
        		SELECT DISTINCT ON(d.value) d.*
        		FROM dps d
        		INNER JOIN
        		(
        		    SELECT DISTINCT MAX(value) AS value, player_name, spec
        		    FROM dps
        		    WHERE boss_id={boss_id}
        		    GROUP BY player_name, spec
        		) temp ON d.player_name=temp.player_name AND d.spec=temp.spec AND d.value=temp.value
    		  	INNER JOIN player_info
    		  	ON d.player_name=player_info.name
        		WHERE d.boss_id={boss_id} AND d.spec={spec}
        		ORDER BY value DESC
    		    LIMIT {limit}
        	""").on(
        'boss_id -> boss.id,
        'spec -> spec,
        'limit -> limit).as(simple *)
    } map {
      el => Report(el.playerName, el.spec, el.value, boss, el.reportId)
    }
  }
  
  def getPlayerNames: List[String] = {
    DB.withConnection { implicit c =>
      SQL("SELECT player_name FROM dps GROUP BY player_name").as(get[String]("player_name")*)
    }
  }
  
  val specRoles = Map(
    "Blood 101" -> Tank,
    "Frost 102" -> DPS,
    "Unholy 103" -> DPS,
    "Balance 201" -> DPS,
    "Feral/Cat 202" -> DPS,
    "Feral/Bear 203" -> Tank,
    "Restoration 204" -> Healer,
    "Beast Mastery 301" -> DPS,
    "Marksmanship 302" -> DPS,
    "Survival 303" -> DPS,
    "Arcane 401" -> DPS,
    "Fire 402" -> DPS,
    "Frost 403" -> DPS,
    "Holy 501" -> Healer,
    "Protection 502" -> Tank,
    "Retribution 503" -> DPS,
    "Discipline 601" -> Healer,
    "Holy 602" -> Healer,
    "Shadow 603" -> DPS,
    "Assassination 701" -> DPS,
    "Combat 702" -> DPS,
    "Subtlety 703" -> DPS,
    "Elemental 801" -> DPS,
    "Enhancement 802" -> DPS,
    "Restoration 803" -> Healer,
    "Affliction 901" -> DPS,
    "Demonology 902" -> DPS,
    "Destruction 903" -> DPS,
    "Arms 1001" -> DPS,
    "Fury 1002" -> DPS,
    "Protection 1003" -> Tank,
    "Brewmaster 1101" -> Tank,
    "Mistweaver 1102" -> Healer,
    "Windwalker 1103" -> DPS)
}

case class Guild(id: Long, name: String, wolId: String, lastReport: java.util.Date)

object Guild {
  val simple = {
    get[Long]("id") ~
      get[String]("name") ~
      get[String]("wol_id") ~
      get[java.util.Date]("last_report") map {
        case id ~ name ~ wol_id ~ last_report => Guild(id, name, wol_id, last_report)
      }
  }

  // Adds a guild to the database, if one with the same wolId doesn't already exist
  def create(name: String, wolId: String, lastReport: java.util.Date) {
    DB.withConnection { implicit c =>
      SQL("""
    		  INSERT INTO guilds(name, wol_id, last_report) 
    		  SELECT {name}, {wol_id}, {last_report}
    		  WHERE NOT EXISTS (SELECT * FROM guilds WHERE wol_id={wol_id})
			""").on(
        'name -> name,
        'wol_id -> wolId,
        'last_report -> lastReport).executeUpdate()
    }
  }

  def update(guild: Guild) {
    DB.withConnection { implicit c =>
      SQL("UPDATE guilds SET last_report={last_report} WHERE id={id}").on(
        'last_report -> guild.lastReport,
        'id -> guild.id).executeUpdate()
    }
  }
  
  def getByWolId(id: String): Option[Guild] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM guilds WHERE wol_id={id}").on('id -> id).as(simple.singleOpt)
    }
  }

  def all(): List[Guild] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM guilds").as(simple *)
    }
  }
}