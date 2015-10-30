package ch.uzh.ifi.pdeboer.pdfpreprocessing.stats

import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.{StatisticalMethod, Paper, StatTermOccurrence, StatisticalTerm}

import scala.collection.immutable.Iterable

/**
 * Created by pdeboer on 16/10/15.
 */
class StatTermSearcher(paper: Paper, includeAssumptions: Boolean = true, terms: List[StatisticalMethod] = StatTermloader.terms) {

	import StatTermSearcher._

	lazy val occurrences: Iterable[StatTermOccurrence] = {
		val withDuplicates = findOccurrences
		removeDuplicates(withDuplicates)
	}

	protected def findOccurrences = {
		terms.flatMap(method => {
			searchForTerm(method) ::: (if (includeAssumptions) method.assumptions.flatMap(searchForTerm) else Nil)
		})
	}

	def searchForTerm(originalTerm: StatisticalTerm): List[StatTermOccurrence] = {
		(originalTerm.searchTerm :: originalTerm.synonyms).flatMap(searchTerm => {
			val regexes = buildRegexForString(searchTerm.toLowerCase)
			regexes.flatMap(r => {
				val regex = r.r
				paper.lowerCaseContents.zipWithIndex.flatMap(paperPage => {
					regex.findAllMatchIn(paperPage._1).map(termMatch => {
						StatTermOccurrence(originalTerm, r, paper, termMatch.start, termMatch.end, paperPage._2)
					})
				})
			})
		})
	}

	def removeDuplicates(withDuplicates: List[StatTermOccurrence]) = {
		withDuplicates.groupBy(d => (d.term, d.page, d.startIndex)).map(_._2.head)
	}

	def buildRegexForString(searchString: String): List[String] = {
		val searchStringInclSuffixes = if (searchString.length < 7) addLikelySuffixesAndPostfixesToMethods(searchString) else List(searchString)

		if (searchString.length < 7)
			searchStringInclSuffixes.map(search => {
				"(\\b" + addRegexToAllowSpaces(search) + "\\b)"
			})
		else
			List("(?i)(" + addRegexToAllowSpaces(searchString) + ")")
	}

	private def addLikelySuffixesAndPostfixesToMethods(t: String): List[String] = {
		List(t, t + "s")
	}
}

object StatTermSearcher {
	val charsToEscape = "-()[].!{}:*"

	def addRegexToAllowSpaces(str: String) =
		str.replaceAll("\\s", "").map(c => (if (charsToEscape.contains(c)) s"\\$c" else c) + "[\\s\\-]*").mkString("")
}