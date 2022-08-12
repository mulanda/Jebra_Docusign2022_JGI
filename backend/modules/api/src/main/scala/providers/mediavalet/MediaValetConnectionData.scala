package providers.mediavalet

import core.date.DatePlus
import env.ApiContext
import fetchers.{GetCounter, GroupedFetch}
import providers.mediavalet.data.{MediaValetAsset, MediaValetAttribute, MediaValetCategory}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/**
 * Created by Daudi Chilongo on 08/10/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
trait MediaValetConnectionData {

  implicit class MediaValetDataWith(conn: MediaValetConnection)
                               (implicit ec: ApiContext,
                                gc: GetCounter = new GetCounter {})
    extends GroupedFetch with DatePlus {

    private def withFetcher[T](f: MediaValetFetcher => Future[T]): Future[T] = {
      conn.withUser { user =>
        f(MediaValetFetcher(user, conn))
      }
    }

    def updateAssets(): Future[Long] = {
      withFetcher { fetcher =>
        MediaValetAsset.t0(conn.id).latestDate
          .flatMap { case (t0, latest) =>
            groupedFetch(t0, replace = true) {
              fetcher.getAssets(latest.map(d => d - 1.day), None)
            }
          }
      }
    }

    def updateAttributes(): Future[Long] = {
      withFetcher { fetcher =>
        MediaValetAttribute.t0(conn.id).latestDate
          .flatMap { case (t0, latest) =>
            groupedFetch(t0, replace = true) {
              fetcher.getAttributes(latest.map(d => d - 1.day), None)
            }
          }
      }
    }

    def updateCategories(): Future[Long] = {
      withFetcher { fetcher =>
        MediaValetCategory.t0(conn.id).latestDate
          .flatMap { case (t0, latest) =>
            groupedFetch(t0, replace = true) {
              fetcher.getCategories(latest.map(d => d - 1.day), None)
            }
          }
      }
    }

  }

}
