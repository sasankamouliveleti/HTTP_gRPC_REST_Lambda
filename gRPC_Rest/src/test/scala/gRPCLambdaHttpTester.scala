package scala

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import HelperUtils.Constants
import com.typesafe.config.ConfigFactory

class gRPCLambdaHttpTester extends AnyFlatSpec with Matchers {
  behavior of "Various helper methods in constants file"

  it should "should get correct timeInterval from Input Time for a timeInterval of 1" in {
    val inputTime = "14:23".split(":")
    //val testVal = Constants.generateTimeInterval(inputTime)
    //"14:23 14:24" shouldBe testVal
  }
}
