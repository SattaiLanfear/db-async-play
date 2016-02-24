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

import com.google.inject.ImplementedBy
import models.User
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.{ExecutionContext, Future}

/**
	* An interface to the User storage facility.
	* @param srandom the application Secure Random instance.
	*/
@ImplementedBy(classOf[MySQLUserRepository])
abstract class UserRepository(private val srandom: SecureRandom) {

	/**
		* Finds the identified user.
		* @param id the user's identifier.
		* @param ec the execution context to use while preparing the response.
		* @return Some containing the desired user if found, otherwise None.
		*/
	def findById(id: Long)(implicit ec: ExecutionContext): Future[Option[User]]

	/**
		* Finds the identified users, if possible, returning as many as were found.
		* @param ids the user identifiers.
		* @param ec the execution context to use while preparing the response.
		* @return A map of all found ids to their users.
		*/
	def findAllById(ids: Set[Long])(implicit ec: ExecutionContext): Future[Map[Long, User]]

	/**
		* Finds the identified user, if present.
		* @param name the name of the user to search for, it must match precicely.
		* @param ec the execution context to use while preparing the response.
		* @return A future Some(user) if found, otherwise Future None.
		*/
	def findByName(name: String)(implicit ec: ExecutionContext): Future[Option[User]]

	/**
		* Finds all of the currently registered users.
		* @param ec the execution context to use while preparing the response.
		* @return a sequence of users contained in the database.
		*/
	def listAll()(implicit ec: ExecutionContext): Future[Seq[User]]


	/**
		* Updates the stored balance for the specified user.
		* @param id the user to be updated.
		* @param change the change in their balance.
		* @param ec the execution context to use while preparing the response.
		* @return the new user information.
		*/
	def updateBalance(id: Long, change: Long)(implicit ec: ExecutionContext): Future[User]

	/**
		* Creates a new User with the given attributes.
		* @param name the User's name.
		* @param password the User's unhashed password.
		* @param ec the execution context to use while preparing the response.
		* @return the newly created User.
		*/
	final def create(name: String, password: String)(implicit ec: ExecutionContext): Future[User] =
		_create(name, encodePassword(password))

	/**
		* Attempts to fetch the identified user, but failing that attempts to create a new user in its place.  If an existing
		* user is found, the password is not compared.
		* @param name the name of the user to look up or create.
		* @param password the new password to use if the user is created.
		* @param ec the execution context to use while preparing the response.
		* @return a pair of boolean Created(true if the user did not previously exist), and the found or created user.
		*/
	def getOrCreate(name: String, password: String)(implicit ec: ExecutionContext): Future[(Boolean, User)] =
		_getOrCreate(name, password)

	/**
		* Updates the lastLogin field to "now" for the specified user.
		* @param id the User's id number.
		* @param ec the execution context to use while preparing the response.
		* @return the updated User object.
		*/
	def updateLastLogin(id: Long)(implicit ec: ExecutionContext): Future[User]

	/**
		* Deletes the specified user from the user repository.
		* @param id the User's id number.
		* @param ec the execution context to use while preparing the response.
		* @return true if a user was deleted, false otherwise.
		*/
	def delete(id: Long)(implicit ec: ExecutionContext): Future[Boolean]

	/**
		* Deletes all Users that have not logged in since before.
		* @param before the cutoff date.
		* @param ec the execution context to use while preparing the response.
		* @return the Id's deleted.
		*/
	def deleteByLastLogin(before: DateTime)(implicit ec: ExecutionContext): Future[Set[Long]]

	/**
		* The internal implementation of create.
		* @param name the new User's name.
		* @param encodedPassword the encoded password.
		* @param ec the execution context to use while preparing the response.
		* @return the newly created User.
		*/
	protected def _create(name: String, encodedPassword: String)(implicit ec: ExecutionContext): Future[User]

	/**
		* Performs the getOrCreate operation, retrying up to 3 times.
		* @param name the name of the user to look up or create.
		* @param password the new password to use if the user is created.
		* @param depth the number of times this function has had to reenter.
		* @param ec the execution context to use while preparing the response.
		* @return a pair of boolean Created(true if the user did not previously exist), and the found or created user.
		*/
	private def _getOrCreate(name: String, password: String, depth: Int = 0)(implicit ec: ExecutionContext): Future[(Boolean, User)] = {
		findByName(name).flatMap {
			case Some(user) ⇒ Future.successful((false, user))
			case _ ⇒ create(name, password).map((true, _))
		}.recoverWith {
			case e: UserAlreadyExists if depth < 3 ⇒
				// Okay we should be able to find it now, retry up to 3 times
				_getOrCreate(name, password, depth + 1)
		}
	}

	private def encodePassword(pw: String): String =
		BCrypt.hashpw(pw, BCrypt.gensalt(10, srandom))
}
