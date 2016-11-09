package ch.uzh.ifi.pdeboer.pdfpreprocessing

import java.io.File

import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.Paper
import ch.uzh.ifi.pdeboer.pdfpreprocessing.pdf.PDFLoader
import ch.uzh.ifi.pdeboer.pdfpreprocessing.stats._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

/**
  * Created by pdeboer on 16/10/15.
  */
object SnippetCalculator extends App with LazyLogging {
  logger.debug("starting scan")

  val conf = ConfigFactory.load()

  private val path: File = new File("/home/user/pdeboer/allPapers") //new File("/Users/pdeboer/ownCloud/Privat/schule/phd/projects/stats Mike/all papers")

  val allPapers = new PDFLoader(path).papers
  println("all journals: " + allPapers.map(_.journal).toSet.mkString(", "))

  val snippets = allPapers.par.map(paper => {
    val searcher = new StatTermSearcher(paper)
    val statTermsInPaper = new StatTermPruning(List(new PruneTermsWithinOtherTerms)).prune(searcher.occurrences)
    val combinationsOfMethodsAndAssumptions = new StatTermPermuter(statTermsInPaper).permutations
    PaperSnippets(paper, combinationsOfMethodsAndAssumptions.size)
  })


  snippets.groupBy(_.paper.journal).foreach(j => {
    val snippetCount = j._2.map(_.snippets).sum
    println(s"${j._1.name}  $snippetCount")
  })

  case class PaperSnippets(paper: Paper, snippets: Int)

}