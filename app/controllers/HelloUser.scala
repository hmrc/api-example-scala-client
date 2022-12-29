/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.mvc._
import services.{HelloUserService, OauthTokens}

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

@Singleton
class HelloUser @Inject()(service: HelloUserService, config: HelloUserConfig, cc: MessagesControllerComponents)
                         (implicit ec: ExecutionContext)extends FrontendController(cc) {

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
        s"${config.authorizeUrl}",
        Map(
          "client_id" -> Seq(config.clientId),
          "scope" -> Seq("hello"),
          "response_type" -> Seq("code"),
          "redirect_uri" -> Seq(config.callbackUrl)
        )
      )
    )

    def callOAuth = service.helloOauth(request.session.get("token").get, request.session.get("refresh_token").getOrElse(""))

    (request.session.get("token"), request.session.get("refresh_token")) match {
      case (None, None) => redirectToOauthFrontEnd
      case (Some(_), Some(_)) => callOAuth map { case (json, token) => Ok(json).addToken(token) } recover {
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

case class HelloUserConfig(clientId: String, callbackUrl: String, authorizeUrl: String)
