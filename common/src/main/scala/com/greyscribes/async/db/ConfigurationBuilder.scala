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

package com.greyscribes.async.db

import java.net.{URI, URISyntaxException}
import java.nio.charset.Charset

import com.github.mauricio.async.db.pool.{ObjectFactory, PoolConfiguration}
import com.github.mauricio.async.db.{Configuration ⇒ DBConfiguration, Connection}
import play.api.{Configuration, Logger}

import scala.concurrent.duration._

/**
 * This class sorts out the default connection factory from the others and is primarily used to construct the type-specific
 * Pools.
 * @param default the default ObjectFactory
 * @param defaultName the name for the default, used when looking it up within itself.
 * @param others all other ObjectFactories for this connection type, associated with their name.
 * @tparam T the type of Connection the ConnectionPoolGroup will be expected to use.
 */
case class ConfigurationGroup[T <: Connection](default: ObjectFactory[T],
                                               defaultName: String,
                                               others: Map[String, ObjectFactory[T]])


/**
 * Common base code for pulling DB configuration out of the Play configuration.
 */
abstract class ConfigurationBuilder[T <: Connection] {

	/**
	 * Assembles a ConfigurationGroup for the identified driver, out of the provided configuration data.
	 * @param config the Play Configuration data.
	 * @return a ConfigurationGroup, or a PlayException if we are unable to select a default.
	 */
	def getConfigurationGroup(config: Configuration): ConfigurationGroup[T] = {
		// Get the configurations as factories
		val configurations = buildDBConfiguration(config)
			.mapValues { pair ⇒
				(buildFactory(pair._1), pair._2)
			}

		// Find the potential defaults
		val defaults = configurations.filter(_._2._2)

		if(defaults.size > 1)
			throw config.reportError("db", s"Too many defaults found for $getDriverName.")

		// Settle on a default to use
		val default = defaults.headOption.orElse {
			logger.warn(s"No default specified for $getDriverName, using first (possibly randomly determined) configuration.")
			configurations.headOption
		}.map(p ⇒
			(p._1, p._2._1)
		).getOrElse {
			throw config.reportError("db", s"No configurations found for $getDriverName")
		}

		ConfigurationGroup(
			default._2,
			default._1,
			// Convert the remainder into String->Factory
			(configurations - default._1).mapValues(p ⇒ p._1)
		)
	}

	/**
	 * @param config the database connection configuration.
	 * @return a factory appropriate for this type of Builder based on the provided configuration.
	 */
	protected def buildFactory(config: DBConfiguration): ObjectFactory[T]


	/**
	 * Constructs a set of Configuration objects based on teh provided Play configuration, specific to the driver
	 * specified in getDriverName.
	 * @param config the Play Configuration from which to draw the database settings.
	 * @return Configuration objects paired with whether or not they had default: true in their configuration, mapped with
	 *         their configured names.
	 */
	def buildDBConfiguration(config: Configuration): Map[String, (DBConfiguration, Boolean)] = {
		config.getConfig("db").map { db ⇒
			// Keys, rejecting those groups we know belong to other settings
			(db.subKeys -- skippedKeys).flatMap { name ⇒
				db.getConfig(name).flatMap { implicit entryConfig ⇒
					(entryConfig.getString("driver"), entryConfig.getString("url")) match {
						case (Some(driver), Some(uri)) if driver == getDriverName ⇒
							logger.trace("Found entry configuration db." + name)
							try {
								parseURI(new URI(uri)).map { dbc ⇒
									var updateableDBC = dbc

									// First since it can throw exceptions, get failure out of the way early
									for(charset ← entryConfig.getString("charSet")) {
										try {
											updateableDBC = updateableDBC.copy(charset = Charset.forName(charset))
										} catch {
											case e: Exception ⇒
												throw entryConfig.reportError("charset", "Error associating charset with database configuration", Some(e))
										}
									}

									// Update for extra parameters in the block, conf file wins over url
									for(userName ← entryConfig.getString("username"))
										updateableDBC = updateableDBC.copy(username = userName)

									for(pass ← entryConfig.getString("password"))
										updateableDBC = updateableDBC.copy(password = Some(pass).filterNot(_.isEmpty))

									ensureMinimumValue(maxMessageSize, entryConfig.getBytes(maxMessageSize)) { mms ⇒
										updateableDBC = updateableDBC.copy(maximumMessageSize = mms.toInt)
									}

									ensureMinimumValue(connectTimeout, entryConfig.getMilliseconds(connectTimeout)) { ct ⇒
										updateableDBC = updateableDBC.copy(connectTimeout = ct.milliseconds)
									}

									ensureMinimumValue(testTimeout, entryConfig.getMilliseconds(testTimeout)) { tt ⇒
										updateableDBC = updateableDBC.copy(testTimeout = tt.milliseconds)
									}

									ensureMinimumValue(queryTimeout, entryConfig.getMilliseconds(queryTimeout)) { qt ⇒
										updateableDBC = updateableDBC.copy(queryTimeout = Some(qt.milliseconds))
									}

									(updateableDBC, entryConfig.getBoolean("default").getOrElse(false))
								}
							} catch {
								case e: URISyntaxException ⇒
									throw entryConfig.reportError("url", "Unable to parse provided connection URI.", Some(e))
							}
						case (Some(_), Some(_)) ⇒
							logger.debug(s"Skipping db.$name; configuration intended for another driver.")
							None
						case (Some(driver), None) if driver == getDriverName ⇒
							logger.error(s"Driver configuration attempted for db.$name; but no url provided.")
							None
						case (None, Some(_)) ⇒
							logger.warn(s"Possible driver configuration attempt found at db.$name; but no driver specified.")
							None
						case _ ⇒
							logger.trace(s"Skipping db.$name; does not appear to be a driver configuration block.")
							None
					}
				}.map((name, _))
			}.toMap
		}.getOrElse {
			logger.warn("No database configuration information found...")
			Map.empty[String, (DBConfiguration, Boolean)]
		}
	}

	private val maxMessageSize = "maxMessageSize"
	private val connectTimeout = "connectTimeout"
	private val testTimeout = "testTimeout"
	private val queryTimeout = "queryTimeout"


	/**
	 * Builds a PoolConfiguration, using the library defaults to start with, and allowing settings in application.conf to override.
	 *
	 * @param config - the application configuration file
	 * @return a PoolConfiguration for the driver(s).
	 */
	def buildPoolConfiguration(config: Configuration): PoolConfiguration = {
		var finalPoolConf = PoolConfiguration.Default

		config.getConfig("db.asyncPool").foreach { implicit dbConfig ⇒


			ensureMinimumValue(poolMaxObjects, dbConfig.getInt(poolMaxObjects)) { mo ⇒
				finalPoolConf = finalPoolConf.copy(maxObjects = mo)
			}

			ensureMinimumValue(poolMaxIdleObjects, dbConfig.getLong(poolMaxIdleObjects)) { mio ⇒
				finalPoolConf = finalPoolConf.copy(maxIdle = mio)
			}

			ensureMinimumValue(poolMaxQueueSize, dbConfig.getInt(poolMaxQueueSize)) { mqs ⇒
				finalPoolConf = finalPoolConf.copy(maxQueueSize = mqs)
			}

			ensureMinimumValue(poolValidationInterval, dbConfig.getMilliseconds(poolValidationInterval)) { vi ⇒
				finalPoolConf = finalPoolConf.copy(validationInterval = vi)
			}
		}

		finalPoolConf
	}

	/**
	 * Ensures that the provided optional value is at least a minimum, inclusive.  By default, 1 or higher.
	 * @param path the source of the field.
	 * @param setting the value of the field.
	 * @param minimum the minimum value of the field
	 * @param onSuccess a block to evaluate if the field meets the minimum requested
	 * @param conf the configuration from which the value was drawn
	 * @tparam X a value class.
	 */
	protected def ensureMinimumValue[X <: AnyVal](path: String, setting: Option[X], minimum: Long = 1)(onSuccess: X ⇒ Unit)(implicit conf: Configuration): Unit = setting match {
		case Some(char: Char) if char >= minimum ⇒ onSuccess(setting.get)
		case Some(short: Short) if short >= minimum ⇒ onSuccess(setting.get)
		case Some(int: Int) if int >= minimum ⇒ onSuccess(setting.get)
		case Some(long: Long) if long >= minimum ⇒ onSuccess(setting.get)
		case Some(float: Float) if float >= minimum ⇒ onSuccess(setting.get)
		case Some(double: Double) if double >= minimum ⇒ onSuccess(setting.get)
		case Some(_: Char) | Some(_: Short) | Some(_: Int) | Some(_: Long) | Some(_: Float) | Some(_: Double) ⇒
			throw conf.reportError(path, "Provided value less than " + minimum)
		case None ⇒ logger.debug(s"No value found for $path; skipping.")
		case _ ⇒ throw conf.reportError(path, "Unrecognized type passed in.  This is likely a programming error in db-async-play.")
	}

	/**
	 * Parses out userInfo into a tuple of optional username and password
	 * @param userInfo the optional user info string
	 * @return a tuple of optional username and password
	 */
	protected[db] def parseUserInfo(userInfo: Option[String]): (Option[String], Option[String]) = userInfo.map(_.split(":", 2).toList) match {
		case Some(user :: pass :: Nil) ⇒ (Some(user), Some(pass))
		case Some(user :: Nil) ⇒ (Some(user), None)
		case _ ⇒ (None, None)
	}

	// These are used to filter out pool related fields from the configuration scan.
	private val poolMaxObjects = "maxObjects"
	private val poolMaxIdleObjects = "maxIdleObjects"
	private val poolMaxQueueSize = "maxQueueSize"
	private val poolValidationInterval = "validationInterval"

	private def skippedKeys = Set(
		"asyncPool",
		"hikaricp",
		"bonecp"
	)

	/**
	 * @return the name of the currently processing database driver, as it should be entered on the db.*.driver line
	 */
	protected def getDriverName: String

	/**
	 * Used to parse the URIs that this particular DBConfigurationBuilder is interested in.
	 * @param uri the source URI
	 */
	protected def parseURI(uri: URI): Option[DBConfiguration]

	/**
	 * @return the logger to use for this class
	 */
	protected def logger: Logger

}
