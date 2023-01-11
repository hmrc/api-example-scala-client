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

package controllers

import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{HelloUserService, OauthTokens}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import play.api.mvc.MessagesControllerComponents
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import org.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.mockito.ArgumentMatchersSugar
import scala.concurrent.ExecutionContext.Implicits.global

class HelloUserSpec extends PlaySpec with Matchers with ScalaFutures with MockitoSugar with ArgumentMatchersSugar with GuiceOneAppPerTest {

  implicit val hc = HeaderCarrier()

  trait Setup {
    val config               = HelloUserConfig("CLIENT_ID", "http://helloworld.org", "http://authorizeurl.org")
    val mockHelloUserService = mock[HelloUserService]
    val mcc                  = app.injector.instanceOf[MessagesControllerComponents]
    val controller           = new HelloUser(mockHelloUserService, config, mcc)
  }

  "HelloUser callback" should {
    "return 200 with a json when authorization code is provided" in new Setup {
      val authorizationCode = "11111111"
      val token             = OauthTokens("023456789", "9876543442")
      when(mockHelloUserService.helloOauth(eqTo(authorizationCode))(*)).thenReturn(
        Future.successful((Json.parse("""{"message":"hello User"}"""), token))
      )
      val result            = controller.helloWithCallback(Some(authorizationCode), None).apply(FakeRequest())
      verify(mockHelloUserService).helloOauth(eqTo(authorizationCode))(*)

      status(result) mustBe 200
      session(result).get("token").get mustBe token.access_token
      session(result).get("refresh_token").get mustBe token.refresh_token
      contentAsJson(result) mustBe Json.parse("""{"message":"hello User"}""")
    }

    "return internal server error when authorization code is not given" in new Setup {
      val result = controller.helloWithCallback(None, None).apply(FakeRequest())
      status(result) mustBe 500
      contentAsString(result) mustBe "Did not receive the Authorization Code"
    }

    "return internal server error when error is given" in new Setup {
      val result = controller.helloWithCallback(None, Some("error")).apply(FakeRequest())
      status(result) mustBe 500
      contentAsString(result) mustBe "Error passed by caller: 'error'"
    }

    "return internal server error when error is given and auth code is given too" in new Setup {
      val result = controller.helloWithCallback(Some("authCode"), Some("error")).apply(FakeRequest())
      status(result) mustBe 500
      contentAsString(result) mustBe "Error passed by caller: 'error'"
    }

    "return internal server eror when service fails" in new Setup {
      val authorizationCode = "11111111"
      when(mockHelloUserService.helloOauth(eqTo(authorizationCode))(*)).thenReturn(
        Future.failed(new RuntimeException("exception"))
      )
      val result            = controller.helloWithCallback(Some(authorizationCode), None).apply(FakeRequest())
      status(result) mustBe 500
      contentAsString(result) mustBe "exception"
    }
  }

  "HelloUser hello with tokens" should {

    "redirect to the oauth frontend when there is no token in the session" in new Setup {
      val result = controller.hello().apply(FakeRequest())
      status(result) mustBe 303
      headers(result).get("location").get mustBe
        "http://authorizeurl.org?client_id=CLIENT_ID&scope=hello&response_type=code&redirect_uri=http%3A%2F%2Fhelloworld.org"
    }

    "return 200 with a json when there is a valid access token and refresh token in the session" in new Setup {
      val accessToken  = "023456789"
      val refreshToken = "9876543442"
      val oldToken     = OauthTokens(accessToken, refreshToken)
      when(mockHelloUserService.helloOauth(eqTo(accessToken), eqTo(refreshToken))(*)).thenReturn(
        Future.successful((Json.parse("""{"message":"hello User"}"""), oldToken))
      )

      val req = FakeRequest().withSession(
        "token"         -> accessToken,
        "refresh_token" -> refreshToken
      )

      val result: Future[Result] = controller.hello().apply(req)
      verify(mockHelloUserService).helloOauth(eqTo(accessToken), eqTo(refreshToken))(*)
      status(result) mustBe 200
      session(result).get("token").get mustBe oldToken.access_token
      session(result).get("refresh_token").get mustBe oldToken.refresh_token

      contentAsJson(result) mustBe Json.parse("""{"message":"hello User"}""")
    }

    "return 200 with a json when it gets a new token from service" in new Setup {
      val accessToken  = "023456789"
      val refreshToken = "9876543442"
      val newToken     = OauthTokens("111111111", "222222222")
      when(mockHelloUserService.helloOauth(eqTo(accessToken), eqTo(refreshToken))(*)).thenReturn(
        Future.successful((Json.parse("""{"message":"hello User"}"""), newToken))
      )

      val req = FakeRequest().withSession(
        "token"         -> accessToken,
        "refresh_token" -> refreshToken
      )

      val result: Future[Result] = controller.hello().apply(req)
      verify(mockHelloUserService).helloOauth(eqTo(accessToken), eqTo(refreshToken))(*)
      status(result) mustBe 200
      session(result).get("token").get mustBe newToken.access_token
      session(result).get("refresh_token").get mustBe newToken.refresh_token

      contentAsJson(result) mustBe Json.parse("""{"message":"hello User"}""")
    }

    "return 500 when service fails" in new Setup {
      val accessToken            = "023456789"
      val refreshToken           = "9876543442"
      when(mockHelloUserService.helloOauth(eqTo(accessToken), eqTo(refreshToken))(*)).thenReturn(
        Future.failed(new RuntimeException("exception"))
      )
      val req                    = FakeRequest().withSession(
        "token"         -> accessToken,
        "refresh_token" -> refreshToken
      )
      val result: Future[Result] = controller.hello().apply(req)
      verify(mockHelloUserService).helloOauth(eqTo(accessToken), eqTo(refreshToken))(*)
      status(result) mustBe 500
      contentAsString(result) mustBe "exception"
    }

  }

}
