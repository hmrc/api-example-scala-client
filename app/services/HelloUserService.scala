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

package services

import connectors.{UnauthorizedException, OAuth20Connector, ApiConnector}
import play.api.libs.json.JsValue
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

case class OauthTokens(access_token: String, refresh_token: String)

trait HelloUserService {

  val apiConnector: ApiConnector
  val oauthConnector: OAuth20Connector

  def helloOauth(oAuthToken: String, refreshToken: String): Future[(JsValue, OauthTokens)] = {
    apiConnector.helloUser(oAuthToken).map((_, OauthTokens(oAuthToken,refreshToken))) recoverWith {
      case e: UnauthorizedException =>
        oauthConnector.refreshToken(refreshToken) flatMap { t =>
          apiConnector.helloUser(t.access_token) map ((_, OauthTokens(t.access_token, t.refresh_token)))
        }
    }
  }

  def helloOauth(authorizationCode: String): Future[(JsValue, OauthTokens)] = {
    oauthConnector.getToken(authorizationCode) flatMap { t =>
      apiConnector.helloUser(t.access_token) map ((_, OauthTokens(t.access_token, t.refresh_token)))
    }
  }
}

object HelloUserService extends HelloUserService {
  override val apiConnector: ApiConnector = ApiConnector
  override val oauthConnector = OAuth20Connector
}
