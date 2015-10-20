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

	def copyFileIntoDirectory(source: File, destination: String): File = {
		val destinationFile = new File(destination + source.getName)

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
}
