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

package models

import org.joda.time.DateTime

/**
	* A transaction made by a user.
	*/
case class Transaction(id: Long,
                       to: Option[Long],
                       from: Option[Long],
                       change: Long,
                       timestamp: DateTime)

object Transaction {

	/**
		* Finds the current value of the provided group of transactions, from the perspective of the specified user.
		* @param id the user id to use as the perspective source.
		* @param transactions the transactions to examine.
		* @return the total value.
		*/
	def sumAs(id: Long, transactions: Transaction*): Long =
		transactions.foldLeft(0l) { (total, current) ⇒
			current match {
				case Transaction(_, Some(`id`), _, change, _) ⇒ total + change
				case Transaction(_, _, Some(`id`), change, _) ⇒ total - change
				case _ ⇒ total
			}
		}

	def totalGains(id: Long, transactions: Transaction*): Long =
		transactions.foldLeft(0l) { (total, current) ⇒
			current match {
				case Transaction(_, Some(`id`), _, change, _) ⇒ total + change
				case _ ⇒ total
			}
		}

	def totalLosses(id: Long, transactions: Transaction*): Long =
		transactions.foldLeft(0l) { (total, current) ⇒
			current match {
				case Transaction(_, _, Some(`id`), change, _) ⇒ total + change
				case _ ⇒ total
			}
		}

	def gainsAndLosses(id: Long, transactions: Transaction*): (Long, Long) =
		transactions.foldLeft((0l, 0l)) { (totals, current) ⇒
			current match {
				case Transaction(_, Some(`id`), _, change, _) ⇒ totals.copy(_1 = totals._1 + change)
				case Transaction(_, _, Some(`id`), change, _) ⇒ totals.copy(_2 = totals._2 + change)
				case _ ⇒ totals
			}
		}
}
