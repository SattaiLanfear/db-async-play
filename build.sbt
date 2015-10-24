Common.settings("db-async-base")

lazy val base = (project in file("."))
	.aggregate(common, postgresql, mysql)

lazy val common = (project in file("common"))


lazy val postgresql = (project in file("pg"))
	.dependsOn(common)

lazy val mysql = (project in file("mysql"))
	.dependsOn(common)


libraryDependencies ++= Common.commonDependencies



