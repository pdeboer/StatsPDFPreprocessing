package ch.uzh.ifi.pdeboer.pdfpreprocessing.sampling

import java.io.File

import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.{StatTermOccurrence, Paper, StatisticalMethod}
import com.github.tototoshi.csv.CSVWriter

/**
 * Created by pdeboer on 30/10/15.
 */
class PaperSelection(val papers: Set[PaperMethodMap]) extends MethodDistribution(Map.empty) {
	override lazy val methodOccurrenceMap: Map[StatisticalMethod, Int] = papers.foldLeft(Map.empty[StatisticalMethod, Int])((l, r) => {
		val allMethodKeys = (l.keys.toList ::: r.methodOccurrenceMap.keys.toList).toSet
		allMethodKeys.map(key => key -> (l.getOrElse(key, 0) + r.methodOccurrenceMap.getOrElse(key, 0))).toMap
	})

	def addPaper(p: PaperMethodMap) = new PaperSelection(papers + p)

	def persist(filename: String): Unit = {
		val w = CSVWriter.open(new File(filename))
		val keysList = methodOccurrenceMap.keys.toList
		w.writeRow("paper" :: keysList.map(_.name))
		papers.foreach(p => w.writeRow(p :: keysList.map(k => p.methodOccurrenceMap.getOrElse(k, 0))))
		w.close()
	}
}

class PaperMethodMap(val paper: Paper, methodOccurrenceMap: Map[StatisticalMethod, Int]) extends MethodDistribution(methodOccurrenceMap) {}

object PaperMethodMap {
	def fromOccurrenceList(occurrences: List[StatTermOccurrence]) = {
		val methodOccurrences = occurrences.filter(_.term.isStatisticalMethod)
		val map = methodOccurrences.groupBy(_.term).map(g => g._1.asInstanceOf[StatisticalMethod] -> g._2.size)
		new PaperMethodMap(methodOccurrences.head.paper, map)
	}
}

class MethodDistribution(_methodOccurrenceMap: Map[StatisticalMethod, Int]) {
	def methodOccurrenceMap: Map[StatisticalMethod, Int] = _methodOccurrenceMap

	def canEqual(other: Any): Boolean = other.isInstanceOf[MethodDistribution]

	override def equals(other: Any): Boolean = other match {
		case that: MethodDistribution =>
			(that canEqual this) &&
				methodOccurrenceMap == that.methodOccurrenceMap
		case _ => false
	}

	override def hashCode(): Int = {
		val state = Seq(methodOccurrenceMap)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}

	def hasMoreOccurrencesForAtLeastOneMethod(other: MethodDistribution) =
		methodOccurrenceMap.exists(e => e._2 > other.methodOccurrenceMap.getOrElse(e._1, 0))
}
