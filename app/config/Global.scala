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

package config

import play.api._
import play.api.mvc.Results._
import play.api.mvc._
import scala.concurrent.Future

sealed abstract class ErrorOutcome(
   val status: Int,
   val title: String,
   val heading: String,
   val message: String)

case object ErrorBadRequest extends ErrorOutcome(
  400,
  "Bad request - 400",
  "Bad request",
  "Please check that you have entered the correct web address.")

case object ErrorServerError extends ErrorOutcome(
  500,
  "Sorry, we are experiencing technical difficulties - 500",
  "Sorry, we’re experiencing technical difficulties",
  "Please try again in a few minutes.")

case object ErrorNotFound extends ErrorOutcome(
  404,
  "Page not found - 404",
  "This page can’t be found",
  "Please check that you have entered the correct web address.")


object Global extends GlobalSettings {

  def errorPage(err: ErrorOutcome): Future[Result] =
    Future.successful {Status(err.status)(views.html.global_error(err.title,err.heading,err.message))}

  override def onBadRequest(request: RequestHeader, error: String) = errorPage(ErrorBadRequest)

  override def onError(request: RequestHeader, throwable: Throwable) = errorPage(ErrorServerError)

  override def onHandlerNotFound(request: RequestHeader) = errorPage(ErrorNotFound)
}
