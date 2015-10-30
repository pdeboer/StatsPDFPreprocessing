package ch.uzh.ifi.pdeboer.pdfpreprocessing

import java.io.File

import ch.uzh.ifi.pdeboer.pdfpreprocessing.pdf.PDFLoader
import ch.uzh.ifi.pdeboer.pdfpreprocessing.sampling.{MethodDistribution, PaperMethodMap, PaperSelection}
import ch.uzh.ifi.pdeboer.pdfpreprocessing.stats.StatTermSearcher
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

/**
 * Created by pdeboer on 30/10/15.
 */
object PaperSampler extends App with LazyLogging {
	logger.debug("starting highlighting")

	val conf = ConfigFactory.load()
	val INPUT_DIR = conf.getString("highlighter.pdfSourceDir")
	val PERCENTAGE = conf.getDouble("sampler.targetPercentage")

	val allPapers = new PDFLoader(new File(INPUT_DIR)).getPapers()
	val allPaperMethodMaps = allPapers.map(p => PaperMethodMap.fromOccurrenceList(
		new StatTermSearcher(p, includeAssumptions = false).occurrences.toList)).toSet

	val targetDistribution = new MethodDistribution(
		new PaperSelection(allPaperMethodMaps).methodOccurrenceMap.map(e => e._1 -> (e._2 * PERCENTAGE).toInt))

	class PaperSelectionFound(val paperSelection: PaperSelection) extends Exception

	def tryAddingPaperToSelection(currentSelection: PaperSelection, holdOut: Set[PaperMethodMap]): Option[PaperSelection] = {
		if (currentSelection.hasMoreOccurrencesForAtLeastOneMethod(targetDistribution)) None
		else if (currentSelection == targetDistribution) throw new PaperSelectionFound(currentSelection)
		else {
			holdOut.par.map(m => {
				val newSelection = currentSelection.addPaper(m)
				val holdOutWithoutNewPaper = holdOut - m
				tryAddingPaperToSelection(newSelection, holdOutWithoutNewPaper)
			}).find(_.isDefined).getOrElse(None)
		}
	}

	try {
		tryAddingPaperToSelection(new PaperSelection(Set.empty[PaperMethodMap]), allPaperMethodMaps)
	}
	catch {
		case p: PaperSelectionFound => {
			logger.info(s"Found possible configuration: ${p.paperSelection.papers.map(_.paper).mkString(",")}")
			p.paperSelection.persist("sample.csv")
		}
	}
}