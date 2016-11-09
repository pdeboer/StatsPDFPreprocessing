package ch.uzh.ifi.pdeboer.pdfpreprocessing.entities

import java.io.File

import ch.uzh.ifi.pdeboer.pdfpreprocessing.pdf.PDFTextExtractor
import ch.uzh.ifi.pdeboer.pdfpreprocessing.stats.{StatTermSearcher, UniqueSearchStringIdentifier}

import scala.io.Source

/**
  * Created by pdeboer on 16/10/15.
  */
case class Journal(name: String = "journal", basePath: File, year: Int = 2014) extends Serializable {
  def numColumns: Int = numColumnsOption.getOrElse(2)

  def numColumnsOption: Option[Int] = {
    ".*_([0-9])+col".r.findFirstMatchIn(name).map(_.group(1).toInt)
  }

  def nameWithoutCol = if (numColumnsOption.isDefined) name.substring(0, name.length - "_1col".length) else name
}


class Paper(val name: String, val file: File, val journal: Journal) extends Serializable {
  val contents = new PDFTextExtractor(file.getAbsolutePath).pages
  lazy val lowerCaseContents = contents.map(_.toLowerCase)

  override def toString = s"Paper: $name"

  def canEqual(other: Any): Boolean = other.isInstanceOf[Paper]

  override def equals(other: Any): Boolean = other match {
    case that: Paper =>
      (that canEqual this) &&
        contents == that.contents &&
        lowerCaseContents == that.lowerCaseContents &&
        name == that.name &&
        file == that.file &&
        journal == that.journal
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(contents, lowerCaseContents, name, file, journal)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

class MaterializedPaper(_name: String, _file: File, _journal: Journal, txtContent: File)
  extends Paper(_name, _file, _journal) {
  override val contents = List(Source.fromFile(txtContent).getLines().mkString("\n"))
}

case class StatTermOccurrence(term: StatisticalTerm, matchedExpression: String, paper: Paper, startIndex: Int, endIndex: Int, page: Int) {
  def actualText = paper.contents(page).substring(startIndex, endIndex)

  def escapedMatchText = StatTermSearcher.addRegexToAllowSpaces(actualText)

  def inclPageOffset(index: Int) = paper.contents.take(Math.max(0, page - 1)).map(_.length).sum + index

  lazy val uniqueSearchStringInPaper = new UniqueSearchStringIdentifier(this).findUniqueTerm()
}

case class StatTermOccurrenceGroup(term: StatisticalTerm, occurrences: List[StatTermOccurrence]) {
  lazy val minIndex = occurrences.map(o => o.inclPageOffset(o.startIndex)).min

  lazy val maxIndex = occurrences.map(o => o.inclPageOffset(o.endIndex)).max
}

sealed trait StatisticalTerm extends Serializable {
  def name: String

  def synonyms: List[String]

  def searchTerm = name.toLowerCase

  def isStatisticalMethod: Boolean
}

case class StatisticalMethod(name: String, synonyms: List[String], assumptions: List[StatisticalAssumption], delta: Int = 0) extends StatisticalTerm {
  override def isStatisticalMethod = true

  override def toString = "Method: " + name
}

case class StatisticalAssumption(name: String, synonyms: List[String]) extends StatisticalTerm {
  override def isStatisticalMethod = false

  override def toString = s"Assumption: " + name
}