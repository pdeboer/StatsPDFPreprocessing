package ch.uzh.ifi.pdeboer.pdfpreprocessing

import java.io.File

import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.Paper
import ch.uzh.ifi.pdeboer.pdfpreprocessing.pdf.PDFLoader
import ch.uzh.ifi.pdeboer.pdfpreprocessing.stats._
import com.github.tototoshi.csv.CSVWriter
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable

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
    val combinationsOfMethodsAndAssumptions: List[PDFPermutation] = new StatTermPermuter(statTermsInPaper).permutations
    PaperSnippets(paper, combinationsOfMethodsAndAssumptions)
  }).flatMap(ps => ps.snippets.map(sni => (ps.paper, sni.method, sni.assumption)))


  val wr = CSVWriter.open("all_snippets.csv")
  wr.writeRow(List("paper", "journal", "method", "assumption"))
  wr.writeAll(snippets.map(sni => List(sni._1.file.getName, sni._1.journal.name, sni._2.name, sni._3.name)).toList)
  var papersWithoutSnippets = mutable.HashSet.empty[Paper] ++ allPapers.toSet
  snippets.foreach(s => papersWithoutSnippets -= s._1)
  private val csvVersion: List[List[String]] = papersWithoutSnippets.toList.map(p => List(p.file.getName, p.journal.name))
  wr.writeRow(csvVersion)
  wr.close()

  case class PaperSnippets(paper: Paper, snippets: List[PDFPermutation])

}