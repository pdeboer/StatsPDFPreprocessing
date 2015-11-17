package ch.uzh.ifi.pdeboer.pdfpreprocessing.stats

import ch.uzh.ifi.pdeboer.pdfpreprocessing.TestingPaper
import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.StatisticalMethod

/**
 * Created by pdeboer on 30/10/15.
 */
class TermMergerTest {
	//deactivated for now
	//@Test
	def testSimpleMerging: Unit = {
		val methodName = "my method"
		val m = StatisticalMethod(methodName, Nil, Nil, 8 + methodName.length)
		val paper = new TestingPaper(List(s"bla $methodName blupp $methodName", s"some more text where the method doesn't belong to the previous group $methodName", s"but $methodName belongs to page 2"))
		val terms = new StatTermSearcher(paper, false, List(m)).occurrences
		val res = new TermMerger(terms).mergeAllMethods
		println(res)
		//Assert.assertTrue(terms.nonEmpty)
	}
}
