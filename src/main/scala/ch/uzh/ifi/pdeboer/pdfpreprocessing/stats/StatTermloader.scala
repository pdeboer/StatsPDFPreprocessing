package ch.uzh.ifi.pdeboer.pdfpreprocessing.stats

import ch.uzh.ifi.pdeboer.pdfpreprocessing.entities.{StatisticalMethod, StatisticalAssumption}

import scala.collection.mutable
import scala.io.Source

/**
 * Created by pdeboer on 16/06/15.
 */
object StatTermloader {
	lazy val deltas = {

		val methodToDelta = Source.fromFile("deltas.csv", "UTF-8").getLines().map(l => {
			val cols = l.split(",")
			(cols(0), cols(1).toInt)
		}).toList

		methodToDelta
	}

	lazy val terms: List[StatisticalMethod] = {

		def getTermCSV(filename: String) = Source.fromFile(filename, "UTF-8").getLines().map(l => {
			val cols = l.split(",").map(_.trim)
			(cols(0), cols.drop(1).toList)
		}).toList

		val assumptionsInCSV = getTermCSV("assumptions.csv").map(a => StatisticalAssumption(a._1, a._2))
		val methodNamesAndSynonyms = getTermCSV("methods.csv")

		var methodMap = new mutable.HashMap[String, List[StatisticalAssumption]]()
		Source.fromFile("met2ass.csv", "UTF-8").getLines().foreach(l => {
			val cols = l.split(",").map(_.trim)

			val assumption = assumptionsInCSV.find(_.name == cols(1)).getOrElse(throw new Exception(cols(1)))
			methodMap += cols(0) -> (assumption :: methodMap.getOrElse(cols(0), Nil))
		})

		val methods = methodMap.map { case (method, assumptions) =>
			val methodAndSynonym = methodNamesAndSynonyms.find(_._1 == method).get
			StatisticalMethod(methodAndSynonym._1, methodAndSynonym._2, assumptions)
		}

		methods
	}.toList

	def getDeltaForMethod(method: String): Int = {
		try {
			val value = deltas.find(_._1.equalsIgnoreCase(method)).getOrElse((method, 0))
			value._2
		} catch {
			case e: Exception => 0
		}
	}

	def getMethodAndSynonymsFromMethodName(method: String): Option[StatisticalMethod] = {
		terms.find(m => m.name.equalsIgnoreCase(method))
	}

}