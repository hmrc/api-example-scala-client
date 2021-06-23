/*
 * Copyright 2021 HM Revenue & Customs
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

import connectors.{ApiConnector, OAuth20Connector, OauthResponse, UnauthorizedException}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class HelloServiceSpec extends WordSpec with Matchers with ScalaFutures with MockitoSugar {

  implicit val hc = HeaderCarrier()

  trait Setup {
    val mockApiConnector = mock[ApiConnector]
    val mockOauthConnector: OAuth20Connector = mock[OAuth20Connector]
    val service = new HelloUserService(mockApiConnector, mockOauthConnector)
  }

  "Hello with authorization code" should {
    "return a valid JsValue and a token when access code is valid" in new Setup {
      val authorizationCode = "1111111111"
      val oauthResponse = OauthResponse("11111111111", "22222222222", 10000)
      val tokens = OauthTokens("11111111111", "22222222222")

      when(mockOauthConnector.getToken(authorizationCode)).thenReturn(Future.successful(oauthResponse))
      when(mockApiConnector.helloUser(meq(oauthResponse.access_token))(any())).thenReturn(Future.successful(Json.parse( """{"message":"hello User"}""")))
      val (jv, t) = service.helloOauth(authorizationCode).futureValue

      jv shouldBe Json.parse( """{"message":"hello User"}""")
      t shouldBe tokens
    }
  }

  "Hello with access token and refresh token" should {
    "return a valid JsValue and no token when the oauth token is valid" in new Setup {
      val accessToken = "023456789"
      val refreshToken = "111111111"
      val oldToken = OauthTokens(accessToken, refreshToken)
      when(mockApiConnector.helloUser(accessToken)).thenReturn(Future.successful(Json.parse( """{"message":"hello User"}""")))
      val (jv, t) = service.helloOauth(accessToken, refreshToken).futureValue
      jv shouldBe Json.parse( """{"message":"hello User"}""")
      t shouldBe oldToken
      verify(mockApiConnector).helloUser(accessToken)
    }

    "return a valid JsValue and new token when the token was expired and was refreshed" in new Setup {
      val accessToken = "023456789"
      val refreshToken = "9876543442"
      val oauthResponse = OauthResponse("11111111111", "22222222222", 10000)
      val newTokens = OauthTokens("11111111111", "22222222222")

      when(mockApiConnector.helloUser(accessToken)).thenReturn(Future.failed(new UnauthorizedException("unauthorized")))
      when(mockOauthConnector.refreshToken(refreshToken)).thenReturn(Future.successful(oauthResponse))
      when(mockApiConnector.helloUser(oauthResponse.access_token)).thenReturn(Future.successful(Json.parse( """{"message":"hello User"}""")))
      val (jv, t) = service.helloOauth(accessToken, refreshToken).futureValue
      jv shouldBe Json.parse( """{"message":"hello User"}""")
      t shouldBe newTokens
      verify(mockApiConnector).helloUser(accessToken) // failed call, token expired
      verify(mockOauthConnector).refreshToken(refreshToken) // refresh the token call
      verify(mockApiConnector).helloUser(oauthResponse.access_token) // success call, token is refreshed
    }

    "return RuntimeException when the token was expired and refreshing failed" in new Setup {
      val accessToken = "023456789"
      val refreshToken = "9876543442"
      val newToken = OauthTokens("11111111111", "22222222222")
      val rtException = new RuntimeException("exception")

      when(mockApiConnector.helloUser(accessToken)).thenReturn(Future.failed(new UnauthorizedException("unauthorized")))
      when(mockOauthConnector.refreshToken(refreshToken)).thenReturn(Future.failed(rtException))
      service.helloOauth(accessToken, refreshToken).failed.futureValue shouldBe rtException
      verify(mockApiConnector).helloUser(accessToken) // failed call, token expired
      verify(mockOauthConnector).refreshToken(refreshToken) // refresh the token call fails
      verify(mockApiConnector, times(0)).helloUser(newToken.access_token) // hello user is not called
    }

    "return RuntimeException when backend fails" in new Setup {
      val accessToken = "023456789"
      val refreshToken = "111111111"
      val rtException = new RuntimeException("exception")
      when(mockApiConnector.helloUser(accessToken)).thenReturn(Future.failed(rtException))
      service.helloOauth(accessToken, refreshToken).failed.futureValue shouldBe rtException
    }

  }

}
