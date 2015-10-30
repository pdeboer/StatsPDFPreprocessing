package ch.uzh.ifi.pdeboer.pdfpreprocessing.stats

import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.{StatTermOccurrenceGroup, StatTermOccurrence, StatisticalMethod}

/**
 * Created by pdeboer on 30/10/15.
 */
class TermMerger(terms: Iterable[StatTermOccurrence]) {
	lazy val methodOccurrences = terms.filter(_.term.isStatisticalMethod).groupBy(_.term).asInstanceOf[Map[StatisticalMethod, Iterable[StatTermOccurrence]]]

	def mergeMethod(method: StatisticalMethod, terms: Iterable[StatTermOccurrence]) = {
		var groups = terms.map(t => StatTermOccurrenceGroup(method, List(t)))
		var changes: Boolean = true
		while (changes) {
			changes = false
			val newGroups = groups.map(g1 => {
				val possibleMergeCandidate = groups.find(g2 => g1 != g2
					&& g1.minIndex < g2.minIndex && g1.minIndex + method.delta > g2.minIndex)

				if (possibleMergeCandidate.isDefined) {
					changes = true
					StatTermOccurrenceGroup(method, g1.occurrences ::: possibleMergeCandidate.get.occurrences)
				} else g1
			})
			groups = newGroups
		}
		groups
	}

	def mergeAllMethods = {
		methodOccurrences.map(m => mergeMethod(m._1, m._2))
	}
}
