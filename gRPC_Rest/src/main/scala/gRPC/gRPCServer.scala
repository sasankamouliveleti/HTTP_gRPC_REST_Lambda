package gRPC

import HelperUtils.{Constants, CreateLogger}
import io.grpc.{Server, ServerBuilder}
import protobuf.TimeCheck.{checkLogsGrpc, lambdaRequest, lambdaResponse}

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

/* Reference - https://scalapb.github.io/docs/grpc */
object gRPCServer {
  /* Main method which gets the arguments*/
  def main(args: Array[String]):Unit = {
    val server = new gRPCServer(ExecutionContext.global) /* create the grpc server*/
    server.start() /* start the server*/
    server.blockUntilShutdown() /* receive requests until shutdown*/
  }
  private val port = Constants.portNumber /* define the portnumber where the server should serve*/
}


/* grpc server class which has different methods*/
class gRPCServer(executionContext: ExecutionContext) { self =>
  private[this] var server: Server = null
  val logger: Logger = CreateLogger(classOf[gRPCServer]) /* define logger of type grpcserver*/

  /* Method to start the server*/
  private def start(): Unit = {
    server = ServerBuilder.forPort(gRPCServer.port).addService(checkLogsGrpc.bindService(new lambdaImpl, executionContext)).build.start
    logger.info("Server started, listening on " + gRPCServer.port)
    sys.addShutdownHook {
      logger.info("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      logger.info("*** server shut down")
    }
  }

  /* To stop the server*/
  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  /* to inform to wait until termination*/
  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  /* class which implements the grpc method checklogs*/
  private class lambdaImpl extends checkLogsGrpc.checkLogs {

    /* method calllambda which takes protobuf request and returns future response*/
    override def callLambda(req: lambdaRequest): Future[lambdaResponse] = {

      implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
      implicit val executionContext: ExecutionContextExecutor = system.executionContext

      val uriToCall = Constants.makeUrl(req.timestamp, "", "1") /* make the url to call*/

      val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = uriToCall)) /* define the result object*/

      val timeout = 30000.millis /* defining the time out time for request to getthrough*/
      /* Await for future to get results*/
      val responseAsString = Await.result(
        responseFuture.flatMap((resp: HttpResponse) => Unmarshal(resp.entity).to[String]),
        timeout
      )
      /* Reference - https://www.baeldung.com/scala/circe-json*/
      /* parse the result fetched from lambda*/
      val parseResult: Either[ParsingFailure, Json] = parse(responseAsString)
      val ans : String = parseResult match {
        case Left(parsingError) =>
          throw new IllegalArgumentException(s"Invalid JSON object: ${parsingError.message}")
        case Right(json) =>
          val booleanOfLogTimeStamp = json \\ Constants.boolTimeStamp /* extract the key boolTImestamp*/
          val firstNumber: Option[Option[JsonNumber]] =
            booleanOfLogTimeStamp.collectFirst { case field => field.asNumber }
          logger.info("The result achieved from the lambda function is " + firstNumber.flatten.flatMap(_.toInt))
          firstNumber.flatten.flatMap(_.toInt).get.toString
      }
      logger.info("The ansValue from case is" + ans)
      val reply = lambdaResponse(message = ans)
      Future.successful(reply) /* return the protobuf response */
    }
  }
}