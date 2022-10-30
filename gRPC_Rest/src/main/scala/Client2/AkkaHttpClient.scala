package Client2

import HelperUtils.{Constants, CreateLogger}

import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.*
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.ByteString
import io.circe.*
import io.circe.parser.*
import org.slf4j.Logger

import scala.util.{Failure, Success}
import scala.concurrent.duration.*

object AkkaHttpClient {
  def main(args: Array[String]): Unit = {
    val logger = CreateLogger(classOf[AkkaHttpClient]) /* Creates a logger of class type AkkaHttClient*/
    if(args.length == 2){
      val timeStamp = args(0) /* The argument 1 has timestamp*/
      val deltaTime = args(1) /* The argument 2 has delta*/
      /* Check the validity of input*/
      if(!Constants.timeStampValidity(timeStamp) || !Constants.timeStampValidity(deltaTime)){
        throw IllegalArgumentException("Please provide timestamp and delta in correct format %H:%M:%S.%f")
      }
      val invokeObject = new AkkaHttpClient() /* Create an object of type akkahttpclient*/
      val invokedResult = invokeObject.callLambda(timeStamp, deltaTime) /* Call the member method on the object*/
      /* Log the results from the HTTP call*/
      invokedResult.keys.foreach(value=>{
        logger.info("************* The bool value of whether the timestamp is in s3 is " + value)
      })
      invokedResult.values.foreach(value => {
        logger.info("************* The hash values are " + value)
      })
    }else{
      throw IllegalArgumentException("Please provide timestamp and deltaTime") /* throw err if the arguments are not provided*/
    }
  }
}

class AkkaHttpClient  {
  val logger: Logger = CreateLogger(classOf[AkkaHttpClient])

  /* Method which makes a call to the lambda function and returns the results in a Map of string and list*/
  def callLambda(timeStamp: String, deltaTime: String): Map[String, List[String]] = {
    
      implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
      implicit val executionContext: ExecutionContextExecutor = system.executionContext

      val uriToCall = Constants.makeUrl(timeStamp, deltaTime, "2") /* make the url to call */
    
      val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = uriToCall)) /* define the result object*/

      val timeout = Constants.timeouttime.millis /* defining the time out time for request to getthrough*/
      /* Await for future to get results*/
      val responseAsString = Await.result(
        responseFuture.flatMap((resp: HttpResponse) => Unmarshal(resp.entity).to[String]),
        timeout
      )
      /* parse the result fetched from lambda*/
      val parseResult: Either[ParsingFailure, Json] = parse(responseAsString)
      val ans: Map[String, List[String]] = parseResult match {
        case Left(parsingError) =>
          throw new IllegalArgumentException(s"Invalid JSON object: ${parsingError.message}")
        case Right(json) =>
          val booleanOfLogTimeStamp = json \\ Constants.boolTimeStamp /* extract the key boolTImestamp*/
          val hashList = json \\ Constants.hashListString /* extract the key hashList*/
          val firstNumber: Option[Option[JsonNumber]] =
            booleanOfLogTimeStamp.collectFirst { case field => field.asNumber }
          logger.info("The result achieved from the lambda function is " + firstNumber.flatten.flatMap(_.toInt))
          //logger.info("The datatype of hashlist is" + hashList)
          val listValue = hashList.map(value => {
            value.toString
          })
          Map(firstNumber.flatten.flatMap(_.toInt).get.toString -> listValue) /* return a map key string i.e boolval, and list of hashes*/
      }
      //logger.info("The ansValue from case is" + ans)
      ans 
  }

}
