/*
 * Copyright 2015 Stephen Couchman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.greyscribes.async.db.postgresql

import java.net.URI

import com.github.mauricio.async.db.{Configuration ⇒ DBConfiguration}
import org.specs2.mock.Mockito
import org.specs2.mutable._
import play.api.{Configuration, Environment, PlayException}

import scala.concurrent.duration._

class PostgreSQLConnectionPoolSpecification extends Specification with Mockito {

	"The PostgreSQLConnectionPool uri parser" should {
		import PostgreSQLConnectionPool._

		"recognize a jdbc:postgresql:dbname uri" in {
			parseURI(new URI("jdbc:postgresql:dbname")) mustEqual Some(DBConfiguration(
				username = "postgres",
				database = Some("dbname")
			))
		}

		"recognise a postgresql:// uri" in {
			parseURI(new URI("postgresql://localhost:425/dbname")) mustEqual Some(DBConfiguration(
				username = "postgres",
				database = Some("dbname"),
				port = 425,
				host = "localhost"
			))
		}

		"recognize a jdbc:postgresql:// uri" in {
			parseURI(new URI("jdbc:postgresql://localhost:425/dbname")) mustEqual Some(DBConfiguration(
				username = "postgres",
				database = Some("dbname"),
				port = 425,
				host = "localhost"
			))
		}

		"pull the username and password from URI credentials" in {
			parseURI(new URI("jdbc:postgresql://user:password@localhost:425/dbname")) mustEqual Some(DBConfiguration(
				username = "user",
				password = Some("password"),
				database = Some("dbname"),
				port = 425,
				host = "localhost"
			))
		}

		"pull the username and password from query string" in {
			parseURI(new URI("jdbc:postgresql://localhost:425/dbname?user=user&password=password")) mustEqual Some(DBConfiguration(
				username = "user",
				password = Some("password"),
				database = Some("dbname"),
				port = 425,
				host = "localhost"
			))
		}

		// Included for consistency, so later changes aren't allowed to change behavior
		"use the query string parameters to override URI credentials" in {
			parseURI(new URI("jdbc:postgresql://baduser:badpass@localhost:425/dbname?user=user&password=password")) mustEqual Some(DBConfiguration(
				username = "user",
				password = Some("password"),
				database = Some("dbname"),
				port = 425,
				host = "localhost"
			))
		}
	}

	private def getConfig(file: String) =
		Configuration.load(Environment.simple(), Map("config.resource" → file))

	"The PostgreSQLConnectionPoolSpecification" should {
		"ensure test resources are correctly placed" in {
			val conf = getConfig("resource test.conf")
			conf.getString("testKey") mustEqual Some("testValue")
		}
	}


	"The PostgreSQLConnectionPool Database Configuration Builder" should {
		import PostgreSQLConnectionPool._

		"successfully read a single default configuration" in {
			val config = getConfig("just default.conf")
			val dbConfig = buildDBConfiguration(config)

			dbConfig("default") mustEqual DBConfiguration(
				//postgresql://user:password@localhost:432/dbname
				username = "user",
				password = Some("password"),
				host = "localhost",
				port = 432,
				database = Some("dbname")
			)
		}

		"ignore other drivers" in {
			val config = getConfig("ignore other drivers.conf")
			val dbConfig = buildDBConfiguration(config)

			dbConfig must beEmpty
		}

		"handle settings overrides" in {
			val config = getConfig("settings overrides.conf")
			val dbConfig = buildDBConfiguration(config)

			dbConfig("default") mustEqual DBConfiguration(
				//postgresql://user:password@localhost:432/dbname
				username = "overrideuname",
				password = Some("overridepassword"),
				host = "localhost",
				port = 432,
				database = Some("dbname"),
				maximumMessageSize = 1024 * 1024 * 24,
				connectTimeout = 12.seconds,
				testTimeout = 6.seconds,
				queryTimeout = Some(10.seconds)
			)
		}

		"throw a PlayException on a bad URI" in {
			val config = getConfig("bad uri.conf")
			buildDBConfiguration(config) must throwA[PlayException]
		}

		"throw a PlayException on a bad charset" in {
			val config = getConfig("bad charset.conf")
			buildDBConfiguration(config) must throwA[PlayException]
		}

		"successfully read multiple configurations from a single config" in {
			val config = getConfig("multiple.conf")
			val dbConfig = buildDBConfiguration(config)

			dbConfig.contains("someoneElse") aka "contains the wrong driver's data" must beFalse

			dbConfig("default") mustEqual DBConfiguration(
				//postgresql://user:password@localhost:432/dbname
				username = "user",
				password = Some("password"),
				host = "localhost",
				port = 432,
				database = Some("dbname")
			)

			dbConfig("oranges") mustEqual DBConfiguration(
				//postgresql://user:password@localhost:432/orangedb
				username = "user",
				password = Some("password"),
				host = "localhost",
				port = 432,
				database = Some("orangedb")
			)

			dbConfig("blues") mustEqual DBConfiguration(
				//postgresql://blueuser:password@localhost:432/bluedb
				username = "blueuser",
				password = Some("password"),
				host = "localhost",
				port = 432,
				database = Some("bluedb")
			)
		}

	}


}
