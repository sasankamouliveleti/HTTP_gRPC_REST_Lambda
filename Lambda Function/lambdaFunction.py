import json
import boto3
import re
import hashlib
from datetime import datetime, timedelta
import math


def binarySearchSearchForType2(allLogs, starTimeVal, endTimeVal):
    low = 0
    high = len(allLogs) - 1

    while(low <= high):
        mid = (low + high) // 2
        midValTime = datetime.strptime(
            allLogs[mid].split(" ")[0], "%H:%M:%S.%f")
        if(starTimeVal > midValTime):
            low = mid + 1
        elif(endTimeVal < midValTime):
            high = mid - 1
        else:
            return [low, high]
    return []


def binarySearchForType1(allLogs, searchTimeStamp):
    low = 0
    high = len(allLogs) - 1
    while(low <= high):
        mid = (low + high)//2
        midTimeStamp = datetime.strptime(
            allLogs[mid].split(" ")[0], "%H:%M:%S.%f")
        if(searchTimeStamp == midTimeStamp):
            return True
        elif(searchTimeStamp < midTimeStamp):
            high = mid - 1
        elif(searchTimeStamp > midTimeStamp):
            low = mid + 1
    return False

def buildResponse(statusCode, typeOfFunc, boolValue, hashList = []):
    response = {}
    response["boolTimeStamp"] = boolValue
    if(typeOfFunc == "2"):
        response["hashlist"] = hashList
    headers = {}
    headers["Content-Type"] = 'application/json'
    return{
        'statusCode': statusCode,
        'headers': headers,
        'body': json.dumps(response)
    }


def lambda_handler(event, context):
    s3 = boto3.resource('s3')
    bucket_name = s3.Bucket('ec2-logs-mouli')

    try:
        inputTimeFromReq = event["queryStringParameters"]["timestamp"]
        inputTimeVal = datetime.strptime(inputTimeFromReq, "%H:%M:%S.%f")

        print(inputTimeVal)

        inputRegex = event["queryStringParameters"]["regex"]

        inputDelta = event["queryStringParameters"]["delta"]

        functionalityType = event["queryStringParameters"]["type"]

        if(functionalityType == "1"):

            for obj in bucket_name.objects.all():
                key = obj.key
                body = obj.get()['Body'].read().decode('utf-8')
                allLogs = body.splitlines()
                print(allLogs)
                print(allLogs[0].split(" ")[0])

                logStartTime = datetime.strptime(
                    allLogs[0].split(" ")[0], "%H:%M:%S.%f")
                logEndTime = datetime.strptime(
                    allLogs[-1].split(" ")[0], "%H:%M:%S.%f")

                print(logStartTime)
                print(logEndTime)
                if(logStartTime <= inputTimeVal <= logEndTime):
                    if(binarySearchForType1(allLogs, inputTimeVal)):
                        return buildResponse(200, "1", 1, [])
            return buildResponse(404, "1", 0, [])
        elif(functionalityType == "2"):
            for obj in bucket_name.objects.all():
                key = obj.key
                body = obj.get()['Body'].read().decode('utf-8')
                allLogs = body.splitlines()
                print(allLogs)
                print(allLogs[0].split(" ")[0])

                logStartTime = datetime.strptime(
                    allLogs[0].split(" ")[0], "%H:%M:%S.%f")
                logEndTime = datetime.strptime(
                    allLogs[-1].split(" ")[0], "%H:%M:%S.%f")

                print(logStartTime)
                print(logEndTime)
                if(logStartTime <= inputTimeVal <= logEndTime):
                    timeDeltaArr = inputDelta.split(":")
                    print(timeDeltaArr)
                    startTime = inputTimeVal - timedelta(hours=int(timeDeltaArr[0])) - timedelta(
                        minutes=int(timeDeltaArr[1])) - timedelta(seconds=math.floor(float(timeDeltaArr[2])))
                    endTime = inputTimeVal + timedelta(hours=int(timeDeltaArr[0])) + timedelta(
                        minutes=int(timeDeltaArr[1])) + timedelta(seconds=math.floor(float(timeDeltaArr[2])))
                    print(startTime, endTime)
                    rangeVal = binarySearchSearchForType2(
                        allLogs, startTime, endTime)
                    print(rangeVal)
                    if(len(rangeVal) > 0):
                        ansMD5 = []
                        for logLine in allLogs[rangeVal[0]: rangeVal[1] + 1]:
                            if(re.search(inputRegex, logLine)):
                                logVal = str(logLine)
                                md5Val = hashlib.md5(logVal.encode())
                                ansMD5.append(md5Val.hexdigest())
                        print(ansMD5)
                        return buildResponse(200, "2", 1, ansMD5)
                    else:
                        return buildResponse(404, "2", 0, [])
                else:
                    continue
            return buildResponse(404, "2", 0, [])
        else:
            headers = {}
            headers["Content-Type"] = 'application/json'
            return{
                'statusCode': 404,
                'headers': headers,
                'body': json.dumps("Please send correct functionality type")
            }

    except Exception as e:
        print(e)
        raise e
