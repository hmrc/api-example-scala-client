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

package config

import javax.inject.{Inject, Provider, Singleton}

import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}

import connectors.{ApiConfig, OAuth20Config}
import controllers.HelloUserConfig

class ConfigurationModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[HelloUserConfig].toProvider[HelloUserConfigProvider],
      bind[OAuth20Config].toProvider[OAuth20ConfigProvider],
      bind[ApiConfig].toProvider[ApiConfigProvider]
    )
  }
}

object ConfigHelper {

  def callBackUrl(implicit configuration: Configuration) = {
    val callbackUrlBase = configuration.get[String]("callbackUrl")
    s"$callbackUrlBase/hello/hello-world/oauth20/callback"
  }

  def oauthUrlBase(implicit configuration: Configuration) = {
    configuration.get[String]("services.oauth")
  }

}

@Singleton
class HelloUserConfigProvider @Inject() (implicit val configuration: Configuration)
    extends Provider[HelloUserConfig] {

  override def get() = {
    val clientId     = configuration.get[String]("clientId")
    val callbackUrl  = ConfigHelper.callBackUrl
    val authorizeUrl = s"${ConfigHelper.oauthUrlBase}/oauth/authorize"
    HelloUserConfig(clientId, callbackUrl, authorizeUrl)
  }
}

@Singleton
class OAuth20ConfigProvider @Inject() (implicit val configuration: Configuration)
    extends Provider[OAuth20Config] {

  override def get() = {
    val clientId     = configuration.get[String]("clientId")
    val clientSecret = configuration.get[String]("clientSecret")
    val tokenUrl     = s"${ConfigHelper.oauthUrlBase}/oauth/token"
    val callbackUrl  = ConfigHelper.callBackUrl
    OAuth20Config(clientId, clientSecret, tokenUrl, callbackUrl)
  }
}

@Singleton
class ApiConfigProvider @Inject() (implicit val configuration: Configuration)
    extends Provider[ApiConfig] {

  override def get() = {
    val apiGateway  = configuration.get[String]("services.api-gateway")
    val serverToken = configuration.get[String]("serverToken")
    ApiConfig(apiGateway, serverToken)
  }
}
