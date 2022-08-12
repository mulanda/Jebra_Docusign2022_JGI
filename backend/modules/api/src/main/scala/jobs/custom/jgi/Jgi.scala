package jobs.custom.jgi

import connections.ConnectionID
import core.constants.{Currencies, TimeZones}
import env.known.Users
import jobs.JobLifecycle.DEV
import jobs.custom.jgi.mediasync.JgiMediaSyncJob
import jobs.{JobDateFactory, JobID, PrivateJobGroup}
import providers.Provider.{ArcGis, Docusign, MediaValet}

import java.time.YearMonth
import java.util.{Currency, TimeZone}
import scala.util.matching.Regex

/**
 * Created by Daudi Chilongo on 06/26/2021.
 * Copyright (c) 2021 Jebra LTD. All rights reserved.
 */
object Jgi extends PrivateJobGroup  {

  def clientName: String = "Jgi"

  def tz: TimeZone = TimeZones.losAngeles

  def currency: Currency = Currencies.GBP

  object Connections {

    /** ArcGis */
    lazy val arcGIS: ConnectionID = ConnectionID(
      id = "JgiArcGisDemo",
      provider = ArcGis,
      userId = Users.jgiDemo.id,
      name = "ArcGis Demo"
    )

    /** Docusign */
    lazy val docusign: ConnectionID = ConnectionID(
      id = "JgiDocusignDemo",
      provider = Docusign,
      userId = Users.jgiDemo.id,
      name = "DocuSign Demo"
    )

    /** MediaValet */
    lazy val mediaValet: ConnectionID = ConnectionID(
      id = "JgiMediaValetDemo",
      provider = MediaValet,
      userId = Users.jgiDemo.id,
      name = "MediaValet Demo"
    )

  }

  object Jobs {

    lazy val mediaSync: JobID = JobID(
      id = "62f4030f83d9cd48e11e7cdc",
      jobType = JgiMediaSyncJob.jobType,
      subtype = None,
      userId = Users.jgiDemo.id,
      name = "MediaSync",
      lifecycle = DEV,
    )

  }

  /**
   * @todo: Move user-editable config to JobSettings
   */

  /**
   * MediaValet Categories of Interest
   */
  object MediaValetCategories {

    val Jebra = "34edee83-8902-4589-b539-004805635f92"
    val JebraCamera1 = "8451e91e-a437-427e-988a-7c3de2b6fe4b"
    val JebraCamera2 = "3de902fc-6dff-4747-8fc4-c7af8b6e53f8"
    val JebraUploads = "0fb02ab3-067d-4af7-b5eb-10dd320d0f38"
    val Hackathon2022 = "ab56aa6d-41dc-4046-8fb7-c526896414e7"
    val HackerJoe = "128e7b27-7575-49d7-bee9-ada8e44155c6"
    val HackerWill = "678d437d-ef35-48af-b1d5-c68598874858"

    /**
     * The user-configurable categories to process recursively.
     * @todo: In v2 we'll decouple the input and output assets so that
     *        we'll write to e.g a different output folder? A different
     *        version? For now we'll just read/write to the same asset
     */
    val sources: Seq[String] = Seq(Jebra)

    val sourceFolderRegex: Regex = "^(CAMERA(\\d+))_(\\d+)_(\\d+)_(\\d+)$".r // CAMERA2_4_08_2022

  }

  object MediaSync extends JobDateFactory {

    /** No need for end-of-month delay */
    private val yearMonthOverride: Option[YearMonth] = None //Some(YearMonth.parse("2022-08"))

    def defaultYearMonth: YearMonth = yearMonthOverride.getOrElse(tz.jobMonth)

    val MediaZip = "Jebra_JgiMediaSync.zip"

    val SignedDocumentTest: String =
      """
        |This is a test document
        |
        |In PROD this might be:
        |- The merged data zip [sign here]
        |- The output CSV / JSON [or SIGN here]
        |- The image files, etc
        |
        |And to test some anchor points lie [sign] here and also <sign> on and on
        |until you can not sign/sign/sign or SIGN/sign/SIGNERS// or SIGN/-UPs!!
        |
        |Happy Hacking!
        |
        |Sincerely, Jebra.
        |
        |""".stripMargin

  }

  /**
   * @todo: Make `accountId` configurable? Or save in providers.conf?
   */
  val DocusignAccountId: String = "0cb9cc73-184a-4a38-aaec-4ae9a7a52d2a"


}
