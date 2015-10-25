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

import java.net.URI

import com.github.mauricio.async.db.pool.PoolConfiguration
import com.github.mauricio.async.db.{Configuration ⇒ DBConfiguration}
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.specification.core.Fragments
import play.api.{Configuration, Logger, PlayException}


class ConfigurationBuilderSpecification extends Specification with Mockito {

	/**
	 * Dummy class used to make the abstract methods visible.
	 */
	private class DummyConfigurationBuilder extends ConfigurationBuilder {
		/**
		 * @return the name of the currently processing database driver, as it should be entered on the db.*.driver line
		 */
		override protected def getDriverName: String = "dummy"

		/**
		 * Used to parse the URIs that this particular DBConfigurationBuilder is interested in.
		 * @param uri the source URI
		 */
		override protected def parseURI(uri: URI): Option[DBConfiguration] = None

		/**
		 * @return the logger to use for this class
		 */
		override protected[db] def logger: Logger = mock[Logger]
	}


	"The ConfigurationBuilder poolConfiguration builder" should {

		"correctly recognize configuration values" in {
			val fakeConfigurationBuilder = new DummyConfigurationBuilder


			val config = Configuration(
				"db.asyncPool.maxObjects" → 401,
				"db.asyncPool.maxIdleObjects" → 65,
				"db.asyncPool.maxQueueSize" → 9876,
				"db.asyncPool.validationInterval" → 43
			)

			val poolConfig = fakeConfigurationBuilder.buildPoolConfiguration(config)

			poolConfig.maxObjects mustEqual 401
			poolConfig.maxIdle mustEqual 65
			poolConfig.maxQueueSize mustEqual 9876
			poolConfig.validationInterval mustEqual 43
		}

		"throw an exception for illegal settings" in {
			val fakeConfigurationBuilder = new DummyConfigurationBuilder

			Fragments.foreach(Seq("db.asyncPool.maxObjects" → -401,
				"db.asyncPool.maxIdleObjects" → -65,
				"db.asyncPool.maxQueueSize" → -9876,
				"db.asyncPool.validationInterval" → -43)) { setting ⇒
				val config = Configuration(setting)
				val name = s"illegal ${setting._1} should throw an exception"

				name ! {
					fakeConfigurationBuilder.buildPoolConfiguration(config) must throwA[PlayException]
				}
			}
		}

		"return defaults when no settings are found" in {
			val fakeConfigurationBuilder = new DummyConfigurationBuilder

			val config = Configuration()

			val poolConfig = fakeConfigurationBuilder.buildPoolConfiguration(config)

			poolConfig mustEqual PoolConfiguration.Default
		}

		"accept only some settings being provided" in {
			val fakeConfigurationBuilder = new DummyConfigurationBuilder
			val config = Configuration(
				"db.asyncPool.maxObjects" → 41
			)

			val poolConfig = fakeConfigurationBuilder.buildPoolConfiguration(config)

			poolConfig mustEqual PoolConfiguration.Default.copy(maxObjects = 41)
		}

	}

	"The ConfigurationBuilder urlParser" should {

		"return none gracefully if there is no username or password information" in {
			val fakeConfigurationBuilder = new DummyConfigurationBuilder

			fakeConfigurationBuilder.parseUserInfo(None) mustEqual ((None, None))
		}


		"recognize a user alone" in {
			val fakeConfigurationBuilder = new DummyConfigurationBuilder

			fakeConfigurationBuilder.parseUserInfo(Some("user")) mustEqual ((Some("user"), None))
		}


		"recognize a user and password" in {
			val fakeConfigurationBuilder = new DummyConfigurationBuilder

			fakeConfigurationBuilder.parseUserInfo(Some("user:pass")) mustEqual ((Some("user"), Some("pass")))
		}

		"not lose parts" in {
			val fakeConfigurationBuilder = new DummyConfigurationBuilder

			fakeConfigurationBuilder.parseUserInfo(Some("user:pass:word")) mustEqual ((Some("user"), Some("pass:word")))
		}
	}


}
