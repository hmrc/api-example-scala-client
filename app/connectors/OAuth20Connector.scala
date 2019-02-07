/*
 * Copyright 2019 HM Revenue & Customs
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

package connectors

import javax.inject.Inject
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

case class OauthResponse(access_token: String, refresh_token: String, expires_in: Long)

object OauthResponse {
  implicit val formats = Json.format[OauthResponse]
}

class OAuth20Connector @Inject()(config: OAuth20Config, httpClient: HttpClient)(implicit ec: ExecutionContext) {

  def getToken(authorisationCode: String)(implicit hc: HeaderCarrier): Future[OauthResponse] = oauth2(
    Map(
      "redirect_uri" -> Seq(config.callbackUrl),
      "grant_type" -> Seq("authorization_code"),
      "code" -> Seq(authorisationCode)
    )
  )

  def refreshToken(refreshToken: String)(implicit hc: HeaderCarrier): Future[OauthResponse] = oauth2(
    Map(
      "grant_type" -> Seq("refresh_token"),
      "refresh_token" -> Seq(refreshToken)
    )
  )

  private def oauth2(body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[OauthResponse] = {

    val bodyWithClientData = Map(
      "client_id" -> Seq(config.clientId),
      "client_secret" -> Seq(config.clientSecret)
    ) ++ body

    httpClient.POST[Map[String, Seq[String]], OauthResponse](config.tokenUrl, bodyWithClientData)
  }

}

case class OAuth20Config(clientId: String, clientSecret: String, tokenUrl: String, callbackUrl: String)
