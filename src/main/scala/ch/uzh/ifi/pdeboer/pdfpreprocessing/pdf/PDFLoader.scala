package ch.uzh.ifi.pdeboer.pdfpreprocessing.pdf

import java.io.File

import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.{Paper, Journal}
import ch.uzh.ifi.pdeboer.pdfpreprocessing.util.FileUtils

/**
 * Created by pdeboer on 16/10/15.
 */
class PDFLoader(path: File) {
	def papers = {
		path.listFiles().flatMap(journalDir => {
			val resultForJournalFolder = Option(journalDir.listFiles()).getOrElse(Array.empty[File]).map(paperFile => {
				extractPaper(journalFromFolder(journalDir), paperFile)
			}).filter(_.isDefined)

			if (resultForJournalFolder.nonEmpty) resultForJournalFolder else List(extractPaper(journalFromFolder(path), journalDir))
		}).map(_.get)
	}

	private def journalFromFolder(journalDir: File): Journal = {
		val journal = Journal(journalDir.getName, journalDir)
		journal
	}

	private def extractPaper(journal: Journal, paperFile: File): Option[Paper] = {
		if (paperFile.getName.endsWith(".pdf"))
			Some(Paper(FileUtils.filenameWithoutExtension(paperFile), paperFile, journal))
		else None
	}
}
