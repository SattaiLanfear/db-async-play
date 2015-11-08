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

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.pool.{ConnectionPool, ObjectFactory, PoolConfiguration}
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}


class ConnectionPoolGroup[T <: Connection](default: ObjectFactory[T],
                                                    defaultName: String,
                                                    others: Map[String, ObjectFactory[T]],
                                                    poolConfig: PoolConfiguration)
	extends ConnectionPool[T](default, poolConfig) {

	protected val pools = others.mapValues(new ConnectionPool(_, poolConfig)) + (defaultName → this)

	def this(configurationGroup: ConfigurationGroup[T], poolConfig: PoolConfiguration) =
		this(configurationGroup.default, configurationGroup.defaultName, configurationGroup.others, poolConfig)

	def this(config: Configuration, configurationBuilder: ConfigurationBuilder[T]) =
		this(configurationGroup = configurationBuilder.getConfigurationGroup(config),
			poolConfig = configurationBuilder.buildPoolConfiguration(config))

	/**
	 * @param pool the name of the desired pool
	 * @return the desired pool or NoSuchElementException if there is no configured pool by that name.
	 */
	def apply(pool: String): ConnectionPool[T] = pools(pool)

	/**
	 * Closes all of the pools in this pool group, including the default pool.
	 * @return a Future of the result of the close operation (an exception, or nothing).
	 */
	def closeAll()(implicit ec: ExecutionContext): Future[Unit] = {
		Future.sequence(pools.values.map(_.close)).map(_ ⇒ Unit)
	}
}
