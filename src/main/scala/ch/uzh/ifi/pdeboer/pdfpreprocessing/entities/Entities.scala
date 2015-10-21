package ch.uzh.ifi.pdeboer.pdfpreprocessing.entities

import java.io.File

import ch.uzh.ifi.pdeboer.pdfpreprocessing.pdf.PDFTextExtractor
import ch.uzh.ifi.pdeboer.pdfpreprocessing.stats.{StatTermSearcher, UniqueSearchStringIdentifier}

/**
 * Created by pdeboer on 16/10/15.
 */
case class Journal(name: String = "journal", basePath: File, year: Int = 2014) {
	def singleColumnPapers: Boolean = {
		name.endsWith("_1col")
	}
}


case class Paper(name: String, file: File, journal: Journal) {
	lazy val contents = new PDFTextExtractor(file.getAbsolutePath).pages
	lazy val lowerCaseContents = contents.map(_.toLowerCase)

	override def toString = s"Paper: $name"
}

case class StatTermOccurrence(term: StatisticalTerm, matchedExpression: String, paper: Paper, startIndex: Int, endIndex: Int, page: Int) {
	def actualText = paper.contents(page).substring(startIndex, endIndex)

	def escapedMatchText = StatTermSearcher.addRegexToAllowSpaces(actualText)

	lazy val uniqueSearchStringInPaper = new UniqueSearchStringIdentifier(this).findUniqueTerm()
}

sealed trait StatisticalTerm {
	def name: String

	def synonyms: List[String]

	def searchTerm = name.toLowerCase

	def isStatisticalMethod: Boolean
}

case class StatisticalMethod(name: String, synonyms: List[String], assumptions: List[StatisticalAssumption]) extends StatisticalTerm {
	override def isStatisticalMethod = true

	override def toString = "Method: " + name

}

case class StatisticalAssumption(name: String, synonyms: List[String]) extends StatisticalTerm {
	override def isStatisticalMethod = false

	override def toString = s"Assumption: " + name
}