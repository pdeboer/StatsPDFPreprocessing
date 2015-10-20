package ch.uzh.ifi.pdeboer.pdfpreprocessing.pdf

import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.StatTermOccurrence
import ch.uzh.ifi.pdeboer.pdfpreprocessing.util.FileUtils

/**
 * Created by pdeboer on 16/10/15.
 */
class PDFHighlighter(occurrences: Iterable[StatTermOccurrence], copyTargetFolder: String = "output/") {
	private def targetFolderWithTrailingSlash = if (copyTargetFolder.endsWith("/")) copyTargetFolder else copyTargetFolder + "/"

	def copyAndHighlight(): Unit = {
		val pdfToHighlight = occurrences.headOption.map(h => FileUtils.copyFileIntoDirectory(h.paper.file, targetFolderWithTrailingSlash))


	}
}
