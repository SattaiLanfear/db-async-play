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
				Left(Redirect(routes.Application.login()).withNewSession.flashing("info" → "You must be logged in!"))

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

	/**
		* Displays the Login form, and handles its completion.
		*/
	def login = Action.async { implicit req ⇒
		???
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

	def createTransaction = TODO

	def viewUsers = TODO

	def viewUser(id: Long) = AuthenticatedAction.async(parse.empty) { implicit req ⇒
		_viewUser(id)
	}

	def _viewUser(id: Long)(implicit req: AuthenticatedRequest[Unit]): Future[Result] = {
		???
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

}