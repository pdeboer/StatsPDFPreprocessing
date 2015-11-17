package ch.uzh.ifi.pdeboer.pdfpreprocessing.util

import java.io.File

import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger

/**
  * Created by pdeboer on 16/10/15.
  */
object FileUtils extends LazyLogger {
	def emptyDir(dir: File): Boolean = {
		dir.listFiles().par.foreach(file => {
			if (file.isDirectory) {
				emptyDir(file)
			}
			file.delete()
		})
		true
	}

	def copyFileIntoDirectory(source: File, destination: String, filename: Option[String] = None, createFolders: Boolean = true): File = {
		val destinationFile = new File(destination + filename.getOrElse(source.getName))
		if (createFolders) destinationFile.mkdirs()

		try {
			org.codehaus.plexus.util.FileUtils.copyFile(source, destinationFile)
			destinationFile
		} catch {
			case e: Exception => {
				logger.error(s"Cannot copy file $source to $destinationFile", e)
				null
			}
		}
	}

	def filenameWithoutExtension(file: File) = {
		val filename: String = file.getName
		val lastDotIndex = filename.lastIndexOf(".")

		if (filename.length - lastDotIndex > 4)
			filename // doesnt have an extension
		else filename.substring(0, lastDotIndex)
	}
}
