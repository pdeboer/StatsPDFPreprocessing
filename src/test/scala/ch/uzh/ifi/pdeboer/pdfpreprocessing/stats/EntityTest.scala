package ch.uzh.ifi.pdeboer.pdfpreprocessing.stats

import java.io.File

import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.Journal
import org.junit.{Assert, Test}

/**
  * Created by pdeboer on 17/11/15.
  */
class JournalTest {
	@Test def testColumnCounter: Unit = {
		Assert.assertEquals(1, Journal("asdf_1col", new File(".")).numColumns)
		Assert.assertEquals(2, Journal("asdf", new File(".")).numColumns)
		Assert.assertEquals(3, Journal("asdf_3col", new File(".")).numColumns)
	}

	@Test def testJournalName: Unit = {
		Assert.assertEquals("asdff", Journal("asdff_1col", new File(".")).nameWithoutCol)
		Assert.assertEquals("asdff", Journal("asdff", new File(".")).nameWithoutCol)
		Assert.assertEquals("asdff", Journal("asdff_3col", new File(".")).nameWithoutCol)
	}
}
