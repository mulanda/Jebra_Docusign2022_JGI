package providers.arcgis.data

import core.db.{RootDocument, RootJsonCodecProvider}
import core.env.ExecContext
import core.util.SeqPlus.WithSeq
import dx.data.FetchedDB
import dx.data.FetchedData.SYNC_DATE
import env.ApiContext
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.{MongoCollection, MongoDatabase}
import providers.Provider
import providers.Provider.ArcGis

import scala.concurrent.Future
import scala.util.matching.Regex

/**
 * Created by Daudi Chilongo on 05/17/2021.
 * Copyright (c) 2021 Jebra LTD. All rights reserved.
 */
class ArcGisDB(val wrapped: MongoDatabase) extends FetchedDB {

  /** FetchedDB */

  override def providers: Seq[Provider] = Seq(ArcGis)

  def collectionsForConnection(connectionId: String): Seq[MongoCollection[_]] = {
    Seq(
      surveyPayload(connectionId),
    )
  }

  override def resetIndexes(connectionId: String)
                           (implicit ec: ApiContext): Future[Unit] = {
    for {
      _ <- resetSurveyPayloadIndex(connectionId)
    } yield {}
  }

  /** DynamicDB */

  override def dynamicCollections(implicit ec: ExecContext):
  Future[Seq[MongoCollection[_ <: RootDocument[_]]]] = {
    for {
      assets <- collectionsNamedLike(surveyPayloadRegex, surveyPayloadProvider)
    } yield {
      Seq(
        assets,
      ).flatten
    }
  }

  /** MongoDB */

  override def collectionFor[T](t: T)(implicit ec: ExecContext): Option[MongoCollection[T]] = {
    val collection = t match {
      case e: ArcGisSurveyPayload => surveyPayload(e.connectionId)
      case _ => null
    }
    Option(collection).map(_.asInstanceOf[MongoCollection[T]])
  }

  override def resetIndexes(deleteOnly: Boolean = false)
                           (implicit ec: ExecContext): Future[Unit] = {
    for {
      _ <- resetSurveyPayloadIndexes()
    } yield {}
  }

  /** SurveyPayload **/

  private val surveyPayloadProvider  = new RootJsonCodecProvider[ArcGisSurveyPayload]()
  private val surveyPayloadSuffix: String = "survey"
  private val surveyPayloadRegex: Regex = regexOf(surveyPayloadSuffix)
  private def surveyPayloadName(connId: String): String = nameOf(connId, surveyPayloadSuffix)

  def surveyPayload(connId: String): MongoCollection[ArcGisSurveyPayload] = {
    wrap(surveyPayloadName(connId), surveyPayloadProvider)
  }

  protected def resetSurveyPayloadIndexes()(implicit ec: ExecContext): Future[Unit] = {
    wrapped.listCollectionNames().toFuture().flatMap { names =>
      names.serially {
        case surveyPayloadRegex(connId) => resetSurveyPayloadIndex(connId)
        case name => Future.successful(())
      }
    }
  }

  def resetSurveyPayloadIndex(connId: String)(implicit ec: ExecContext): Future[Unit] = {
    val collection = wrap(surveyPayloadName(connId), surveyPayloadProvider)
    for {
      _ <- collection.dropIndexesByName()
      _ <- collection.createIndex(ascending(SYNC_DATE)).toFuture()
    } yield {}
  }

}
