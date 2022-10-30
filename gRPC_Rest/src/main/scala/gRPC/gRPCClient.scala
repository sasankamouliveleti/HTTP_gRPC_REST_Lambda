package gRPC

import org.slf4j.Logger
import HelperUtils.{Constants, CreateLogger}
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}
import org.slf4j.event.Level
import protobuf.TimeCheck.checkLogsGrpc.checkLogsBlockingStub
import protobuf.TimeCheck.{checkLogsGrpc, lambdaRequest}

import scala.concurrent.ExecutionContext
import java.util.concurrent.TimeUnit


object gRPCClient {

  val logger: Logger = CreateLogger(classOf[gRPCClient])
  def apply(host: String, port: Int): gRPCClient = {
    val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build
    val blockingStub = checkLogsGrpc.blockingStub(channel)
    new gRPCClient(channel, blockingStub)
  }

  def main(args: Array[String]): Unit = {
    logger.info("*************Entering the gRPC Client Main Method**************")
    logger.info("The arguments are" + args(0))
    val client = gRPCClient(Constants.domainName, Constants.portNumber)
    try {
      val timeStamp = args(0)
      if (!Constants.timeStampValidity(timeStamp)) {
        throw IllegalArgumentException("Please provide timestamp in correct format %H:%M:%S.%f")
      }
      client.sendRequest(timeStamp)
    } finally {
      client.shutdown()
    }
    logger.info("*************Exiting the gRPC Client Main Method**************")
  }
}

class gRPCClient private(
                                private val channel: ManagedChannel,
                                private val blockingStub: checkLogsBlockingStub
                              ) {
  val logger: Logger = CreateLogger(classOf[gRPCClient])

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def sendRequest(timeStamp: String): Unit = {
    logger.info("*************Entering the sendRequest Method**************")
    logger.info("The give timeStamp and delta are" + timeStamp)
    val request = lambdaRequest(timestamp = timeStamp)
    try {
      val response = blockingStub.callLambda(request)
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