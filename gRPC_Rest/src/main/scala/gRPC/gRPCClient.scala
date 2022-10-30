package gRPC

import org.slf4j.Logger
import HelperUtils.{Constants, CreateLogger}
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}
import org.slf4j.event.Level
import protobuf.TimeCheck.checkLogsGrpc.checkLogsBlockingStub
import protobuf.TimeCheck.{checkLogsGrpc, lambdaRequest}

import scala.concurrent.ExecutionContext
import java.util.concurrent.TimeUnit


/* The is a grpc client which communicates with the grpc server using protobufs*/
object gRPCClient {

  val logger: Logger = CreateLogger(classOf[gRPCClient])
  def apply(host: String, port: Int): gRPCClient = {
    val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build /*defining the channel or domain through which the communication to happen*/
    val blockingStub = checkLogsGrpc.blockingStub(channel) /* passing the param to the stub generated*/
    new gRPCClient(channel, blockingStub) /* invoking the client*/
  }

  def main(args: Array[String]): Unit = {
    logger.info("*************Entering the gRPC Client Main Method**************")
    logger.info("The arguments are" + args(0))
    val client = gRPCClient(Constants.domainName, Constants.portNumber) /*get a reference to client object*/
    try {
      val timeStamp = args(0)
      /* throw exception when the input is not valid*/
      if (!Constants.timeStampValidity(timeStamp)) {
        throw IllegalArgumentException("Please provide timestamp in correct format %H:%M:%S.%f")
      }
      /* send the request*/
      client.sendRequest(timeStamp)
    } finally {
      client.shutdown() /* shutdown the client */
    }
    logger.info("*************Exiting the gRPC Client Main Method**************")
  }
}

/* grpc client class which has different member methods to handle different tasks*/
class gRPCClient private(
                                private val channel: ManagedChannel,
                                private val blockingStub: checkLogsBlockingStub
                              ) {
  val logger: Logger = CreateLogger(classOf[gRPCClient]) /* create a logger of type grpclient*/

  /* method to shutdown the client*/
  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  /* method to send requst to the grpc server and get response back*/
  def sendRequest(timeStamp: String): Unit = {
    logger.info("*************Entering the sendRequest Method**************")
    logger.info("The give timeStamp and delta are" + timeStamp)
    val request = lambdaRequest(timestamp = timeStamp) /*create a protobuf request*/
    try {
      val response = blockingStub.callLambda(request) /*get the response from the server*/
      /* log the response from the server*/
      if(response.message == Constants.successResponse){
        logger.info("********The Timestamp given is in the S3 Bucket, you can proceed with other actions******")
      } else {
        logger.info("********The Timestamp given is not in the S3 Bucket******")
      }
      logger.info("The response received is: " + response.message)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.info("The exception occurred" + e.getStatus)
    }
    logger.info("*************Exiting the sendRequest Method**************")
  }
}