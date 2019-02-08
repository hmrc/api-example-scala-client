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

package config

import connectors.{ApiConfig, OAuth20Config}
import controllers.HelloUserConfig
import javax.inject.{Inject, Provider, Singleton}
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}

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

  def getConfig[T](key: String, f: String => Option[T]): T = {
    f(key).getOrElse(throw new IllegalStateException(s"[$key] is not configured!"))
  }

  def callBackUrl(implicit runModeConfiguration: Configuration) = {
    val callbackUrlBase = ConfigHelper.getConfig("callbackUrl", runModeConfiguration.getString(_))
    s"$callbackUrlBase/hello/hello-world/oauth20/callback"
  }

  def oauthUrlBase(implicit runModeConfiguration: Configuration) = {
    getConfig("services.oauth", runModeConfiguration.getString(_))
  }

}

@Singleton
class HelloUserConfigProvider @Inject()(implicit val runModeConfiguration: Configuration)
  extends Provider[HelloUserConfig] {

  override def get() = {
    val clientId = ConfigHelper.getConfig("clientId", runModeConfiguration.getString(_))
    val callbackUrl = ConfigHelper.callBackUrl(runModeConfiguration)
    val authorizeUrl = s"${ConfigHelper.oauthUrlBase}/oauth/authorize"
    HelloUserConfig(clientId, callbackUrl, authorizeUrl)
  }
}

@Singleton
class OAuth20ConfigProvider @Inject()(implicit val runModeConfiguration: Configuration)
  extends Provider[OAuth20Config] {

  override def get() = {
    val clientId = ConfigHelper.getConfig("clientId", runModeConfiguration.getString(_))
    val clientSecret = ConfigHelper.getConfig("clientSecret", runModeConfiguration.getString(_))
    val tokenUrl = s"${ConfigHelper.oauthUrlBase}/oauth/token"
    val callbackUrl = ConfigHelper.callBackUrl
    OAuth20Config(clientId, clientSecret, tokenUrl, callbackUrl)
  }
}

@Singleton
class ApiConfigProvider @Inject()(implicit val runModeConfiguration: Configuration)
  extends Provider[ApiConfig] {

  override def get() = {
    val apiGateway = ConfigHelper.getConfig("services.api-gateway", runModeConfiguration.getString(_))
    val serverToken = ConfigHelper.getConfig("serverToken", runModeConfiguration.getString(_))
    ApiConfig(apiGateway, serverToken)
  }
}


