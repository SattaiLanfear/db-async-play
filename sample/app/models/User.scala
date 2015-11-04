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
import org.mindrot.jbcrypt.BCrypt

/**
	* Represents a User in our system.
	* @param id the User's database ID.
	* @param name the User's name.
	* @param password the user's BCrypt'd password
	* @param lastLogin the last time the user logged in.
	*/
case class User(id: Long,
                name: String,
                password: String,
                lastLogin: DateTime) {

	/**
		* Checks the user's password.
		* @param pass the password to check
		* @return true if the password matches.
		*/
	def checkPassword(pass: String): Boolean =
		BCrypt.checkpw(pass, password)

}


object User {

	import scala.language.implicitConversions

	/**
		* Allows users to be passed anywhere a user id would be needed
		* @param u a User object
		* @return the user's Id#
		*/
	implicit def getId(u: User): Long = u.id

}
