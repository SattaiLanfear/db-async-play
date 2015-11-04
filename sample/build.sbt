Common.settings("sample")

libraryDependencies ++= Common.commonDependencies ++: Seq(
	filters,
	// Old-style drivers used by Flyway
	"mysql" % "mysql-connector-java" % "5.1.37",
	"org.postgresql" % "postgresql" % "9.4-1202-jdbc42",
	"org.flywaydb" %% "flyway-play" % "2.2.0",
	"de.svenkubiak" % "jBCrypt" % "0.4",
	"org.webjars" % "bootstrap" % "3.3.5"
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
