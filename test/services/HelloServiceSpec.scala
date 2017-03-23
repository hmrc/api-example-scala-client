/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.{UnauthorizedException, OauthResponse, ApiConnector, OAuth20Connector}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

import scala.concurrent.Future


class HelloServiceSpec extends WordSpec with Matchers with ScalaFutures with MockitoSugar {


  trait Setup {
    val service = new HelloUserService {
      override val apiConnector = mock[ApiConnector]
      override val oauthConnector: OAuth20Connector = mock[OAuth20Connector]
    }
  }

  "Hello with authorization code" should {
    "return a valid JsValue and a token when access code is valid" in new Setup {
      val authorizationCode = "1111111111"
      val oauthResponse = OauthResponse("11111111111","22222222222",10000)
      val tokens = OauthTokens("11111111111","22222222222")

      when(service.oauthConnector.getToken(authorizationCode)).thenReturn(Future.successful(oauthResponse))
      when(service.apiConnector.helloUser(oauthResponse.access_token)).thenReturn(Future.successful(Json.parse( """{"message":"hello User"}""")))
      val (jv,t) = service.helloOauth(authorizationCode).futureValue

      jv shouldBe Json.parse( """{"message":"hello User"}""")
      t shouldBe tokens
    }
  }

  "Hello with access token and refresh token" should {
    "return a valid JsValue and no token when the oauth token is valid" in new Setup {
      val accessToken = "023456789"
      val refreshToken = "111111111"
      val oldToken = OauthTokens(accessToken,refreshToken)
      when(service.apiConnector.helloUser(accessToken)).thenReturn(Future.successful(Json.parse( """{"message":"hello User"}""")))
      val (jv, t) = service.helloOauth(accessToken, refreshToken).futureValue
      jv shouldBe Json.parse( """{"message":"hello User"}""")
      t shouldBe oldToken
      verify(service.apiConnector).helloUser(accessToken)
    }

    "return a valid JsValue and new token when the token was expired and was refreshed" in new Setup{
      val accessToken = "023456789"
      val refreshToken = "9876543442"
      val oauthResponse = OauthResponse("11111111111","22222222222",10000)
      val newTokens = OauthTokens("11111111111","22222222222")

      when(service.apiConnector.helloUser(accessToken)).thenReturn(Future.failed(new UnauthorizedException("unauthorized")))
      when(service.oauthConnector.refreshToken(refreshToken)).thenReturn(Future.successful(oauthResponse))
      when(service.apiConnector.helloUser(oauthResponse.access_token)).thenReturn(Future.successful(Json.parse( """{"message":"hello User"}""")))
      val (jv, t) = service.helloOauth(accessToken, refreshToken).futureValue
      jv shouldBe Json.parse( """{"message":"hello User"}""")
      t shouldBe newTokens
      verify(service.apiConnector).helloUser(accessToken) // failed call, token expired
      verify(service.oauthConnector).refreshToken(refreshToken) // refresh the token call
      verify(service.apiConnector).helloUser(oauthResponse.access_token) // success call, token is refreshed
    }

    "return RuntimeException when the token was expired and refreshing failed" in new Setup{
      val accessToken = "023456789"
      val refreshToken = "9876543442"
      val newToken = OauthTokens("11111111111","22222222222")
      val rtException = new RuntimeException("exception")

      when(service.apiConnector.helloUser(accessToken)).thenReturn(Future.failed(new UnauthorizedException("unauthorized")))
      when(service.oauthConnector.refreshToken(refreshToken)).thenReturn(Future.failed(rtException))
      service.helloOauth(accessToken, refreshToken).failed.futureValue shouldBe rtException
      verify(service.apiConnector).helloUser(accessToken) // failed call, token expired
      verify(service.oauthConnector).refreshToken(refreshToken) // refresh the token call fails
      verify(service.apiConnector,times(0)).helloUser(newToken.access_token) // hello user is not called
    }

    "return RuntimeException when backend fails" in new Setup {
      val accessToken = "023456789"
      val refreshToken = "111111111"
      val rtException = new RuntimeException("exception")
      when(service.apiConnector.helloUser(accessToken)).thenReturn(Future.failed(rtException))
      service.helloOauth(accessToken, refreshToken).failed.futureValue shouldBe rtException
    }

  }


}
