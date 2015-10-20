package ch.uzh.ifi.pdeboer.pdfpreprocessing.png

import java.io.File

import scala.sys.process._
import ch.uzh.ifi.pdeboer.pdfpreprocessing.stats.PDFPermutation
import ch.uzh.ifi.pdeboer.pdfpreprocessing.util.FileUtils
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger

/**
 * Created by pdeboer on 20/10/15.
 */
class PDFToPNGConverter(pdfFile: File, perm: PDFPermutation, conversionCommand: String) extends LazyLogger {
	def convert(): File = {
		if (conversionCommandWithParameters.! != 0) {
			FileUtils.copyFileIntoDirectory(pdfFile, "errors_convertPDFtoPNG/")
			logger.error(s"couldn't convert file using $conversionCommandWithParameters")
			null
		} else {
			logger.debug(s"File: ${pdfFile.getName} successfully converted to PNG")
			new File(destinationPath)
		}
	}

	def destinationPath = pdfFile.getParentFile.getAbsolutePath + "/" + FileUtils.filenameWithoutExtension(pdfFile) + ".png"

	def conversionCommandWithParameters = {
		Seq("bash", "-c", s"nice -n 5 $conversionCommand -density 200 -append ${pdfFile.getPath + pageRange} $destinationPath")
	}

	private def pageRange: String = {
		val pageIndices = perm.highlights.map(_.occurrence.page - 1)
		val (minPage, maxPage) = (pageIndices.min, pageIndices.max)
		if (minPage == maxPage) s"[$minPage]" else s"[$minPage-$maxPage]"
	}
}
