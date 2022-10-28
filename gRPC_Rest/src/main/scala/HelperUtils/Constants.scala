package HelperUtils

import com.typesafe.config.{Config, ConfigFactory}
import gRPC.gRPCClient.logger

import scala.util.matching.Regex

object Constants {

  val successResponse: String = "1"
  val boolTimeStamp: String = "boolTimeStamp"
  val timeStampString: String = "timestamp="
  val typeVal: String = "&type="
  val regex: String = "&regex="
  val deltaString: String = "&delta="
  val hashListString: String = "hashlist"

  /* Getting user defined attributes from config file*/
  val config: Config = ConfigFactory.load("application.conf").getConfig("applicationConfigParams")
  val mainPattern: Regex = Constants.config.getString("regex").r
  val lambdaEndpoint: String = config.getString("lambdaEndpoint")
  val portNumber: Int = config.getInt("portNumber")
  val domainName: String = config.getString("domainName")

  def makeUrl(timestamp:String, delta: String, typeValue: String):String = {
    logger.info("The url built is " + lambdaEndpoint + timeStampString + timestamp + typeVal + typeValue + regex + mainPattern + deltaString + delta )
    lambdaEndpoint + timeStampString + timestamp + typeVal + typeValue + regex + mainPattern + deltaString + delta
  }
}
