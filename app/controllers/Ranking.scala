package controllers

import play.api._
import play.api.mvc._

import scala.collection.mutable

import org.jsoup._
import models.ranking._
import crawler._

object Ranking extends Utils {

  def rankings(boss: String, difficulty: String, spec: String) = Action { implicit request =>
    val b = Boss.getByName(boss, difficulty)
    val reports = Boss.getByName(boss, difficulty) match {
      case Some(boss) => {
        spec match {
          case "all" => Report.getBestReports(boss, 100)
          case spec => Report.getBestReports(boss, 100, spec)
        }

      }
      case None => Nil
    }
    
    WithUri(views.html.ranking.rankings(reports, boss, difficulty, spec, user))
  }
  
  def player(name: String, boss: String, difficulty: String, spec: String) = Action { implicit request =>
    val b = Boss.getByName(boss, difficulty)
    val reports = Boss.getByName(boss, difficulty) match {
      case Some(boss) => Report.getReports(boss, name)
      case None => Nil
    }
    
    WithUri(views.html.ranking.rankings(reports, boss, difficulty, spec, user))
  }

  val reportUrl = "http://worldoflogs.com/reports/REPORT_ID/"

  val bosses = List("Immerseus", "The Fallen Protectors", "Norushen", "Sha of Pride", "Galakras", "Iron Juggernaut",
    "Kor'kron Dark Shaman", "General Nazgrim", "Malkorok", "Spoils of Pandaria", "Thok the Bloodthirsty",
    "Siegecrafter Blackfuse", "Paragons of the Klaxxi", "Garrosh Hellscream")

  val classes = List("Death Knight",
    "Druid",
    "Hunter",
    "Mage",
    "Monk",
    "Paladin",
    "Priest",
    "Rogue",
    "Shaman",
    "Warlock",
    "Warrior")

  val specs = Map(
    "Blood 101" -> "Death Knight",
    "Frost 102" -> "Death Knight",
    "Unholy 103" -> "Death Knight",
    "Balance 201" -> "Druid",
    "Feral/Cat 202" -> "Druid",
    "Feral/Bear 203" -> "Druid",
    "Restoration 204" -> "Druid",
    "Beast Mastery 301" -> "Hunter",
    "Marksmanship 302" -> "Hunter",
    "Survival 303" -> "Hunter",
    "Arcane 401" -> "Mage",
    "Fire 402" -> "Mage",
    "Frost 403" -> "Mage",
    "Holy 501" -> "Paladin",
    "Protection 502" -> "Paladin",
    "Retribution 503" -> "Paladin",
    "Discipline 601" -> "Priest",
    "Holy 602" -> "Priest",
    "Shadow 603" -> "Priest",
    "Assassination 701" -> "Rogue",
    "Combat 702" -> "Rogue",
    "Subtlety 703" -> "Rogue",
    "Elemental 801" -> "Shaman",
    "Enhancement 802" -> "Shaman",
    "Restoration 803" -> "Shaman",
    "Affliction 901" -> "Warlock",
    "Demonology 902" -> "Warlock",
    "Destruction 903" -> "Warlock",
    "Arms 1001" -> "Warrior",
    "Fury 1002" -> "Warrior",
    "Protection 1003" -> "Warrior",
    "Brewmaster 1101" -> "Monk",
    "Mistweaver 1102" -> "Monk",
    "Windwalker 1103" -> "Monk")

  val difficulties = List("10N", "25N", "10H", "25H", "25L")
}