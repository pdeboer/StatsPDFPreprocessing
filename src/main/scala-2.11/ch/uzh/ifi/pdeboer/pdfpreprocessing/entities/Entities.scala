package ch.uzh.ifi.pdeboer.pdfpreprocessing.entities

import java.io.File

import ch.uzh.ifi.pdeboer.pdfpreprocessing.pdf.PDFTextExtractor
import ch.uzh.ifi.pdeboer.pdfpreprocessing.stats.UniqueSearchStringIdentifier

/**
 * Created by pdeboer on 16/10/15.
 */
case class Journal(name: String = "journal", basePath: File, year: Int = 2014)

case class Paper(name: String, file: File, journal: Journal) {
	lazy val contents = new PDFTextExtractor(file.getAbsolutePath).pages
	lazy val lowerCaseContents = contents.map(_.toLowerCase)
}

case class StatTermOccurrence(term: StatisticalTerm, matchedExpression: String, paper: Paper, startIndex: Int, endIndex: Int, page: Int) {
	def actualText = paper.contents(page).substring(startIndex, endIndex)

	lazy val uniqueSearchStringInPaper = new UniqueSearchStringIdentifier(this).findUniqueTerm()
}

case class UniqueSearchTerm(term: String, statTerm: String)

sealed class StatisticalTerm(val name: String, val synonyms: List[String]) {
	def searchTerm = name.toLowerCase
}

case class StatisticalMethod(methodName: String, _synonyms: List[String], assumptions: List[StatisticalAssumption]) extends StatisticalTerm(methodName, _synonyms)

case class StatisticalAssumption(assumptionName: String, _synonyms: List[String]) extends StatisticalTerm(assumptionName, _synonyms)