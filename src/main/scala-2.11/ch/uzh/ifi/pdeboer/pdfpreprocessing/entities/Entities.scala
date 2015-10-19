package ch.uzh.ifi.pdeboer.pdfpreprocessing.entities

import java.io.File

import ch.uzh.ifi.pdeboer.pdfpreprocessing.pdf.PDFTextExtractor

/**
 * Created by pdeboer on 16/10/15.
 */
case class Journal(name: String, basePath: File, year: Int = 2014)

case class Paper(name: String, file: File, journal: Journal) {
	lazy val contents = new PDFTextExtractor(file.getAbsolutePath).pages
	lazy val lowerCaseContents = contents.map(_.toLowerCase)
}

case class StattermOccurrence(term: StatisticalTerm, paper: Paper, startIndex: Int, endIndex: Int, pageNr: Int)

class StatisticalTerm(name: String, synonyms: List[String]) {
	def searchTerm = name.toLowerCase
}

case class StatisticalMethod(methodName: String, synonyms: List[String], var assumptions: List[StatisticalAssumption]) extends StatisticalTerm(methodName, synonyms)

case class StatisticalAssumption(assumptionName: String, synonyms: List[String]) extends StatisticalTerm(assumptionName, synonyms)