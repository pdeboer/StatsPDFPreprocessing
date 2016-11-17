package helper.pdfpreprocessing

import java.io.File

import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.StatisticalMethod
import com.github.tototoshi.csv.{CSVReader, CSVWriter}

import scala.io.Source

/**
  * Created by pdeboer on 08.11.16.
  */
object CountMethods extends App {
  val path = new File("/Users/pdeboer/ownCloud/Privat/schule/phd/projects/stats Mike/all papers txt")
  //val path = new File("testpdfs/CHI")

  val methods: List[StatisticalMethod] = CSVReader.open(new File("methods.csv")).all().map(t => StatisticalMethod(t.head, t.drop(1), Nil))
  val journals: Array[File] = path.listFiles().filter(_.isDirectory)
  val journalPapers = journals.map(j => j -> getTXTFiles(j).par.map(p => new TXTPaper(p, j.getName))).toMap

  val wr = CSVWriter.open("method_occurrences.csv")
  wr.writeRow("journals" :: methods.map(_.name).toList)
  val data = journalPapers.map(j => {
    j._1.getName :: methods.map(m => j._2.count(p => p.methodMap(m)).toDouble / j._2.length.toDouble)
  }).toList
  wr.writeAll(data)
  wr.close()

  val wr2 = CSVWriter.open("methods_per_paper.csv")
  wr2.writeRow(List("paper", "journal") ::: methods.map(_.name))
  wr2.writeAll(journalPapers.flatMap(j => j._2).map(p => List(p.path.getAbsolutePath, p.journal) ::: methods.map(m => if (p.methodMap(m)) 1 else 0)).toList)
  wr2.close()

  def getTXTFiles(folder: File): List[File] = {
    val descendantFiles = folder.listFiles().filter(f => f.isDirectory).flatMap(f => getTXTFiles(f)).toList
    val myPDFs = folder.listFiles().filter(f => {
      val isTxtFile = f.getName.toLowerCase().endsWith(".txt")
      val limitBMCTo2014 = !f.getParentFile.getName.startsWith("bmc_") || f.getParentFile.getName.startsWith("bmc_") && f.getName.startsWith("2014_")
      val limitCHITo2014 = !f.getParentFile.getName.startsWith("CHI-") || f.getParentFile.getName == "CHI-2014"
      isTxtFile && limitBMCTo2014 && limitCHITo2014
    }).toList
    myPDFs ::: descendantFiles
  }

  class TXTPaper(val path: File, val journal: String) {
    val methodMap = {
      val data = Source.fromFile(path).getLines().mkString("\n").toLowerCase()
      val r = methods.map(m => m -> methodOccurrences(data, m)).toMap
      println(s"analysed paper $path")
      r
    }

    def methodOccurrences(txt: String, method: StatisticalMethod) = {
      val terms = method.searchTerm :: method.synonyms.map(_.toLowerCase().trim)
      val matches = terms.flatMap(searchTerm => {
        val regexes: List[String] = buildRegexForString(searchTerm.toLowerCase)
        regexes.flatMap(r => {
          val regex = r.r
          regex.findAllMatchIn(txt)
        })
      })
      matches.nonEmpty
    }
  }

  def buildRegexForString(searchString: String): List[String] = {
    import ch.uzh.ifi.pdeboer.pdfpreprocessing.stats.StatTermSearcher._
    val searchStringInclSuffixes = if (searchString.length < 7) addLikelySuffixesAndPostfixesToMethods(searchString) else List(searchString)

    if (searchString.length < 7)
      searchStringInclSuffixes.map(search => {
        "(\\b" + addRegexToAllowSpaces(search) + "\\b)"
      })
    else
      List("(?i)(" + addRegexToAllowSpaces(searchString) + ")")
  }

  private def addLikelySuffixesAndPostfixesToMethods(t: String): List[String] = {
    List(t, t + "s")
  }
}
