/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import config.ApplicationContext
import play.api.mvc._
import services.{HelloUserService, OauthTokens}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait HelloUser extends Controller {
  val service: HelloUserService
  val clientId: String
  val callbackUrl: String
  val authorizeUrl: String

  implicit class RequestBuilder(result: Result)(implicit request: play.api.mvc.RequestHeader) {
    def addToken(oauthTokens: OauthTokens) =
      result.addingToSession(
        "token" -> oauthTokens.access_token,
        "refresh_token" -> oauthTokens.refresh_token
      )
  }

  def hello = Action.async { implicit request =>

    def redirectToOauthFrontEnd = Future.successful(
      Redirect(
        s"$authorizeUrl",
        Map(
          "client_id" -> Seq(clientId),
          "scope" -> Seq("hello"),
          "response_type" -> Seq("code"),
          "redirect_uri" -> Seq(callbackUrl)
        )
      )
    )

    def callOAuth = service.helloOauth(request.session.get("token").get, request.session.get("refresh_token").getOrElse(""))

    (request.session.get("token"), request.session.get("refresh_token")) match {
      case (None, None) => redirectToOauthFrontEnd
      case (Some(t), Some(rt)) => callOAuth map { case (json, token) => Ok(json).addToken(token) } recover {
        case ex => InternalServerError(ex.getMessage)
      }
      case _ => Future.successful(InternalServerError("Token mismatch, both token and refresh token should be in the session"))
    }
  }

  def helloWithCallback(code: Option[String], error: Option[String]) = Action.async { implicit request =>

    (code, error) match {
      case (Some(c), None) => service.helloOauth(c) map { case (json, token) => Ok(json).addToken(token) } recover {
        case ex => InternalServerError(ex.getMessage)
      }
      case (_, Some(e)) => Future.successful(InternalServerError(s"Error passed by caller: '$e'"))
      case (None, None) => Future.successful(InternalServerError("Did not receive the Authorization Code"))
    }
  }

}

object HelloUser extends HelloUser {
  override val clientId: String = ApplicationContext.clientId
  override val callbackUrl: String = ApplicationContext.callbackUrl
  override val authorizeUrl: String = ApplicationContext.authorizeUrl
  override val service: HelloUserService = HelloUserService
}
