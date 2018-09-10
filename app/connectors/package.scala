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

import connectors.UnauthorizedException
import play.Logger
import play.api.libs.json.{JsError, JsResult, JsSuccess, JsValue}
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

package object connectors {
  def extractJson[T](response: Future[WSResponse], validation: JsValue => JsResult[T] = returnSame): Future[T] = {
    response.map {
      r => {
        r.status match {
          case 200 => validation(r.json) match {
            case s: JsSuccess[T] => s.getOrElse(throw new RuntimeException("There is no token in the body"))
            case e: JsError => throw new RuntimeException(s"Failed to parse: ${r.body}")
          }
          case 401 => throw new UnauthorizedException(r.body)
          case _ =>
            Logger.error(s"WSResponse has status ${r.status}")
            throw new RuntimeException(r.body)
        }
      }
    }
  }

  def returnSame[T]: JsValue => JsResult[JsValue] =
    j => j.validate[JsValue]

}
