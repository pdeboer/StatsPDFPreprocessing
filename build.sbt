name := "StatsPDFPreprocessing"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
	"org.apache.pdfbox" % "pdfbox" % "1.8.10",
	"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
	"org.scalikejdbc" %% "scalikejdbc" % "2.2.7",
	//"ch.qos.logback" % "logback-classic" % "1.1.3",
	"com.github.tototoshi" %% "scala-csv" % "1.2.2",
	"mysql" % "mysql-connector-java" % "5.1.36",
	"pdeboer" %% "pplib" % "0.1-SNAPSHOT",

	"junit" % "junit" % "4.8.1" % "test",
	"org.scalacheck" %% "scalacheck" % "1.12.4" % "test",
	"org.mockito" % "mockito-core" % "1.10.19" % "test",
	"com.novocode" % "junit-interface" % "0.8" % "test->default"
)

//assemblyJarName in assembly := "preprocessor.jar"

test in assembly := {}

//mainClass in assembly := Some("ch.uzh.ifi.pdeboer.pdfpreprocessing.PreprocessPDF")
mainClass in assembly := Some("ch.uzh.ifi.pdeboer.pdfpreprocessing.PaperSampler")

assemblyMergeStrategy in assembly := {
	case "log4j.properties" => MergeStrategy.concat
	case "application.conf" => MergeStrategy.concat
	case "application.conf_default" => MergeStrategy.discard
	case "StaticLoggerBinder.class" => MergeStrategy.first
	case "StaticMDCBinder.class" => MergeStrategy.first
	case "StaticMarkerBinder.class" => MergeStrategy.first
	case x =>
		val oldStrategy = (assemblyMergeStrategy in assembly).value
		oldStrategy(x)
}