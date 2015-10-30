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

package com.greyscribes.async.db.mysql

import java.net.URI
import javax.inject.{Inject, Singleton}

import com.github.mauricio.async.db.mysql.MySQLConnection
import com.github.mauricio.async.db.mysql.pool.MySQLConnectionFactory
import com.github.mauricio.async.db.{Configuration ⇒ DBConfiguration}
import com.greyscribes.async.db.{ConfigurationBuilder, LifecycleBoundConnectionPoolGroup}
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Logger}
import play.core.parsers.FormUrlEncodedParser

import scala.concurrent.duration._

/**
 * The primary purpose of this class is to create a strong type for the dependency injector.
 * @param config the Play Configuration from which to gather our configuration(s).
 * @param lifecycle the Play Lifecycle object, so that this class can self-register for shutdown.
 */
@Singleton
class MySQLConnectionPool @Inject()()(config: Configuration,
                                      lifecycle: ApplicationLifecycle)
	extends LifecycleBoundConnectionPoolGroup[MySQLConnection](config, MySQLConnectionPool, lifecycle) {

}

/**
 * This companion object acts as the MySQLConnection ConfigurationBuilder, providing Configuration parsing
 * services to MySQLConnectionPool.
 */
object MySQLConnectionPool extends ConfigurationBuilder[MySQLConnection] {

	/**
	 * @return the logger to use for this class
	 */
	override protected val logger: Logger = Logger(classOf[MySQLConnectionPool])
	/**
	 * @return the name of the currently processing database driver, as it should be entered on the db.*.driver line
	 */
	override protected val getDriverName: String = "mysql-async"

	override protected def buildFactory(config: DBConfiguration): MySQLConnectionFactory =
		new MySQLConnectionFactory(config)

	/**
	 * Used to parse the URIs that this particular DBConfigurationBuilder is interested in.
	 * @param uri the source URI
	 */
	override protected[mysql] def parseURI(uri: URI): Option[DBConfiguration] = {
		(uri.getScheme match {
			case "mysql" ⇒
				val userInfo = parseUserInfo(Option(uri.getUserInfo))
				var port = uri.getPort
				if(port < 0) {
					port = 3306
				}

				Some(DBConfiguration(
					username = userInfo._1.getOrElse("root"),
					password = userInfo._2,
					host = Option(uri.getHost).getOrElse("localhost"),
					port = port,
					database = Option(uri.getPath).map(_.stripPrefix("/")).filterNot(_.isEmpty)
				))
			case "jdbc" ⇒
				// the schemeSpecificPart is just a mysql:// connection string now
				parseURI(new URI(uri.getSchemeSpecificPart))
		}).map { startingConfiguration ⇒
			Option(uri.getQuery).map { qs ⇒
				val parameters = FormUrlEncodedParser.parseNotPreservingOrder(qs)
				var finalConfiguration = startingConfiguration

				// Correct for query parameter settings, if they exist
				for(nameSeq ← parameters.get("user"))
					finalConfiguration = finalConfiguration.copy(username = nameSeq.head)

				for(pwSeq ← parameters.get("password"))
					finalConfiguration = finalConfiguration.copy(password = pwSeq.headOption)

				// For compatability with more jdbc uri's

				for {
					cTList ← parameters.get("connectTimeout")
					cT ← cTList.map(_.toInt).find(_ > 0)
				} finalConfiguration = finalConfiguration.copy(connectTimeout = cT.milliseconds)

				finalConfiguration
			}.getOrElse(startingConfiguration)
		}
	}
}

