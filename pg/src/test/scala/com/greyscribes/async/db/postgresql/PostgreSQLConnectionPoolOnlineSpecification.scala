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

import com.github.mauricio.async.db.QueryResult
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.specification.mutable.ExecutionEnvironment

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * This specification includes tests that require an actual PostgreSQL connection be available.
 */
class PostgreSQLConnectionPoolOnlineSpecification extends Specification with Mockito with ExecutionEnvironment {
	def is(implicit ee: ExecutionEnv) = {

		"The PostgreSQLConnectionPool with a real database" should {

			"successfully configure and communicate with the local database" in new Injecting("local.conf") {
				val pool = injector.instanceOf[PostgreSQLConnectionPool]

				pool("alpha") must be(pool)
				val beta = pool("beta")

				val numbers = Seq(1, 2, 3, 5, 7, 9, 11, 13, 17)
				val colors = Seq("blue", "red", "green", "orange", "octarine", "amber", "gold", "white", "black", "puce")

				// Highly inefficient but... gets the point across
				pool.inTransaction { c ⇒
					numbers.foldLeft(Future.successful[QueryResult](null)) { (prev, num) ⇒
						prev.flatMap(_ ⇒ c.sendPreparedStatement("INSERT INTO magic_numbers VALUES (?)", Array(num)))
					}
				}.map(result ⇒ result.rowsAffected mustEqual (1)).awaitFor(numbers.size.seconds)


				// Deliberately intermix beta initialization
				beta.inTransaction { c ⇒
					colors.foldLeft(Future.successful[QueryResult](null)) { (prev, color) ⇒
						prev.flatMap(_ ⇒ c.sendPreparedStatement("INSERT INTO magic_colors VALUES (?)", Array(color)))
					}
				}.map(result ⇒ result.rowsAffected mustEqual (1)).awaitFor(colors.size.seconds)


				// Verify Inserts
				pool.sendQuery("SELECT * FROM magic_numbers ORDER BY num ASC")
					.map { results ⇒
						results.rows.map { rows ⇒
							for(row ← rows)
								yield row("num").asInstanceOf[Int]
						}.getOrElse(IndexedSeq.empty)
					} must containTheSameElementsAs(numbers).awaitFor(2.seconds)

				beta.sendQuery("SELECT * FROM magic_colors")
					.map { results ⇒
						results.rows.map { rows ⇒
							for(row ← rows)
								yield row("color").toString
						}.getOrElse(IndexedSeq.empty)
					} must containTheSameElementsAs(colors).awaitFor(2.seconds)

			}
		}
	}

}
