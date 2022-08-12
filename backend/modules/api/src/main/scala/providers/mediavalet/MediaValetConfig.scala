package providers.mediavalet

import providers.{Api, ApiConfig}

/**
 * Created by Daudi Chilongo on 08/09/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
case class MediaValetConfig(api: Api) extends ApiConfig[MediaValetConfig] {

  def subscriptionKey: String = (raw \ "subscriptionKey").as[String]

}
