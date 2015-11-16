/*
 * Copyright 2015 HM Revenue & Customs
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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{MustMatchers}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.{Result}
import play.api.test.{WithApplication, FakeRequest}
import services.{OauthTokens, HelloUserService}
import play.api.test.Helpers._
import org.mockito.Mockito._

import scala.concurrent.Future

class HelloUserSpec extends PlaySpec with MustMatchers with ScalaFutures with MockitoSugar {


  trait Setup {
    val controller = new HelloUser {
      override val service: HelloUserService = mock[HelloUserService]
      override val clientId: String = "CLIENT_ID"
      override val authorizeUrl: String = "http://authorizeurl.org"
      override val callbackUrl: String = "http://helloworld.org"
    }
  }

  "HelloUser callback" should {
    "return 200 with a json when authorization code is provided" in new WithApplication() with Setup{
      val authorizationCode = "11111111"
      val token = new OauthTokens("023456789", "9876543442")
      when(controller.service.helloOauth(authorizationCode)).thenReturn(
        Future.successful((Json.parse( """{"message":"hello User"}"""),token))
      )
      val result = controller.helloWithCallback(Some(authorizationCode), None).apply(FakeRequest())
      verify(controller.service).helloOauth(authorizationCode)

      status(result) mustBe 200
      session(result).get("token").get mustBe token.access_token
      session(result).get("refresh_token").get mustBe token.refresh_token
      contentAsJson(result) mustBe Json.parse( """{"message":"hello User"}""")
    }

    "return internal server error when authorization code is not given" in new WithApplication() with Setup{
      val result = controller.helloWithCallback(None, None).apply(FakeRequest())
      status(result) mustBe 500
      contentAsString(result) mustBe "Did not receive the Authorization Code"
    }

    "return internal server error when error is given" in new WithApplication() with Setup{
      val result = controller.helloWithCallback(None, Some("error")).apply(FakeRequest())
      status(result) mustBe 500
      contentAsString(result) mustBe "Error passed by caller: 'error'"
    }

    "return internal server error when error is given and auth code is given too" in new WithApplication() with Setup{
      val result = controller.helloWithCallback(Some("authCode"), Some("error")).apply(FakeRequest())
      status(result) mustBe 500
      contentAsString(result) mustBe "Error passed by caller: 'error'"
    }

    "return internal server eror when service fails"  in new WithApplication() with Setup {
      val authorizationCode = "11111111"
      val token = new OauthTokens("023456789", "9876543442")
      when(controller.service.helloOauth(authorizationCode)).thenReturn(
        Future.failed(new RuntimeException("exception"))
      )
      val result = controller.helloWithCallback(Some(authorizationCode), None).apply(FakeRequest())
      status(result) mustBe 500
      contentAsString(result) mustBe "exception"
    }
  }

  "HelloUser hello with tokens" should {

    "redirect to the oauth frontend when there is no token in the session" in new WithApplication() with Setup{
      val result = controller.hello().apply(FakeRequest())
      status(result) mustBe 303
      headers(result).get("location").get mustBe "http://authorizeurl.org?client_id=CLIENT_ID&scope=hello&response_type=code&redirect_uri=http%3A%2F%2Fhelloworld.org"
    }

    "return 200 with a json when there is a valid access token and refresh token in the session" in new WithApplication() with Setup{
      val accessToken = "023456789"
      val refreshToken = "9876543442"
      val oldToken = new OauthTokens(accessToken, refreshToken)
      when(controller.service.helloOauth(accessToken,refreshToken)).thenReturn(
        Future.successful((Json.parse( """{"message":"hello User"}"""),oldToken))
      )

      val req = FakeRequest().withSession(
        "token" -> accessToken,
        "refresh_token" -> refreshToken
      )

      val result: Future[Result]   = controller.hello().apply(req)
      verify(controller.service).helloOauth(accessToken,refreshToken)
      status(result) mustBe 200
      session(result).get("token").get mustBe oldToken.access_token
      session(result).get("refresh_token").get mustBe oldToken.refresh_token

      contentAsJson(result) mustBe Json.parse( """{"message":"hello User"}""")
    }

    "return 200 with a json when it gets a new token from service" in new WithApplication() with Setup{
      val accessToken = "023456789"
      val refreshToken = "9876543442"
      val newToken = OauthTokens("111111111","222222222")
      when(controller.service.helloOauth(accessToken,refreshToken)).thenReturn(
        Future.successful((Json.parse( """{"message":"hello User"}"""),newToken))
      )

      val req = FakeRequest().withSession(
        "token" -> accessToken,
        "refresh_token" -> refreshToken
      )

      val result: Future[Result]   = controller.hello().apply(req)
      verify(controller.service).helloOauth(accessToken,refreshToken)
      status(result) mustBe 200
      session(result).get("token").get mustBe newToken.access_token
      session(result).get("refresh_token").get mustBe newToken.refresh_token

      contentAsJson(result) mustBe Json.parse( """{"message":"hello User"}""")
    }

    "return 500 when service fails" in new WithApplication() with Setup{
      val accessToken = "023456789"
      val refreshToken = "9876543442"
      when(controller.service.helloOauth(accessToken,refreshToken)).thenReturn(
        Future.failed(new RuntimeException("exception"))
      )
      val req = FakeRequest().withSession(
        "token" -> accessToken,
        "refresh_token" -> refreshToken
      )
      val result: Future[Result]   = controller.hello().apply(req)
      verify(controller.service).helloOauth(accessToken,refreshToken)
      status(result) mustBe 500
      contentAsString(result) mustBe "exception"
    }



  }

}
