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

package models.repositories

import javax.inject.Inject

import com.greyscribes.async.db.postgresql.PostgreSQLConnectionPool
import models.Transaction

import scala.concurrent.{ExecutionContext, Future}

/**
	* Created by steve on 11/4/15.
	*/
class PostgreSQLTransactionRepository @Inject()(_pool: PostgreSQLConnectionPool) extends TransactionRepository {

	private val pool = _pool("transactions")

	/**
		* Creates a new transfer to the "to" account from the "from" account, in the amount of change.
		* @param to the to user id
		* @param from the from user id
		* @param change the amount of change
		* @param ec the execution context to use while preparing the response.
		* @return the newly created transaction.
		*/
	override def transfer(to: Long, from: Long, change: BigDecimal)(implicit ec: ExecutionContext): Future[Transaction] = ???

	/**
		* Finds all transactions with the specified user either in the To or From columns.
		* @param user the user to search for.
		* @param ec the execution context to use while preparing the response.
		* @return a sequence of transactions in the order they were completed in.
		*/
	override def listFor(user: Long)(implicit ec: ExecutionContext): Future[Seq[Transaction]] = ???

	/**
		* Removes this user from all transactions they were involved in (substituting null).
		* If any transaction has both no to and no from, it will be permanently deleted.
		* @param user the user id to search for.
		* @param ec the execution context to use while preparing the response.
		* @return the number of changed rows, and the number of deleted rows.
		*/
	override def deleteFor(user: Long)(implicit ec: ExecutionContext): Future[(Int, Int)] = ???
}
