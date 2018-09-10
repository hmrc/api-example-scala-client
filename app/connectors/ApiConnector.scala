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

package connectors

import config.ApplicationContext
import play.api.libs.json.JsValue
import play.api.Play.current
import play.api.http.HeaderNames
import play.api.libs.ws.WS

import scala.concurrent.Future

trait ApiConnector {
  val serviceUrl: String
  val appToken: String
  val versionHeader = "application/vnd.hmrc.1.0+json"

  def helloWorld(): Future[JsValue] = api("/hello/world")

  def helloUser(oauthToken: String): Future[JsValue] = api("/hello/user", Some(oauthToken))

  def helloApplication(): Future[JsValue] = api("/hello/application", Some(appToken))

  private def api(endpoint: String, token: Option[String] = None): Future[JsValue] = {
    val authorizationHeader: Seq[(String, String)] = token match {
      case Some(t) => Seq(HeaderNames.AUTHORIZATION -> s"Bearer $t")
      case None => Seq()
    }
    val headers = authorizationHeader ++ Seq(
      HeaderNames.ACCEPT -> versionHeader
    )

    val request = WS.url(s"$serviceUrl$endpoint").withHeaders(headers:_*)

    extractJson[JsValue](request.get())
  }

}

object ApiConnector extends ApiConnector {
  override lazy val serviceUrl = ApplicationContext.apiGateway
  override lazy val appToken: String = ApplicationContext.serverToken
}
