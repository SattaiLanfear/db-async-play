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

import play.api.{Configuration, Environment, inject}

/**
 * This module explicitly provides the PostgreSQLConnectionPool binding.  It shouldn't be needed but is provided for the
 * sake of completeness, or in the case that an eager instance is desired.
 */
class Module extends inject.Module {
	override def bindings(environment: Environment, configuration: Configuration) = Seq(
		bind[PostgreSQLConnectionPool].toSelf.eagerly()
	)
}
