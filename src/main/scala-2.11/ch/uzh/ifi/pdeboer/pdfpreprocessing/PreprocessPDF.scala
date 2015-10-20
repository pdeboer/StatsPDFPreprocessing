package ch.uzh.ifi.pdeboer.pdfpreprocessing

import java.io.File

import ch.uzh.ifi.pdeboer.pdfpreprocessing.pdf.{PDFHighlighter, PDFLoader}
import ch.uzh.ifi.pdeboer.pdfpreprocessing.stats.{StatTermSearcher, StatTermloader}
import ch.uzh.ifi.pdeboer.pdfpreprocessing.util.FileUtils
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

/**
 * Created by pdeboer on 16/10/15.
 */
object PreprocessPDF extends App with LazyLogging {
	logger.debug("starting highlighting")

	val conf = ConfigFactory.load()

	val INPUT_DIR = conf.getString("highlighter.pdfSourceDir")
	val OUTPUT_DIR = conf.getString("highlighter.snippetDir")
	val CONVERT_CMD = conf.getString("highlighter.convertCmd")
	val PERMUTATIONS_CSV_FILENAME = conf.getString("highlighter.permutationFilename")

	FileUtils.emptyDir(new File(OUTPUT_DIR))

	val allPapers = new PDFLoader(new File(INPUT_DIR)).getPapers()
	allPapers.par.foreach(paper => {
		val searcher = new StatTermSearcher(paper, StatTermloader.terms)
		val highlighter = new PDFHighlighter(searcher.occurrences, OUTPUT_DIR).copyAndHighlight()
	})

}