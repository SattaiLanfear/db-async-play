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
import javax.inject.{Inject, Singleton}

import com.github.mauricio.async.db.pool.ObjectFactory
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.postgresql.pool.PostgreSQLConnectionFactory
import com.github.mauricio.async.db.{Configuration ⇒ DBConfiguration}
import com.greyscribes.async.db.{ConfigurationBuilder, LifecycleBoundConnectionPoolGroup}
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Logger}
import play.core.parsers.FormUrlEncodedParser


/**
 * The primary purpose of this class is to create a strong type for the dependency injector.
 * @param config the Play Configuration from which to gather our configuration(s).
 * @param lifecycle the Play Lifecycle object, so that this class can self-register for shutdown.
 */
@Singleton()
class PostgreSQLConnectionPool @Inject()(config: Configuration,
                                         lifecycle: ApplicationLifecycle)
	extends LifecycleBoundConnectionPoolGroup[PostgreSQLConnection](config, PostgreSQLConnectionPool, lifecycle)


/**
 * This companion object acts as the PostgresSQLConnection ConfigurationBuilder, providing Configuration parsing
 * services to PostgreSQLConnectionPool.
 */
object PostgreSQLConnectionPool extends ConfigurationBuilder[PostgreSQLConnection] {

	/**
	 * @return the logger to use for this class
	 */
	override protected val logger: Logger = Logger(classOf[PostgreSQLConnectionPool])
	/**
	 * @return the name of the currently processing database driver, as it should be entered on the db.*.driver line
	 */
	override protected val getDriverName: String = "postgresql-async"

	override protected def buildFactory(config: DBConfiguration): ObjectFactory[PostgreSQLConnection] =
		new PostgreSQLConnectionFactory(config)

	/**
	 * Used to parse the URIs that this particular DBConfigurationBuilder is interested in.
	 * @param uri the source URI
	 */
	override protected[postgresql] def parseURI(uri: URI): Option[DBConfiguration] = {
		val simplePGDB = "^postgresql:(\\w+)$".r
		(uri.getScheme match {
			case "postgresql" ⇒
				val userInfo = parseUserInfo(Option(uri.getUserInfo))

				Some(DBConfiguration(
					username = userInfo._1.getOrElse("postgres"),
					password = userInfo._2,
					host = Option(uri.getHost).getOrElse("localhost"),
					port = Option(uri.getPort).getOrElse(5432),
					database = Option(uri.getPath).map(_.stripPrefix("/"))
				))
			case "jdbc" ⇒
				uri.getSchemeSpecificPart match {
					case simplePGDB(db) ⇒
						// Localhost, no password, user = ???
						Some(DBConfiguration(
							username = "postgres",
							database = Some(db)
						))
					case x ⇒
						// the schemeSpecificPart is just a normal postgresql:// connection string now
						parseURI(new URI(x))
				}
		}).map { startingConfiguration ⇒
			Option(uri.getQuery).map { qs ⇒
				val parameters = FormUrlEncodedParser.parseNotPreservingOrder(qs)
				var finalConfiguration = startingConfiguration

				// Correct for query parameter settings, if they exist
				for(nameSeq ← parameters.get("user"))
					finalConfiguration = finalConfiguration.copy(username = nameSeq.head)

				for(pwSeq ← parameters.get("password"))
					finalConfiguration = finalConfiguration.copy(password = pwSeq.headOption)

				finalConfiguration
			}.getOrElse(startingConfiguration)
		}
	}
}
