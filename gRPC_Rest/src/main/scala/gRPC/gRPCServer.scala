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

object gRPCServer {
  def main(args: Array[String]):Unit = {
    val server = new gRPCServer(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }
  private val port = Constants.portNumber
}


class gRPCServer(executionContext: ExecutionContext) { self =>
  private[this] var server: Server = null
  val logger: Logger = CreateLogger(classOf[gRPCServer])

  private def start(): Unit = {
    server = ServerBuilder.forPort(gRPCServer.port).addService(checkLogsGrpc.bindService(new GreeterImpl, executionContext)).build.start
    logger.info("Server started, listening on " + gRPCServer.port)
    sys.addShutdownHook {
      logger.info("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      logger.info("*** server shut down")
    }
  }

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  private class GreeterImpl extends checkLogsGrpc.checkLogs {
    override def callLambda(req: lambdaRequest): Future[lambdaResponse] = {

      implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
      // needed for the future flatMap/onComplete in the end
      implicit val executionContext: ExecutionContextExecutor = system.executionContext
      val uriToCall = Constants.makeUrl(req.timestamp, "", "1")
      //val uriToCall = "https://34ymq6qdql.execute-api.us-east-2.amazonaws.com/test/checktimestamp?" + "timestamp=" + req.timestamp + "&type=1&regex="+ "" + "&delta=" + ""
      val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = uriToCall))

      val timeout = 30000.millis
      val responseAsString = Await.result(
        responseFuture.flatMap((resp: HttpResponse) => Unmarshal(resp.entity).to[String]),
        timeout
      )
      val parseResult: Either[ParsingFailure, Json] = parse(responseAsString)
      val ans : String = parseResult match {
        case Left(parsingError) =>
          throw new IllegalArgumentException(s"Invalid JSON object: ${parsingError.message}")
        case Right(json) =>
          val booleanOfLogTimeStamp = json \\ Constants.boolTimeStamp
          val firstNumber: Option[Option[JsonNumber]] =
            booleanOfLogTimeStamp.collectFirst { case field => field.asNumber }
          logger.info("The result achieved from the lambda function is " + firstNumber.flatten.flatMap(_.toInt))
          firstNumber.flatten.flatMap(_.toInt).get.toString
      }
      logger.info("The ansValue from case is" + ans)
      val reply = lambdaResponse(message = ans)
      Future.successful(reply)
    }
  }
}