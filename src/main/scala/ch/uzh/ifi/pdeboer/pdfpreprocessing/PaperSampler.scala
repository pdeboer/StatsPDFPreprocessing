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
	logger.debug("starting sampling")

	val conf = ConfigFactory.load()
	val INPUT_DIR = conf.getString("highlighter.pdfSourceDir")
	val PERCENTAGE = conf.getDouble("sampler.targetPercentage")

	val allPapers = new PDFLoader(new File(INPUT_DIR)).getPapers()
	val allPaperMethodMaps = allPapers.map(p => PaperMethodMap.fromOccurrenceList(
		new StatTermSearcher(p, includeAssumptions = false).occurrences.toList)).toList

	val targetDistribution = new MethodDistribution(
		new PaperSelection(allPaperMethodMaps).methodOccurrenceMap.map(e => e._1 -> (e._2 * PERCENTAGE).toInt))
	logger.debug("complete distribution is " + new PaperSelection(allPaperMethodMaps))
	logger.debug(s"target distribution is $targetDistribution")

	class PaperSelectionFound(val paperSelection: PaperSelection) extends Exception

	def tryAddingPaperToSelection(currentSelection: PaperSelection, holdOut: List[PaperMethodMap]): Option[PaperSelection] = {
		if (currentSelection.hasMoreOccurrencesForAtLeastOneMethod(targetDistribution)) None
		else if (currentSelection == targetDistribution) throw new PaperSelectionFound(currentSelection)
		else {
			holdOut.par.map(m => {
				val newSelection = currentSelection.addPaper(m)
				val holdOutWithoutNewPaper = holdOut.filterNot(p => p.paper == m.paper)
				tryAddingPaperToSelection(newSelection, holdOutWithoutNewPaper)
			}).find(_.isDefined).getOrElse(None)
		}
	}

	try {
		tryAddingPaperToSelection(new PaperSelection(Nil), allPaperMethodMaps)
		logger.info("haven't found anything :(")
	}
	catch {
		case p: PaperSelectionFound => {
			logger.info(s"Found possible configuration: ${p.paperSelection.papers.map(_.paper).mkString(",")}")
			p.paperSelection.persist("sample.csv")
		}
	}
}