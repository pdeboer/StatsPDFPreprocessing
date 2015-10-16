package ch.uzh.ifi.pdeboer.pdfpreprocessing.util

import java.io.File

/**
 * Created by pdeboer on 16/10/15.
 */
object FileUtils {
	def emptyDir(dir: File): Boolean = {
		dir.listFiles().par.foreach(file => {
			if (file.isDirectory) {
				emptyDir(file)
			}
			file.delete()
		})
		true
	}
}
