package ch.uzh.ifi.pdeboer.pdfpreprocessing

import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.Paper

/**
 * Created by pdeboer on 30/10/15.
 */
class TestingPaper(_contents: List[String]) extends Paper("some paper", null, null) {
	override lazy val contents = _contents
}
