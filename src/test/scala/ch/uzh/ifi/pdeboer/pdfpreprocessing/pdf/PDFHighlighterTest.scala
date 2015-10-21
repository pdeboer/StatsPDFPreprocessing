package ch.uzh.ifi.pdeboer.pdfpreprocessing.pdf

import java.io.File

import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.{Journal, Paper}
import ch.uzh.ifi.pdeboer.pdfpreprocessing.stats.PDFPermutation
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 21/10/15.
 */
class PDFHighlighterTest {
	@Test
	def testTargetFolder: Unit = {
		val perm = PDFPermutation(Paper("asdf(a[s]df)as df", new File(""), Journal(basePath = new File("."))), Nil)
		val highlighter = new PDFHighlighter(perm)
		Assert.assertEquals("asdfasdfasdf", highlighter.targetFilename)
	}
}
