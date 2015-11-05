package ch.uzh.ifi.pdeboer.pdfpreprocessing

import java.io.File
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import ch.uzh.ifi.pdeboer.pdfpreprocessing.pdf.PDFLoader
import ch.uzh.ifi.pdeboer.pdfpreprocessing.sampling.{MethodDistribution, PaperMethodMap, PaperSelection}
import ch.uzh.ifi.pdeboer.pdfpreprocessing.stats.StatTermSearcher
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable

/**
 * Created by pdeboer on 30/10/15.
 */
object PaperSampler extends App with LazyLogging {
	logger.info("starting sampling")

	val conf = ConfigFactory.load()
	val INPUT_DIR = conf.getString("highlighter.pdfSourceDir")
	val PERCENTAGE = conf.getDouble("sampler.targetPercentage")

	val allPapers = new PDFLoader(new File(INPUT_DIR)).papers
	val allPaperMethodMaps: Set[PaperMethodMap] = allPapers.map(p => new StatTermSearcher(p, includeAssumptions = false).occurrences.toList)
		.filter(_.nonEmpty).map(p => PaperMethodMap.fromOccurrenceList(p)).toSet

	val targetDistribution = new MethodDistribution(
		new PaperSelection(allPaperMethodMaps).methodOccurrenceMap.map(e => e._1 -> (e._2 * PERCENTAGE).toInt))
	logger.info("complete distribution is " + new PaperSelection(allPaperMethodMaps))
	logger.info(s"target distribution is $targetDistribution")

	case class OrderablePaperSelection(paperSelection: PaperSelection) extends Comparable[OrderablePaperSelection] {
		lazy val f = paperSelection.distanceTo(targetDistribution.methodOccurrenceMap)
		lazy val g = paperSelection.vectorLength

		override def compareTo(o: OrderablePaperSelection): Int = {
			-1 * f.compareTo(o.f)
		}

		def unexploredSelections = allPaperMethodMaps.diff(paperSelection.papers)
			.map(p => paperSelection.newSelectionWithPaper(p))
	}

	var closedSet = List.empty[PaperSelection]
	val openSet = new mutable.PriorityQueue[OrderablePaperSelection]()
	openSet += new OrderablePaperSelection(new PaperSelection(Set.empty[PaperMethodMap]))
	var best = new OrderablePaperSelection(new PaperSelection(Set.empty[PaperMethodMap]))

	val t = new Thread {
		val counter = new AtomicInteger(0)
		val discovered = new AtomicLong(0)
		val processed = new AtomicLong(0)

		this.setDaemon(true)

		override def run(): Unit = {
			while (true) {
				Thread.sleep(1000)
				println(s"tried $counter variations. Discovered $discovered, processed $processed")
				counter.set(0)
			}
		}
	}

	t.start()

	while (openSet.nonEmpty) {
		val current = openSet.dequeue()
		t.processed.incrementAndGet()
		t.counter.incrementAndGet()
		if (current.f == 0) {
			println("found working selection!")
			current.paperSelection.persist("target.csv")
			System.exit(0)
		} else {
			closedSet = current.paperSelection :: closedSet
			current.unexploredSelections.par.foreach(s => {
				if (!closedSet.contains(s)) {
					val ops = OrderablePaperSelection(s)
					openSet.synchronized {
						if (!openSet.toSet.contains(ops)) {
							openSet.enqueue(ops)
							if (ops.f < best.f) {
								PaperSampler.synchronized {
									best = ops
								}
								println(s"found selection with lower distance: ${best.f}: $best")
								best.paperSelection.persist("tempselection.csv")
							}
							t.discovered.incrementAndGet()
						}
					}
				}
			})
		}
	}
}

