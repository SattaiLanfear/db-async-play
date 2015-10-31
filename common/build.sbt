Common.libSettings("dbcommon-async-play")

libraryDependencies ++= Common.commonDependencies ++: Seq(
	Common.dbDriverGroup %% "db-async-common" % Common.dbDriverVersion
)



