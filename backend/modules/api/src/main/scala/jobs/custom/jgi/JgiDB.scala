package jobs.custom.jgi

import core.db.{RootDocument, RootJsonCodecProvider}
import core.env.ExecContext
import core.util.SeqPlus.WithSeq
import dx.data.OutputData.POSTED_KEY
import dx.data.{OutputDB, TableID}
import jobs.custom.jgi.JgiOutput.DATE_KEY
import jobs.custom.jgi.mediasync.output.JgiMediaRecord
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.{MongoCollection, MongoDatabase}

import scala.concurrent.Future
import scala.util.matching.Regex

/**
 * Created by Daudi Chilongo on 12/19/2021.
 * Copyright (c) 2021 Jebra LTD. All rights reserved.
 */
class JgiDB(val wrapped: MongoDatabase) extends OutputDB {

  override def groupId: String = Jgi.groupId

  /**
   * All generated data is organized by jobType_jobId
   */
  override def collectionsForJob(ID: TableID): Seq[MongoCollection[_]] = {
    super.collectionsForJob(ID) :++ Seq(
      mediaRecords(ID),
    )
  }

  /** DynamicDB */

  override def dynamicCollections(implicit ec: ExecContext):
  Future[Seq[MongoCollection[_ <: RootDocument[_]]]] = {
    for {
      superDynamics <- super.dynamicCollections

      /** Jgi Contacts */
      mediaRecords <- collectionsNamedLike(mediaRecordsRegex, mediaRecordsProvider)

    } yield {
      Seq(
        superDynamics,
        mediaRecords,
      ).flatten
    }
  }

  /** MongoDB */

  override def collectionFor[T](t: T)(implicit ec: ExecContext): Option[MongoCollection[T]] = {
    //import scala.language.existentials // Silence build warning re: wildcard inference
    val collection = super.collectionFor(t).getOrElse(t match {
      case d: JgiMediaRecord => mediaRecords(d.tableID)
      case _ => null
    })
    Option(collection).map(_.asInstanceOf[MongoCollection[T]])
  }

  // @todo: For now we we ignore deleteOnly
  override def resetIndexes(deleteOnly: Boolean = false)
                           (implicit ec: ExecContext): Future[Unit] = {
    mock("resetIndexes", deleteOnly)
    for {
      _ <- super.resetIndexes(deleteOnly)
      _ <- resetMediaRecordsIndexes()
    } yield {}
  }


  /** MediaRecord */

  private val mediaRecordsProvider  = new RootJsonCodecProvider[JgiMediaRecord]()
  private val mediaRecordsSuffix: String = "media_records"
  private val mediaRecordsRegex: Regex = suffixRegex(mediaRecordsSuffix)

  private def mediaRecordsName(ID: TableID): String = {
    ID.nameOf(mediaRecordsSuffix)
  }

  def mediaRecords(ID: TableID): MongoCollection[JgiMediaRecord] = {
    wrap(mediaRecordsName(ID), mediaRecordsProvider)
  }

  protected def resetMediaRecordsIndexes()(implicit ec: ExecContext): Future[Unit] = {
    wrapped.listCollectionNames().toFuture().flatMap { names =>
      names.serially {
        case mediaRecordsRegex(jobType, jobSubtype, jobId) =>
          resetMediaRecordsIndex(TableID(jobType, Option(jobSubtype), jobId))
        case _ => Future.successful(())
      }
    }
  }

  def resetMediaRecordsIndex(ID: TableID)(implicit ec: ExecContext): Future[Unit] = {
    val connectionName = mediaRecordsName(ID)
    val collection = wrap(connectionName, mediaRecordsProvider)
    for {
      _ <- dropIndexesFor(connectionName)

      // For filtering by date(s), POSTED status
      _ <- collection.createIndex(ascending(DATE_KEY, POSTED_KEY)).toFuture()
    } yield {}
  }

}
