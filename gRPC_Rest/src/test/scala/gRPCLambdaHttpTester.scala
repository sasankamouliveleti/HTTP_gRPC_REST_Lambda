package scala

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import HelperUtils.Constants
import com.typesafe.config.{Config, ConfigFactory}

class gRPCLambdaHttpTester extends AnyFlatSpec with Matchers {
  behavior of "Various Constants and Methods"

  it should "get correct defined in application config" in {
    val getUrl = Constants.lambdaEndpoint
    val config: Config = ConfigFactory.load("application.conf").getConfig("applicationConfigParams")
    val lambdaEndpoint: String = config.getString("lambdaEndpoint")
    getUrl shouldBe lambdaEndpoint
  }

  it should "get correct port for gRPC connected in application config" in {
    val portval: Int = Constants.portNumber
    val config: Config = ConfigFactory.load("application.conf").getConfig("applicationConfigParams")
    val port: Int = config.getInt("portNumber")
    portval shouldBe port
  }

//  it should "get correct port for gRPC connected in application config" in {
//    val portval = Constants.portNumber
//    val config: Config = ConfigFactory.load("application.conf").getConfig("applicationConfigParams")
//    val port: String = config.getString("portNumber")
//    portval shouldBe port
//  }
}
