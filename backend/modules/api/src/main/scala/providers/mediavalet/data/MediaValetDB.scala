package providers.mediavalet.data

import core.db.{RootDocument, RootJsonCodecProvider}
import core.env.ExecContext
import core.util.SeqPlus.WithSeq
import dx.data.FetchedDB
import dx.data.FetchedData.SYNC_DATE
import env.ApiContext
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.{MongoCollection, MongoDatabase}
import providers.Provider
import providers.Provider.MediaValet

import scala.concurrent.Future
import scala.util.matching.Regex

/**
 * Created by Daudi Chilongo on 05/17/2021.
 * Copyright (c) 2021 Jebra LTD. All rights reserved.
 */
class MediaValetDB(val wrapped: MongoDatabase) extends FetchedDB {

  /** FetchedDB */

  override def providers: Seq[Provider] = Seq(MediaValet)

  def collectionsForConnection(connectionId: String): Seq[MongoCollection[_]] = {
    Seq(
      assets(connectionId),
      attributes(connectionId),
      categories(connectionId),
      orgUnits(connectionId),
      users(connectionId),
    )
  }

  override def resetIndexes(connectionId: String)
                           (implicit ec: ApiContext): Future[Unit] = {
    for {
      _ <- resetAssetsIndex(connectionId)
      _ <- resetAttributesIndex(connectionId)
      _ <- resetCategoriesIndex(connectionId)
      _ <- resetOrgUnitsIndex(connectionId)
      _ <- resetUsersIndex(connectionId)
    } yield {}
  }

  /** DynamicDB */

  override def dynamicCollections(implicit ec: ExecContext):
  Future[Seq[MongoCollection[_ <: RootDocument[_]]]] = {
    for {
      assets <- collectionsNamedLike(assetsRegex, assetsProvider)
      attributes <- collectionsNamedLike(attributesRegex, attributesProvider)
      categories <- collectionsNamedLike(categoryRegex, categoryProvider)
      orgUnits <- collectionsNamedLike(orgUnitsRegex, orgUnitsProvider)
      users <- collectionsNamedLike(usersRegex, usersProvider)
    } yield {
      Seq(
        assets,
        attributes,
        categories,
        orgUnits,
        users,
      ).flatten
    }
  }

  /** MongoDB */

  override def collectionFor[T](t: T)(implicit ec: ExecContext): Option[MongoCollection[T]] = {
    val collection = t match {
      case e: MediaValetAsset => assets(e.connectionId)
      case e: MediaValetAttribute => attributes(e.connectionId)
      case e: MediaValetCategory => categories(e.connectionId)
      case e: MediaValetOrgUnit => orgUnits(e.connectionId)
      case e: MediaValetUser => users(e.connectionId)
      case _ => null
    }
    Option(collection).map(_.asInstanceOf[MongoCollection[T]])
  }

  override def resetIndexes(deleteOnly: Boolean = false)
                           (implicit ec: ExecContext): Future[Unit] = {
    for {
      _ <- resetAssetsIndexes()
      _ <- resetAttributesIndexes()
      _ <- resetCategoriesIndexes()
      _ <- resetOrgUnitsIndexes()
      _ <- resetUsersIndexes()
    } yield {}
  }

  /** Assets **/

  private val assetsProvider  = new RootJsonCodecProvider[MediaValetAsset]()
  private val assetsSuffix: String = "assets"
  private val assetsRegex: Regex = regexOf(assetsSuffix)
  private def assetsName(connId: String): String = nameOf(connId, assetsSuffix)

  def assets(connId: String): MongoCollection[MediaValetAsset] = {
    wrap(assetsName(connId), assetsProvider)
  }

  protected def resetAssetsIndexes()(implicit ec: ExecContext): Future[Unit] = {
    wrapped.listCollectionNames().toFuture().flatMap { names =>
      names.serially {
        case assetsRegex(connId) => resetAssetsIndex(connId)
        case name => Future.successful(())
      }
    }
  }

  def resetAssetsIndex(connId: String)(implicit ec: ExecContext): Future[Unit] = {
    val collection = wrap(assetsName(connId), assetsProvider)
    for {
      _ <- collection.dropIndexesByName()
      _ <- collection.createIndex(ascending(SYNC_DATE)).toFuture()
    } yield {}
  }

  /** Attributes **/

  private val attributesProvider  = new RootJsonCodecProvider[MediaValetAttribute]()
  private val attributesSuffix: String = "attributes"
  private val attributesRegex: Regex = regexOf(attributesSuffix)
  private def attributesName(connId: String): String = nameOf(connId, attributesSuffix)

  def attributes(connId: String): MongoCollection[MediaValetAttribute] = {
    wrap(attributesName(connId), attributesProvider)
  }

  protected def resetAttributesIndexes()(implicit ec: ExecContext): Future[Unit] = {
    wrapped.listCollectionNames().toFuture().flatMap { names =>
      names.serially {
        case attributesRegex(connId) => resetAttributesIndex(connId)
        case name => Future.successful(())
      }
    }
  }

  def resetAttributesIndex(connId: String)(implicit ec: ExecContext): Future[Unit] = {
    val collection = wrap(attributesName(connId), attributesProvider)
    for {
      _ <- collection.dropIndexesByName()
      //_ <- collection.createIndex(ascending(SYNC_DATE)).toFuture()
    } yield {}
  }


  /** Categories **/

  private val categoryProvider  = new RootJsonCodecProvider[MediaValetCategory]()
  private val categorySuffix: String = "categories"
  private val categoryRegex: Regex = regexOf(categorySuffix)
  private def categoriesName(connId: String): String = nameOf(connId, categorySuffix)

  def categories(connId: String): MongoCollection[MediaValetCategory] = {
    wrap(categoriesName(connId), categoryProvider)
  }

  protected def resetCategoriesIndexes()(implicit ec: ExecContext): Future[Unit] = {
    wrapped.listCollectionNames().toFuture().flatMap { names =>
      names.serially {
        case categoryRegex(connId) => resetCategoriesIndex(connId)
        case name => Future.successful(())
      }
    }
  }

  def resetCategoriesIndex(connId: String)(implicit ec: ExecContext): Future[Unit] = {
    val collection = wrap(categoriesName(connId), categoryProvider)
    for {
      _ <- collection.dropIndexesByName()
      //_ <- collection.createIndex(ascending(SYNC_DATE)).toFuture()
    } yield {}
  }
  /** OrgUnits **/

  private val orgUnitsProvider  = new RootJsonCodecProvider[MediaValetOrgUnit]()
  private val orgUnitsSuffix: String = "orgs"
  private val orgUnitsRegex: Regex = regexOf(orgUnitsSuffix)
  private def orgUnitsName(connId: String): String = nameOf(connId, orgUnitsSuffix)

  def orgUnits(connId: String): MongoCollection[MediaValetOrgUnit] = {
    wrap(orgUnitsName(connId), orgUnitsProvider)
  }

  protected def resetOrgUnitsIndexes()(implicit ec: ExecContext): Future[Unit] = {
    wrapped.listCollectionNames().toFuture().flatMap { names =>
      names.serially {
        case orgUnitsRegex(connId) => resetOrgUnitsIndex(connId)
        case name => Future.successful(())
      }
    }
  }

  def resetOrgUnitsIndex(connId: String)(implicit ec: ExecContext): Future[Unit] = {
    val collection = wrap(orgUnitsName(connId), orgUnitsProvider)
    for {
      _ <- collection.dropIndexesByName()
      _ <- collection.createIndex(ascending(SYNC_DATE)).toFuture()
    } yield {}
  }


  /** Users **/

  private val usersProvider  = new RootJsonCodecProvider[MediaValetUser]()
  private val usersSuffix: String = "users"
  private val usersRegex: Regex = regexOf(usersSuffix)
  private def usersName(connId: String): String = nameOf(connId, usersSuffix)

  def users(connId: String): MongoCollection[MediaValetUser] = {
    wrap(usersName(connId), usersProvider)
  }

  protected def resetUsersIndexes()(implicit ec: ExecContext): Future[Unit] = {
    wrapped.listCollectionNames().toFuture().flatMap { names =>
      names.serially {
        case usersRegex(connId) => resetUsersIndex(connId)
        case name => Future.successful(())
      }
    }
  }

  def resetUsersIndex(connId: String)(implicit ec: ExecContext): Future[Unit] = {
    val collection = wrap(usersName(connId), usersProvider)
    for {
      _ <- collection.dropIndexesByName()
      _ <- collection.createIndex(ascending(SYNC_DATE)).toFuture()
      _ <- collection.createIndex(ascending("Company")).toFuture()
    } yield {}
  }

}
