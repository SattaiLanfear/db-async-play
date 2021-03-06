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

import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.Around
import org.specs2.specification.Scope
import play.api.inject.DefaultApplicationLifecycle
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Configuration, Environment}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Support trait used to enable multiple tests to easily use a play injector.
 */
abstract class Injecting(val confFile: String, val config: (String, String)*) extends Around with Scope {

	lazy val injector = {
		val env = Environment.simple()
		val conf = Configuration.load(env, Map("config.resource" → confFile))

		new GuiceApplicationBuilder()
			.in(env)
			.configure(conf)
			.configure(config: _*)
			.injector()
	}

	override def around[T: AsResult](t: ⇒ T): Result = {
		try AsResult.effectively(t)
		finally {
			Await.ready(injector.instanceOf[DefaultApplicationLifecycle].stop(), 10.seconds)
		}
	}
}
