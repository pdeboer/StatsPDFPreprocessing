package ch.uzh.ifi.pdeboer.pdfpreprocessing.png

import java.io.File

import ch.uzh.ifi.pdeboer.pdfpreprocessing.stats.PDFPermutation

/**
 * Created by pdeboer on 20/10/15.
 */
class PDFToPNGConverter(pdfFile: File, perm: PDFPermutation, destinationPath: String, conversionCommand: String) {
	def getBashCommandToConvert = {
		Seq("bash", "-c", s"nice -n 5 $conversionCommand -density 200 -append ${pdfFile.getPath + pageRange} $destinationPath.png")
	}

	private def pageRange: String = {
		val pageIndices = perm.highlights.map(_.occurrence.page)
		val (minPage, maxPage) = (pageIndices.min, pageIndices.max)
		if (minPage == maxPage) s"[$minPage]" else s"[$minPage-$maxPage]"
	}
}
