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

import java.security.SecureRandom
import javax.inject.Inject

import com.github.mauricio.async.db.{Connection, ResultSet, RowData}
import com.greyscribes.async.db.mysql.MySQLConnectionPool
import models.User
import org.joda.time.{LocalDateTime, DateTime}

import scala.concurrent.{ExecutionContext, Future}

/**
	* The MySQL backed implementation of the User repository.  This implementation serves as the default User repository
	* and uses the MySQL pool "users".
	* @param _pool the application MySQL Connection Pool.
	* @param _srandom the Application Secure Random instance.
	*/
class MySQLUserRepository @Inject()(_pool: MySQLConnectionPool, _srandom: SecureRandom) extends UserRepository(_srandom) {

	import MySQLUserRepository._

	private val pool = _pool("users")

	/**
		* Finds the identified user.
		* @param id the user's identifier.
		* @param ec the execution context to use while preparing the response.
		* @return Some containing the desired user if found, otherwise None.
		*/
	override def findById(id: Long)(implicit ec: ExecutionContext): Future[Option[User]] =
		_findById(id)

	/**
		* Implements findById and allows the connection to be specified
		* @param id the user's identifier.
		* @param connection the connection to use.
		* @param ec the execution context to use while preparing the response.
		* @return Some containing the desired user if found, otherwise None.
		*/
	private def _findById(id: Long, connection: Connection = pool)(implicit ec: ExecutionContext): Future[Option[User]] =
		connection.sendPreparedStatement("SELECT * FROM users WHERE id = ?", Array(id))
			.map { queryResult ⇒
				queryResult.rows.flatMap(rows ⇒ rows.headOption.map(mapToUser))
			}


	/**
		* Finds the identified users, if possible, returning as many as were found.
		* @param ids the user identifiers.
		* @param ec the execution context to use while preparing the response.
		* @return A map of all found ids to their users.
		*/
	override def findAllById(ids: Set[Long])(implicit ec: ExecutionContext): Future[Map[Long, User]] =
		pool.sendQuery(s"SELECT * FROM users WHERE id IN (${ids.mkString(", ")})")
			.map { queryResult ⇒
				queryResult.rows.map { rows ⇒
					rows.map(mapToUser)
				}.getOrElse(Seq.empty[User]).map { user ⇒
					user.id → user
				}.toMap
			}

	/**
		* Finds the identified user, if present.
		* @param name the name of the user to search for, it must match precicely.
		* @param ec the execution context to use while preparing the response.
		* @return A future Some(user) if found, otherwise Future None.
		*/
	override def findByName(name: String)(implicit ec: ExecutionContext): Future[Option[User]] =
		pool.sendPreparedStatement("SELECT * FROM users WHERE name = ?", Array(name))
			.map { queryResult ⇒
				queryResult.rows.flatMap(rows ⇒ rows.headOption.map(mapToUser))
			}

	/**
		* Finds all of the currently registered users.
		* @param ec the execution context to use while preparing the response.
		* @return a sequence of users contained in the database.
		*/
	override def listAll()(implicit ec: ExecutionContext): Future[Seq[User]] =
		pool.sendQuery("SELECT * FROM users ORDER BY name")
			.map { queryResult ⇒
				queryResult.rows.map { rows ⇒
					rows.map(mapToUser)
				}.getOrElse(Seq.empty[User])
			}


	/**
		* Updates the stored balance for the specified user.
		* @param id the user to be updated.
		* @param change the change in their balance.
		* @param ec the execution context to use while preparing the response.
		* @return the new user information.
		*/
	override def updateBalance(id: Long, change: Long)(implicit ec: ExecutionContext): Future[User] =
		pool.inTransaction { c ⇒
			c.sendPreparedStatement("UPDATE users SET balance = balance + ? WHERE id = ?", Seq(change, id))
				.flatMap { resultSet ⇒
					if(resultSet.rowsAffected == 0)
						throw new UserNotFound
					_findById(id, c)
				}
		}.map {
			case Some(user) => user
			case _ ⇒ throw new UserNotFound
		}

	/**
		* Updates the lastLogin field to "now" for the specified user.
		* @param id the User's id number.
		* @param ec the execution context to use while preparing the response.
		* @return the updated User object.
		*/
	override def updateLastLogin(id: Long)(implicit ec: ExecutionContext): Future[User] =
		pool.inTransaction { c ⇒
			c.sendPreparedStatement("UPDATE users SET lastlogin = now() WHERE id = ?", Array(id))
				.flatMap { resultSet ⇒
					if(resultSet.rowsAffected == 0)
						throw new UserNotFound
					_findById(id, c)
				}
		}.map {
			case Some(user) => user
			case _ ⇒ throw new UserNotFound
		}

	/**
		* Deletes the specified user from the user repository.
		* @param id the User's id number.
		* @param ec the execution context to use while preparing the response.
		* @return true if a user was deleted, false otherwise.
		*/
	override def delete(id: Long)(implicit ec: ExecutionContext): Future[Boolean] =
		pool.sendPreparedStatement("DELETE FROM users WHERE id = ?", Array(id))
			.map { resultSet ⇒
				resultSet.rowsAffected > 0
			}

	/**
		* Deletes all Users that have not logged in since before.
		* @param before the cutoff date.
		* @param ec the execution context to use while preparing the response.
		* @return the Id's deleted.
		*/
	override def deleteByLastLogin(before: DateTime)(implicit ec: ExecutionContext): Future[Set[Long]] =
		pool.sendPreparedStatement("SELECT id FROM users WHERE lastlogin < ?").flatMap { rs ⇒
			rs.rows.map { rows ⇒
				rows.map { row ⇒
					row("id").asInstanceOf[Long]
				}
			}.getOrElse(Seq.empty[Long])
				// We can't afford to run too many at once, our pool is small
				.grouped(3)
				.foldLeft(Future.successful(Set.empty[Long])) { (past, current) ⇒
					past.flatMap { oldSeq ⇒
						Future.sequence(current.map { id ⇒
							pool.sendPreparedStatement("DELETE FROM users WHERE id = ? AND lastlogin < ?", Array(id, before))
								.map(qr ⇒ if(qr.rowsAffected > 0) Some(id) else None)
						}).map { deletedIds ⇒
							oldSeq ++ deletedIds.flatten
						}
					}
				}
		}

	/**
		* The internal implementation of create.
		* @param name the new User's name.
		* @param encodedPassword the encoded password.
		* @param ec the execution context to use while preparing the response.
		* @return the newly created User.
		*/
	override protected def _create(name: String, encodedPassword: String)(implicit ec: ExecutionContext): Future[User] = {
		// We need to maintain the same connection to ensure last_insert_id is valid
		pool.use { c ⇒
			for {
				insertResult ← c.sendPreparedStatement("INSERT INTO users (name, password) VALUES (?, ?)", Array(name, encodedPassword))
				fetchUserResult ← c.sendQuery("SELECT * FROM users WHERE id = last_insert_id()")
			} yield getOneUser(fetchUserResult.rows)
		} // TODO - add logic for UserAlreadyExists; mysql error code 1062
	}


}

object MySQLUserRepository {

	protected def getOneUser(rs: Option[ResultSet]): User =
		rs.flatMap(rows ⇒ rows.headOption.map(mapToUser))
			.getOrElse(throw new UserNotFound)


	protected def mapToUser(row: RowData): User =
		User(
			id = row("id").asInstanceOf[Long],
			name = row("name").toString,
			password = row("password").toString,
			lastLogin = row("lastlogin").asInstanceOf[LocalDateTime].toDateTime,
			balance = row("balance").asInstanceOf[Long]
		)

}
