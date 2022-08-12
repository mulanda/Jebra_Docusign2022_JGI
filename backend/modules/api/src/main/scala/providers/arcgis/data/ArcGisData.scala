package providers.arcgis.data

import core.date.OffsetDateTimePlus
import dx.data.ProviderData
import play.api.libs.json.JsLookupResult
import providers.Provider
import providers.Provider.ArcGis

import java.time.OffsetDateTime

/**
 * Created by Daudi Chilongo on 11/13/2021.
 * Copyright (c) 2021 Jebra LTD. All rights reserved.
 */
trait ArcGisData[T] extends ProviderData[T] with OffsetDateTimePlus {

  override def provider: Provider = ArcGis

  /** Date helpers @todo: Generalize? */

  protected def offsetDate(key: String): Option[OffsetDateTime] = offsetDate(raw \ key)

  protected def offsetDate(r: JsLookupResult): Option[OffsetDateTime] = r.asOpt[String].map { date =>
    OffsetDateTime.parse(date)
  }


}

