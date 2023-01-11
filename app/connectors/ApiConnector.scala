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

package connectors

import javax.inject.Inject
import play.api.http.HeaderNames
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

class ApiConnector @Inject() (config: ApiConfig, httpClient: HttpClient)(implicit ex: ExecutionContext) {
  val versionHeader = "application/vnd.hmrc.1.0+json"

  def helloWorld()(implicit hc: HeaderCarrier): Future[JsValue] = {
    api("/hello/world")(buildHeaderCarrier())
  }

  def helloUser(oauthToken: String)(implicit hc: HeaderCarrier): Future[JsValue] = {
    api("/hello/user")(buildHeaderCarrier(Some(oauthToken)))
  }

  def helloApplication()(implicit hc: HeaderCarrier): Future[JsValue] = {
    api("/hello/application")(buildHeaderCarrier(Some(config.serverToken)))
  }

  private def buildHeaderCarrier(token: Option[String] = None)(implicit hc: HeaderCarrier): HeaderCarrier = {
    val authorizationHeader: Seq[(String, String)] = token match {
      case Some(t) => Seq(HeaderNames.AUTHORIZATION -> s"Bearer $t")
      case None    => Seq()
    }
    val headers                                    = authorizationHeader ++ Seq(
      HeaderNames.ACCEPT -> versionHeader
    )
    hc.withExtraHeaders(headers: _*)
  }

  private def api(endpoint: String)(implicit hc: HeaderCarrier): Future[JsValue] = {
    httpClient.GET[JsValue](s"${config.apiGateway}$endpoint")
  }
}

case class ApiConfig(apiGateway: String, serverToken: String)
