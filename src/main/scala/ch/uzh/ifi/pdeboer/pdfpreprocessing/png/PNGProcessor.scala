package ch.uzh.ifi.pdeboer.pdfpreprocessing.png

import java.awt.Color
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

import ch.uzh.ifi.pdeboer.pdfpreprocessing.stats.PDFPermutation
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import org.codehaus.plexus.util.FileUtils

/**
 * Created by Mattia
 * With some modifications by pdeboer
 */
class PNGProcessor(pngImage: File, pdfPermutation: PDFPermutation, enableCropping: Boolean = true) extends LazyLogger {
	assert(pngImage != null)

	//tolerated color range of highlighted Methods (yellow) and assumptions (green)
	val YELLOW_RANGES = List[(Int, Int)]((190, 255), (190, 255), (100, 160))
	val GREEN_RANGES = List[(Int, Int)]((100, 160), (190, 255), (100, 160))

	val PADDING_SNIPPET = 200
	val MINIMAL_SNIPPET_HEIGHT = 300

	lazy val inputImage = try {
		Some(ImageIO.read(pngImage))
	}
	catch {
		case e: Throwable => logger.error("couldn't load image", e); None
	}

	private lazy val inputImageHeight: Int = inputImage.get.getHeight

	lazy val (yellowCoords: List[Point2D], greenCoords: List[Point2D]) = coordinatesOfHighlights

	def process() = {
		val managerForTargetPNG = if (enableCropping) {
			cropPNG()
			new PNGProcessor(pngImage, pdfPermutation, enableCropping = false)
		} else this

		if (!enableCropping) {
			assert(managerForTargetPNG.yellowCoords.nonEmpty && managerForTargetPNG.greenCoords.nonEmpty, s"no highlightings found in $pngImage")

			val minGreen = managerForTargetPNG.greenCoords.map(_.getY).min / inputImageHeight
			val closestYellow = managerForTargetPNG.yellowCoords.minBy(v => Math.abs((v.getY / inputImageHeight) - minGreen)).getY / inputImageHeight

			val boundaryMin = Math.min(minGreen, closestYellow) * 100
			val boundaryMax = Math.max(minGreen, closestYellow) * 100

			StatTermLocationsInPNG(managerForTargetPNG.isMethodOnTop, boundaryMin, boundaryMax)
		} else StatTermLocationsInPNG(managerForTargetPNG.isMethodOnTop)
	}

	def cropPNG() {
		extractAndGenerateImage()
	}

	def coordinatesOfHighlights: (List[Point2D], List[Point2D]) = {
		val width = inputImage.get.getWidth

		var yellowCoords = List.empty[Point2D]
		var greenCoords = List.empty[Point2D]

		for (x <- 0 until width) {
			for (y <- 0 until inputImageHeight) {
				val color = new Color(inputImage.get.getRGB(x, y))
				if (isSameColor(color, YELLOW_RANGES)) {
					yellowCoords ::= new Point2D.Double(x, y)
				} else if (isSameColor(color, GREEN_RANGES)) {
					greenCoords ::= new Point2D.Double(x, y)
				}
			}
		}
		(yellowCoords, greenCoords)
	}

	def isMethodOnTop: Boolean = {
		try {
			val greenMin: Point2D = greenCoords match {
				case Nil => new Point2D.Double(0.0, 0.0)
				case greenList => greenList.minBy(_.getY)
			}
			val closestYellow: Point2D = yellowCoords match {
				case Nil => new Point2D.Double(0.0, 0.0)
				case yellowList => yellowList.minBy(v => math.abs(v.getY - greenMin.getY))
			}
			if (Math.abs(closestYellow.getY - greenMin.getY) < 5) {
				closestYellow.getX < greenMin.getX
			} else {
				closestYellow.getY < greenMin.getY
			}

		} catch {
			case e: Exception => {
				logger.debug("Cannot find highlight to establish if method or prerequisite is on top.")
				true
			}
		}
	}

	def extractAndGenerateImage() {
		if (greenCoords.nonEmpty && yellowCoords.nonEmpty) {
			val (startY: Int, endY: Int) = extractImageBoundaries()

			val snippetImage: BufferedImage = createImage(inputImage.get, startY, endY)
			ImageIO.write(snippetImage, "png", pngImage)

			logger.debug(s"Snippet successfully written: $pngImage")
		} else {
			logger.error(s"Cannot create snippet. No highlight found in file: ${pngImage.getName}")
			new File("errors_cutting_snippets").mkdir()
			val snippet = new File("errors_cutting_snippets/" + pngImage.getName)
			try {
				FileUtils.copyFile(pngImage, snippet)
			} catch {
				case e: Exception => logger.error(s"Cannot copy file $pngImage to ../errors_cutting_snippets/ directory!", e)
			}
		}
	}

	def createImage(inputImage: BufferedImage, startY: Int, endY: Int): BufferedImage = {
		val snippetHeight = endY - startY
		val imageWidth = inputImage.getWidth

		val snippetImage = new BufferedImage(imageWidth, snippetHeight, BufferedImage.TYPE_INT_RGB)
		for (w <- 0 until imageWidth) {
			for (h <- 0 until snippetHeight) {
				snippetImage.setRGB(w, h, new Color(inputImage.getRGB(w, startY + h)).getRGB)
			}
		}
		snippetImage
	}

	def extractImageBoundaries(): (Int, Int) = {
		val maxHeight = inputImageHeight
		val minGreen = greenCoords.minBy(_.getY)
		val minYellow = yellowCoords.map(y => (Math.abs(minGreen.getY - y.getY), y)).minBy(_._1)._2
		val startY = Math.max(0, Math.min(minYellow.getY, minGreen.getY) - PADDING_SNIPPET)
		val endY = Math.min(Math.max(minYellow.getY, minGreen.getY) + PADDING_SNIPPET, maxHeight)

		checkMinimalBoundaries(startY.toInt, endY.toInt, maxHeight)
	}

	def checkMinimalBoundaries(startY: Int, endY: Int, maxImageHeight: Int): (Int, Int) = {
		var minY = startY
		var maxY = endY
		val originalHeight = maxY - minY
		if (originalHeight < MINIMAL_SNIPPET_HEIGHT) {
			val deltaHeight = (MINIMAL_SNIPPET_HEIGHT - originalHeight) / 2
			if (minY - deltaHeight > 0) {
				minY = minY - deltaHeight
			} else {
				minY = 0
			}
			if (maxY + deltaHeight < maxImageHeight) {
				maxY = maxY + deltaHeight
			} else {
				maxY = maxImageHeight
			}
		}
		(minY, maxY)
	}

	def delta(x: Int, y: Int): Int = {
		Math.abs(x - y)
	}

	def isSameColor(color1: Color, color2: List[(Int, Int)]): Boolean = {
		color1.getRed <= color2.head._2 &&
			color1.getRed >= color2.head._1 &&
			color1.getGreen <= color2(1)._2 &&
			color1.getGreen >= color2(1)._1 &&
			color1.getBlue <= color2.last._2 &&
			color1.getBlue >= color2.last._1

	}
}

case class StatTermLocationsInPNG(methodOnTop: Boolean, relativeTop: Double = 0, relativeBottom: Double = 0)
