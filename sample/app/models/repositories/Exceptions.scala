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

/**
	* Thrown where a user's presence is assumed but could not be located.
	*/
class UserNotFound extends RuntimeException("The specified user could not be located.")

class UserAlreadyExists(name: String) extends RuntimeException(s"The specified user id or name ($name) is already present.") {
	def this(id: Long) =
		this(id.toString)
}
