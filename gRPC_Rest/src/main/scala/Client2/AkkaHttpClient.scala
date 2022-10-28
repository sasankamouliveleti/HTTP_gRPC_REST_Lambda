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
    val logger = CreateLogger(classOf[AkkaHttpClient])
    if(args.length == 2){
      val timeStamp = args(0)
      val deltaTime = args(1)
      val invokeObject = new AkkaHttpClient()
      val invokedResult = invokeObject.callLambda(timeStamp, deltaTime)
      invokedResult.keys.foreach(value=>{
        logger.info("************* The bool value of whether the timestamp is in s3 is " + value)
      })
      invokedResult.values.foreach(value => {
        logger.info("************* The hash values are " + value)
      })
    }else{
      throw IllegalArgumentException("Please provide timestamp and deltaTime")
    }
  }
}

class AkkaHttpClient  {
  val logger: Logger = CreateLogger(classOf[AkkaHttpClient])

  def callLambda(timeStamp: String, deltaTime: String): Map[String, List[String]] = {
    
      implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
      // needed for the future flatMap/onComplete in the end
      implicit val executionContext: ExecutionContextExecutor = system.executionContext

      val uriToCall = Constants.makeUrl(timeStamp, deltaTime, "2")

      //val uriToCall = "https://34ymq6qdql.execute-api.us-east-2.amazonaws.com/test/checktimestamp?" + "timestamp=" + timeStamp + "&type=2&regex=" + ".*" + "&delta=" + deltaTime
      val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = uriToCall))

      val timeout = 30000.millis
      val responseAsString = Await.result(
        responseFuture.flatMap((resp: HttpResponse) => Unmarshal(resp.entity).to[String]),
        timeout
      )
      val parseResult: Either[ParsingFailure, Json] = parse(responseAsString)
      val ans: Map[String, List[String]] = parseResult match {
        case Left(parsingError) =>
          throw new IllegalArgumentException(s"Invalid JSON object: ${parsingError.message}")
        case Right(json) =>
          val booleanOfLogTimeStamp = json \\ Constants.boolTimeStamp
          val hashList = json \\ Constants.hashListString
          val firstNumber: Option[Option[JsonNumber]] =
            booleanOfLogTimeStamp.collectFirst { case field => field.asNumber }
          logger.info("The result achieved from the lambda function is " + firstNumber.flatten.flatMap(_.toInt))
          //logger.info("The datatype of hashlist is" + hashList)
          val listValue = hashList.map(value => {
            value.toString
          })
          Map(firstNumber.flatten.flatMap(_.toInt).get.toString -> listValue)
      }
      //logger.info("The ansValue from case is" + ans)
      ans
  }

}
