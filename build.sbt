Common.settings("db-async-base")

lazy val base = (project in file("."))
		.aggregate(common, postgresql, mysql, sample)

lazy val common = (project in file("common"))


lazy val postgresql = (project in file("pg"))
	.dependsOn(common % "compile->compile;test->test")

lazy val mysql = (project in file("mysql"))
	.dependsOn(common % "compile->compile;test->test")


lazy val sample = (project in file("sample"))
		.enablePlugins(PlayScala)
		.dependsOn(postgresql, mysql, common)
		.aggregate(postgresql, mysql, common)

//libraryDependencies ++= Common.commonDependencies

publishMavenStyle := false

