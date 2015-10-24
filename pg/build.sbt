Common.libSettings("postgresql-async-play")

libraryDependencies ++= Common.commonDependencies ++: Seq(
	Common.postgresDriver
)



