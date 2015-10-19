package ch.uzh.ifi.pdeboer.pdfpreprocessing.pdf

import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.{StatisticalTerm, Paper}

/**
 * Created by pdeboer on 16/10/15.
 */
class StatTermSearcher(paper: Paper, terms: List[StatisticalTerm]) {

	case class SearchTermMatch(term: StatisticalTerm, matchedExpression: String, startIndex: Int, endIndex: Int, page: Int) {
		def actualText = paper.contents(page).substring(startIndex, endIndex)
	}

	lazy val occurrences = {
		val withDuplicates = findOccurrences
		removeDuplicates(withDuplicates)
	}

	protected def findOccurrences: List[SearchTermMatch] = {
		terms.flatMap(originalTerm => {
			val regexes = buildRegexForString(originalTerm.searchTerm)
			regexes.flatMap(r => {
				val regex = r.r
				paper.lowerCaseContents.zipWithIndex.flatMap(paperPage => {
					regex.findAllMatchIn(paperPage._1).map(termMatch => {
						SearchTermMatch(originalTerm, r, termMatch.start, termMatch.end, paperPage._2)
					})
				})
			})
		})
	}


	def removeDuplicates(withDuplicates: List[SearchTermMatch]) = {
		private case class CorrectHashCode(term: StatisticalTerm, page: Int, startIndex: Int)(val searchTermMatch: SearchTermMatch)
		withDuplicates.map(d => CorrectHashCode(d.term, d.page, d.startIndex)(d)).toSet.map(_.searchTermMatch)
	}

	def buildRegexForString(searchString: String): List[String] = {
		val searchStringInclSuffixes = if (searchString.length < 7) addLikelySuffixesAndPostfixesToMethods(searchString) else List(searchString)

		val charsToEscape = "-()[].!{}:*"
		def quoteAndAllowSpaces(str: String) = str.map(c => (if (charsToEscape.contains(c)) s"\\$c" else c) + "[\\s\\-]*").mkString("")

		if (searchString.length < 7)
			searchStringInclSuffixes.map(search => {
				"(\\b" + quoteAndAllowSpaces(search) + "\\b)"
			})
		else
			List("(?i)(" + quoteAndAllowSpaces(searchString) + ")")
	}

	private def addLikelySuffixesAndPostfixesToMethods(t: String): List[String] = {
		List(t, t + "s")
	}
}
