package scala

import Client2.AkkaHttpClient
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import HelperUtils.Constants
import com.typesafe.config.{Config, ConfigFactory}

class gRPCLambdaHttpTester extends AnyFlatSpec with Matchers {
  behavior of "Various Constants and Methods"

  it should "get correct endpoint defined in application config" in {
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

  it should "check the validity of the given input timestamp and delta" in {
    val timeStampVal: String = "12:24:00"
    val fromConsts = Constants.timeStampValidity(timeStampVal)
    false shouldBe fromConsts
  }

  it should "check the validity of the given input delta" in {
    val timeStampVal: String = "12:24:00.01"
    val fromConsts = Constants.timeStampValidity(timeStampVal)
    true shouldBe fromConsts
  }

  it should "return correct URL formed for rest request" in {
    val url = "https://34ymq6qdql.execute-api.us-east-2.amazonaws.com/test/checktimestamp?timestamp=23:18:26.0&type=2&regex="+Constants.mainPattern+"&delta=0:0:16.0"
    val fromConstVal = Constants.makeUrl("23:18:26.0", "0:0:16.0", "2")
    println(url)
    url shouldBe fromConstVal
  }
  it should "check whether the give timestamp present or not test" in {
    val objectVal = new AkkaHttpClient()
    val result = objectVal.callLambda("23:18:26.0", "0:0:16.0")
    val keysList = result.keys.toList
    keysList.head shouldBe "1"
  }
}
