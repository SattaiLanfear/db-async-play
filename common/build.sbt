Common.libSettings("db-async-common")

libraryDependencies ++= Common.commonDependencies ++: Seq(
	Common.dbDriverGroup %% "db-async-common" % Common.dbDriverVersion
)



