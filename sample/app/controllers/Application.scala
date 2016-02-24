package controllers

import javax.inject.Inject

import models.User
import models.repositories.{TransactionRepository, UserRepository}
import play.api._
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data._
import play.api.data.Forms._

import scala.concurrent.Future

/**
	* Singular interface to our
	*/
class Application @Inject()(private val userRepository: UserRepository,
                            private val transactionRepository: TransactionRepository,
                            val messagesApi: MessagesApi)
	extends Controller
	with I18nSupport {

	import Application._

	object AuthenticatedAction extends ActionBuilder[AuthenticatedRequest] with ActionRefiner[Request, AuthenticatedRequest] {
		override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {
			// Pre-build the failure state so we can reuse it
			val redirect: Either[Result, AuthenticatedRequest[A]] =
				Left(Redirect(routes.Application.displayLogin).withNewSession.flashing("info" → "You must be logged in!"))

			request.session.get(keyName).map(_.toLong).fold {
				Future.successful(redirect)
			} { id ⇒
				userRepository.findById(id).collect {
					case Some(user) ⇒ Right(new AuthenticatedRequest(user, request))
					case _ ⇒ redirect
				}
			}
		}
	}

	def displayLogin = Action { implicit req ⇒
		Ok(views.html.login(loginForm))
	}

	/**
		* Displays the Login form, and handles its completion.
		*/
	def login = Action.async(parse.urlFormEncoded) { implicit req ⇒
		loginForm.bindFromRequest.fold({ formWithErrors ⇒
			Future.successful(BadRequest(views.html.login(formWithErrors)))
		}, { loginData ⇒
			userRepository.getOrCreate(name = loginData.name, password = loginData.password)
				.flatMap {
					case (true, user) ⇒
						// New User, create their starting balance
						transactionRepository.transfer(Some(user), None, 500)
							.flatMap(trans ⇒ userRepository.updateBalance(user, trans.change))
							.map { _ ⇒
								Redirect(routes.Application.summary).withNewSession.withSession(keyName → user.id.toString)
									.flashing("success" → "Your New User Has Been Created!")
							}
					case (false, user) if user.checkPassword(loginData.password) ⇒
						// Old user, correct credentials
						userRepository.updateLastLogin(user)
							.map { updatedUser ⇒
								Redirect(routes.Application.summary).withNewSession.withSession(keyName → user.id.toString)
									.flashing("success" → "Welcome back!") // TODO - add "you were last here" as a "info"
							}
					case _ ⇒
						// Bad password
						Future.successful(BadRequest(views.html.login(loginForm.withGlobalError("Bad Password or Username Already Taken"))))
				}
		})
	}

	def logout = Action(parse.empty) { implicit req ⇒
		Redirect(routes.Application.displayLogin).withNewSession.flashing("info" → "You have been logged out.")
	}

	/**
		* Provides the user a summary of their current status, the default landing page for logged in users.
		*/
	def summary = AuthenticatedAction(parse.empty) { implicit req ⇒
		Ok(views.html.summary(req.user))
	}

	/**
		* Displays the current users transactions.
		*/
	def viewTransactions = AuthenticatedAction.async(parse.empty) { implicit req ⇒
		_viewUser(req.user.id) // Implemented in terms of view this user
	}

	def displayCreateTransaction = AuthenticatedAction.async(parse.empty) { implicit req ⇒
		_displayCreateTransaction().map { html ⇒
			Ok(html)
		}
	}

	def _displayCreateTransaction(form: Form[TransferForm] = transferForm.fill(TransferForm(1, -1, 1)))(implicit req: AuthenticatedRequest[_]) = {
		userRepository.listAll()
			.map { users ⇒
				val options = Seq("-1" → "The Magic Bank") ++ users.filterNot(_.id == req.user.id).map { user ⇒
					(user.id.toString, user.name)
				}

				views.html.transfer(form, req.user, options)
			}
	}

	def createTransaction = AuthenticatedAction.async(parse.urlFormEncoded) { implicit req ⇒
		transferForm.bindFromRequest().fold({ formWithErrors ⇒
			_displayCreateTransaction(formWithErrors).map { html ⇒
				Ok(html)
			}
		}, { form ⇒
			val target = if(form.target == -1) None else Some(form.target)
			val (to, from) = if(form.direction == 1) (Some(req.user.id), target) else (target, Some(req.user.id))

			transactionRepository.transfer(to, from, form.change).flatMap { transaction ⇒
				// Update both users, or just the current one if the target doesn't exist (is The Magic Bank)
				Future.sequence(Seq(
					Some(userRepository.updateBalance(req.user, form.change * form.direction)),
					target.map(userRepository.updateBalance(_, form.change * -form.direction))
				).flatten)
			}.flatMap { updatedUsers ⇒
				// Give it the updated user for the correct balances
				_displayCreateTransaction()(new AuthenticatedRequest(updatedUsers.head, req))
			}.map { html ⇒
				Ok(html).flashing("success" → "Transaction complete.")
			}
		})
	}

	def viewUsers = TODO

	def viewUser(id: Long) = AuthenticatedAction.async(parse.empty) { implicit req ⇒
		_viewUser(id)
	}

	def _viewUser(id: Long)(implicit req: AuthenticatedRequest[Unit]): Future[Result] = {
		(if(id == req.user.id) {
			Future.successful(Some(req.user))
		} else {
			userRepository.findById(id)
		}).flatMap {
			case Some(user) ⇒
				transactionRepository.listFor(user).flatMap { transactions ⇒
					// Now go find the id's for everyone else...
					val ids = transactions.foldLeft(Set.empty[Long]) { (state, current) ⇒
						state ++: Set(current.to, current.from).flatten
					} -- Set(id, req.user.id)

					userRepository.findAllById(ids).map { relatedUsers ⇒
							val display


						Ok(views.html.transactions(req.user, user, transactions))
					}
				}
			case None ⇒
				Future.successful(Redirect(routes.Application.summary).flashing("error" → "Unable to locate the requested user's information."))
		}
	}

}

object Application {
	type AuthenticatedRequest[A] = Security.AuthenticatedRequest[A, User]
	val keyName = "userId"


	case class LoginForm(name: String,
	                     password: String)

	val loginForm = Form(mapping(
		"name" → nonEmptyText,
		"password" → nonEmptyText
	)(LoginForm.apply)(LoginForm.unapply))

	case class TransferForm(direction: Int,
	                        target: Long,
	                        change: Long)

	val transferForm = Form(mapping(
		"direction" → default(number(min = -1, max = 1, strict = true), 1),
		"target" → default(longNumber(min = -1), -1l),
		"change" → default(longNumber(min = 1), 1l)
	)(TransferForm.apply)(TransferForm.unapply))

}