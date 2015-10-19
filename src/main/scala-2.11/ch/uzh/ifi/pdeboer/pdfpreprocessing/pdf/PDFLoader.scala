package ch.uzh.ifi.pdeboer.pdfpreprocessing.pdf

import java.io.File

import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.{Paper, Journal}

/**
 * Created by pdeboer on 16/10/15.
 */
class PDFLoader(path: File) {
	def getPapers() = {
		path.listFiles().flatMap(journalDir => {
			val journal = Journal(journalDir.getName, journalDir)
			journalDir.listFiles().map(paperFile => {
				if (paperFile.getName.endsWith(".pdf"))
					Some(Paper(paperFile.getName, paperFile, journal))
				else None
			}).filter(_.isDefined)
		}).map(_.get)
	}
}
