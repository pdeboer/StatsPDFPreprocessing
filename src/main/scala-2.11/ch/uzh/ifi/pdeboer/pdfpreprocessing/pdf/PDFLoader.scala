package ch.uzh.ifi.pdeboer.pdfpreprocessing.pdf

import java.io.File

import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.{Paper, Journal}
import ch.uzh.ifi.pdeboer.pdfpreprocessing.util.FileUtils

/**
 * Created by pdeboer on 16/10/15.
 */
class PDFLoader(path: File) {
	def getPapers() = {
		path.listFiles().flatMap(journalDir => {
			val journal = Journal(journalDir.getName, journalDir)
			journalDir.listFiles().map(paperFile => {
				if (paperFile.getName.endsWith(".pdf"))
					Some(Paper(FileUtils.filenameWithoutExtension(paperFile), paperFile, journal))
				else None
			}).filter(_.isDefined)
		}).map(_.get)
	}
}
