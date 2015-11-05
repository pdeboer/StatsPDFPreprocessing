package ch.uzh.ifi.pdeboer.pdfpreprocessing

import java.io.File
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import ch.uzh.ifi.pdeboer.pdfpreprocessing.pdf.PDFLoader
import ch.uzh.ifi.pdeboer.pdfpreprocessing.sampling.{MethodDistribution, PaperMethodMap, PaperSelection}
import ch.uzh.ifi.pdeboer.pdfpreprocessing.stats.StatTermSearcher
import ch.uzh.ifi.pdeboer.pplib.process.entities.FileProcessMemoizer
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable

/**
 * Created by pdeboer on 30/10/15.
 */
object PaperSampler extends App with LazyLogging {
	logger.info("starting sampling")
	val mem = new FileProcessMemoizer("everything")

	val conf = ConfigFactory.load()
	val INPUT_DIR = conf.getString("highlighter.pdfSourceDir")
	val PERCENTAGE = conf.getDouble("sampler.targetPercentage")

	val allPapers = mem.mem("papers")(new PDFLoader(new File(INPUT_DIR)).papers)
	val allPaperMethodMaps = allPapers.map(p => new StatTermSearcher(p, includeAssumptions = false).occurrences.toList)
		.filter(_.nonEmpty).map(p => PaperMethodMap.fromOccurrenceList(p)).toList

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

		def unexploredSelections = allPaperMethodMaps.filterNot(m => paperSelection.papers.contains(m))

			.map(p => paperSelection.newSelectionWithPaper(p))
	}

	var closedSet = List.empty[PaperSelection]
	val openSet = new mutable.PriorityQueue[OrderablePaperSelection]()
	openSet += new OrderablePaperSelection(new PaperSelection(Nil))
	var best = new OrderablePaperSelection(new PaperSelection(Nil))

	val t = new Thread {
		val counter = new AtomicInteger(0)
		val discovered = new AtomicLong(0)
		val processed = new AtomicLong(0)
		var closestDistance = new AtomicInteger(Integer.MAX_VALUE)

		this.setDaemon(true)

		override def run(): Unit = {
			while (true) {
				Thread.sleep(1000)
				println(s"tried $counter variations. Discovered $discovered, processed $processed. closest distance $closestDistance")
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
						if (!openSet.toList.contains(ops)) {
							openSet.enqueue(ops)
							if (ops.f < best.f) {
								PaperSampler.synchronized {
									best = ops
								}
								t.closestDistance.set(best.f)
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

