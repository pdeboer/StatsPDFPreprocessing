package ch.uzh.ifi.pdeboer.pdfpreprocessing.pdf

import java.io.File

import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.{Paper, Journal}
import ch.uzh.ifi.pdeboer.pdfpreprocessing.util.FileUtils

/**
  * Created by pdeboer on 16/10/15.
  */
class PDFLoader(path: File) {
  def papers: Array[Paper] = {
    path.listFiles().par.flatMap(journalDir => {
      getPDFFiles(journalDir).map(f => extractPaper(journalFromFolder(journalDir), f))
    }).filter(_.isDefined).map(_.get).toArray
  }

  def getPDFFiles(folder: File): List[File] = {
    val descendantFiles = folder.listFiles().filter(f => f.isDirectory).flatMap(f => getPDFFiles(f)).toList
    val myPDFs = folder.listFiles().filter(f => {
      val isPDFFile = f.getName.toLowerCase().endsWith(".pdf") && !f.getName.startsWith("._")
      val limitBMCTo2014 = !f.getParentFile.getName.startsWith("bmc_") || f.getParentFile.getName.startsWith("bmc_") && f.getName.startsWith("2014_")
      val limitCHITo2014 = !f.getParentFile.getName.startsWith("CHI-") || f.getParentFile.getName == "CHI-2014"
      isPDFFile && limitBMCTo2014 && limitCHITo2014
    }).toList
    myPDFs ::: descendantFiles
  }

  private def journalFromFolder(journalDir: File): Journal = {
    val journal = Journal(journalDir.getName, journalDir)
    journal
  }

  private def extractPaper(journal: Journal, paperFile: File): Option[Paper] = {
    if (paperFile.getName.endsWith(".pdf"))
      Some(new Paper(FileUtils.filenameWithoutExtension(paperFile), paperFile, journal))
    else None
  }
}
