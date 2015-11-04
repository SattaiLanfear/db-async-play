import play.sbt.PlayImport
import sbt.Keys._
import sbt._


object Common {

	val dbDriverVersion = "0.2.18"
	val dbDriverGroup = "com.github.mauricio"
	val postgresDriver = dbDriverGroup %% "postgresql-async" % dbDriverVersion
	val mysqlDriver = dbDriverGroup %% "mysql-async" % dbDriverVersion
	val commonDependencies = Seq(
		"com.typesafe.play" %% "play" % "2.4.+" % "provided",
		"com.typesafe.play" %% "play-test" % "2.4.+" % "test",
		PlayImport.specs2 % "test"
	)

	def libSettings(_name: String) = settings(_name) ++: Seq(
		crossScalaVersions := "2.11.7" :: Nil,
		// Begin publish settings
		publishMavenStyle := true,
		publishTo <<= version { v: String ⇒
			val nexus = "https://oss.sonatype.org/"
			if(v.trim.endsWith("SNAPSHOT"))
				Some("snapshots" at nexus + "content/repositories/snapshots")
			else
				Some("releases" at nexus + "service/local/staging/deploy/maven2")
		},
		pomIncludeRepository := { _ ⇒ false },
		licenses := Seq("APACHE-2.0" → url("http://www.apache.org/licenses/LICENSE-2.0")),
		homepage := Some(url("https://github.com/SattaiLanfear/db-async-play")),
		pomExtra :=
				<scm>
					<url>git@github.com:SattaiLanfear/db-async-play.git</url>
					<connection>scm:git:git@github.com:SattaiLanfear/db-async-play.git</connection>
				</scm>
						<developers>
							<developer>
								<id>SattaiLanfear</id>
								<name>Stephen Couchman</name>
								<url>https://github.com/SattaiLanfear</url>
							</developer>
						</developers>
	)

	def settings(_name: String) = Seq(
		name := _name,
		version := "0.1.0",
		organization := "com.greyscribes",
		scalaVersion := "2.11.7",
		scalacOptions ++= Seq(
			"-deprecation", // Emit warning and location for usages of deprecated APIs.
			"-feature", // Emit warning and location for usages of features that should be imported explicitly.
			"-unchecked", // Enable additional warnings where generated code depends on assumptions.
			"-Xfatal-warnings", // Fail the compilation if there are any warnings.
			"-Xlint", // Enable recommended additional warnings.
			"-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
			"-Ywarn-dead-code", // Warn when dead code is identified.
			"-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
			"-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
			"-Ywarn-numeric-widen", // Warn when numerics are widened.
			"-Ywarn-unused"
		),
		resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
	)

}

