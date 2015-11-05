package ch.uzh.ifi.pdeboer.pdfpreprocessing.sampling

import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.StatisticalMethod
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger

import scala.util.Random

/**
  * Created by pdeboer on 05/11/15.
  */
class RandomSampler(val targetDistribution: MethodDistribution, val allPaperMethodMaps: List[PaperMethodMap]) extends LazyLogger {
	val paperMethodMapsWithoutDiscreditedMethods = allPaperMethodMaps.filter(p => p.methodOccurrenceMap.keys.forall(k => targetDistribution.methodOccurrenceMap.contains(k)))


	val allKeys = targetDistribution.methodOccurrenceMap.keys.toList
	var currentSelection = new PaperSelection(Nil)
	var bestSelectionSoFar = currentSelection

	def run(): Unit = {
		while (currentSelection.distanceTo(targetDistribution.methodOccurrenceMap) > 0) {
			val targetKey = allKeys((allKeys.length * Random.nextDouble()).toInt)
			val currentSelectionValueForKey: Int = currentSelection.methodOccurrenceMap.getOrElse(targetKey, 0)
			if (currentSelectionValueForKey < targetDistribution.methodOccurrenceMap(targetKey)) {
				val candidatesToAdd = paperMethodMapsWithoutDiscreditedMethods.filter(_.methodOccurrenceMap.getOrElse(targetKey, 0) > 0)
				if (candidatesToAdd.nonEmpty) {
					val paperToAdd = candidatesToAdd((candidatesToAdd.length * Random.nextDouble()).toInt)
					currentSelection = currentSelection.newSelectionWithPaper(paperToAdd)
				} else removePaperForKey(targetKey)
			} else if (currentSelectionValueForKey > targetDistribution.methodOccurrenceMap(targetKey)) {
				removePaperForKey(targetKey)
			}

			if (currentSelection.distanceTo(targetDistribution.methodOccurrenceMap) < bestSelectionSoFar.distanceTo(targetDistribution.methodOccurrenceMap)) {
				bestSelectionSoFar = currentSelection
				logger.info(s"found better selection with distance Distance: ${bestSelectionSoFar.distanceTo(targetDistribution.methodOccurrenceMap)}: $bestSelectionSoFar")
			}
		}
	}

	def removePaperForKey(targetKey: StatisticalMethod): Unit = {
		val candidatesToRemove = currentSelection.papers.filter(_.methodOccurrenceMap.getOrElse(targetKey, 0) > 0)
		val paperToRemove = candidatesToRemove((candidatesToRemove.length * Random.nextDouble()).toInt)
		currentSelection = new PaperSelection(currentSelection.papers.filterNot(p => p == paperToRemove))
	}

	def addRandomPaper(candidatesToAdd: List[PaperMethodMap]): Unit = {

	}
}
