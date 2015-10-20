package ch.uzh.ifi.pdeboer.pdfpreprocessing.pdf

import java.io._

import ch.uzh.ifi.pdeboer.pdfpreprocessing.stats.PDFPermutation
import ch.uzh.ifi.pdeboer.pdfpreprocessing.util.FileUtils
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import org.apache.pdfbox.pdfparser.PDFParser
import org.apache.pdfbox.pdmodel.PDDocument

/**
 * Created by pdeboer on 16/10/15.
 */
class PDFHighlighter(permutation: PDFPermutation, outputBaseFolder: String = "output/", filenamePrefix: String = "") extends LazyLogger {
	private def targetFolder = {
		val basefolderWithTrailingSlash = if (outputBaseFolder.endsWith("/")) outputBaseFolder else outputBaseFolder + "/"
		val fullname = basefolderWithTrailingSlash + permutation.paper.journal.name + "/" + permutation.paper.name + "/"
		new File(fullname).mkdirs()
		fullname
	}

	def highlight(pdfToHighlight: File): Boolean = {
		try {
			val parser: PDFParser = new PDFParser(new FileInputStream(pdfToHighlight))
			parser.parse()
			val pdDoc: PDDocument = new PDDocument(parser.getDocument)

			val pdfHighlight: TextHighlight = new TextHighlight("UTF-8")
			pdfHighlight.setLineSeparator(" ")
			pdfHighlight.initialize(pdDoc)

			permutation.highlights.foreach(i =>
				pdfHighlight.highlight(
					i.occurrence.uniqueSearchStringInPaper.r.pattern,
					i.occurrence.escapedMatchText.r.pattern, i.color, i.occurrence.page))

			val byteArrayOutputStream = new ByteArrayOutputStream()

			if (pdDoc != null) {
				pdDoc.save(byteArrayOutputStream)
				pdDoc.close()
			}
			if (parser.getDocument != null) {
				parser.getDocument.close()
			}

			Some(new BufferedOutputStream(new FileOutputStream(pdfToHighlight))).foreach(o => {
				o.write(byteArrayOutputStream.toByteArray)
				o.close()
			})
			logger.info(s"highlighted $permutation")
			true
		}
		catch {
			case e: Throwable => {
				logger.error("couldn't highlight pdf", e)
				false
			}
		}
	}

	def copyAndHighlight(): File = {
		val pdfToHighlight = FileUtils.copyFileIntoDirectory(permutation.paper.file, targetFolder, filenamePrefix)

		highlight(pdfToHighlight)
		pdfToHighlight
	}
}
