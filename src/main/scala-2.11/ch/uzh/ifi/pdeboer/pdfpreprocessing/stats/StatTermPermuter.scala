package ch.uzh.ifi.pdeboer.pdfpreprocessing.stats

import java.awt.Color

import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.{Paper, StatisticalAssumption, StatisticalMethod, StatTermOccurrence}
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger

/**
 * Created by pdeboer on 20/10/15.
 */
class StatTermPermuter(occurrences: Iterable[StatTermOccurrence]) extends LazyLogger {
	private var _missingAssumptions = List.empty[(StatTermOccurrence, StatisticalAssumption)]

	def missingAssumptions = _missingAssumptions

	lazy val permutations: Iterable[PDFPermutation] = {
		val methods = occurrences.filter(_.term.isInstanceOf[StatisticalMethod])
		val assumptionsMap: Map[StatisticalAssumption, Iterable[StatTermOccurrence]] = occurrences.filter(_.term.isInstanceOf[StatisticalAssumption])
			.groupBy(_.term).map(a => a._1.asInstanceOf[StatisticalAssumption] -> a._2)

		methods.flatMap(methodOccurrence => {
			val termOfMethod = methodOccurrence.term.asInstanceOf[StatisticalMethod]
			termOfMethod.assumptions.flatMap(a => {
				assumptionsMap.getOrElse(a, {
					_missingAssumptions = (methodOccurrence, a) :: _missingAssumptions
					Nil
				}).map(assumptionOccurrence => {
					PDFPermutation(assumptionOccurrence.paper, List(
						PDFHighlightTerm.fromTermOccurrence(assumptionOccurrence),
						PDFHighlightTerm.fromTermOccurrence(methodOccurrence)
					))
				})
			})
		})
	}
}

case class PDFPermutation(paper: Paper, highlights: List[PDFHighlightTerm])

case class PDFHighlightTerm(color: Color, occurrence: StatTermOccurrence)

object PDFHighlightTerm {
	def fromTermOccurrence(o: StatTermOccurrence) = {
		val color = o.term match {
			case m: StatisticalMethod => Color.yellow
			case a: StatisticalAssumption => Color.green
		}

		PDFHighlightTerm(color, o)
	}
}


