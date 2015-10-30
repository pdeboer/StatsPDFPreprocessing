package ch.uzh.ifi.pdeboer.pdfpreprocessing.sampling

import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.StatisticalMethod
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 30/10/15.
 */
class PaperMethodMapTest {

	val m1 = StatisticalMethod("my method", Nil, Nil)
	val m2 = StatisticalMethod("another method", Nil, Nil)

	@Test
	def testEquals: Unit = {
		val md1 = new MethodDistribution(Map(m1 -> 2, m2 -> 3))
		val md2 = new MethodDistribution(Map(m2 -> 3, m1 -> 2))
		Assert.assertEquals(md1, md2)
	}

	@Test
	def testGreaterThan: Unit = {
		val md1 = new MethodDistribution(Map(m1 -> 3, m2 -> 3))
		val md2 = new MethodDistribution(Map(m2 -> 3, m1 -> 2))
		Assert.assertTrue(md1.hasMoreOccurrencesForAtLeastOneMethod(md2))
		Assert.assertFalse(md2.hasMoreOccurrencesForAtLeastOneMethod(md1))
		Assert.assertFalse(md2.hasMoreOccurrencesForAtLeastOneMethod(md2))

		val md3 = new MethodDistribution(Map(m1 -> 5))
		Assert.assertTrue(md3.hasMoreOccurrencesForAtLeastOneMethod(md1))
		Assert.assertTrue(md2.hasMoreOccurrencesForAtLeastOneMethod(md3))

	}
}
