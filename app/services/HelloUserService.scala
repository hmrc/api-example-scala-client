/*
 * Copyright 2023 HM Revenue & Customs
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

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier

import connectors.{ApiConnector, OAuth20Connector, UnauthorizedException}

case class OauthTokens(access_token: String, refresh_token: String)

class HelloUserService @Inject() (apiConnector: ApiConnector, oauthConnector: OAuth20Connector)(implicit ec: ExecutionContext) {

  def helloOauth(oAuthToken: String, refreshToken: String)(implicit hc: HeaderCarrier): Future[(JsValue, OauthTokens)] = {
    apiConnector.helloUser(oAuthToken).map((_, OauthTokens(oAuthToken, refreshToken))) recoverWith {
      case _: UnauthorizedException =>
        oauthConnector.refreshToken(refreshToken) flatMap { t =>
          apiConnector.helloUser(t.access_token) map ((_, OauthTokens(t.access_token, t.refresh_token)))
        }
    }
  }

  def helloOauth(authorizationCode: String)(implicit hc: HeaderCarrier): Future[(JsValue, OauthTokens)] = {
    oauthConnector.getToken(authorizationCode) flatMap { t =>
      apiConnector.helloUser(t.access_token) map ((_, OauthTokens(t.access_token, t.refresh_token)))
    }
  }
}
