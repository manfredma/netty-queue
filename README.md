Queue server
============

This is an simple java queue server.

Goals:

 - REST for simple integration
 - JSON for flexible message structures
 - Netty for fastest TCP/IP and HTTP server realization
 - Memory mapped files to access data as an transaction log

As result one instance allows:
 - send up to 165175 REST messages-at-request per second in concurrent mode

See integration test benchmarks for more details.

How to run
----------

    mvn exec:exec -DskipTests
    
REST usage
----------

server info 

    curl -i -s -XGET 'http://127.0.0.1:8080/?pretty'

server stats 

    curl -i -s -XGET 'http://127.0.0.1:8080/_stats?pretty'

create queue 

    curl -i -s -XPUT 'http://127.0.0.1:8080/my_queue?pretty'

queue stats

    curl -i -s -XGET 'http://127.0.0.1:8080/my_queue/_stats?pretty'

push message as string

    curl -i -s -XPOST 'http://127.0.0.1:8080/my_queue/message?pretty' -d '{"message":"Hello world 1"}'

push message as json

    curl -i -s -XPOST 'http://127.0.0.1:8080/my_queue/message?pretty' -d '{"message":{"Title":"Hello world"}}'

push with uuid message

    curl -i -s -XPOST 'http://127.0.0.1:8080/my_queue/message?pretty' -d '{"uuid":"a57586b7-3eed-4c7c-b257-8bf9021fffbd","message":"Hello world custom_uid"}'

delete with uuid

    curl -i -s -XDELETE 'http://127.0.0.1:8080/my_queue/message?uuid=a57586b7-3eed-4c7c-b257-8bf9021fffbd&pretty'
    curl -i -s -XDELETE 'http://127.0.0.1:8080/my_queue/message/a57586b7-3eed-4c7c-b257-8bf9021fffbd?pretty'

peek message with uuid

    curl -i -s -XHEAD 'http://127.0.0.1:8080/my_queue/message?pretty'

get message with uuid

    curl -i -s -XHEAD 'http://127.0.0.1:8080/my_queue/message/a57586b7-3eed-4c7c-b256-8bf9021fffbd?pretty'

pop message

    curl -i -s -XGET 'http://127.0.0.1:8080/my_queue/message?pretty'


delete queue

    curl -i -s -XDELETE 'http://127.0.0.1:8080/my_queue?reason=test&pretty'

See tests for communication example

Recommended server options
--------------------------

    localhost$ sysctl net.inet.tcp.msl
    net.inet.tcp.msl: 15000
    localhost$ sudo sysctl -w net.inet.tcp.msl=1000
    net.inet.tcp.msl: 15000 -> 1000
    localhost$ ulimit -n
    2048
    localhost$ ulimit -n 65536
    localhost$ ulimit -n
    65536

Recommended java options
------------------------

    -server
    -d64
    -Xms4g
    -Xmx4g
    -Djava.net.preferIPv4Stack=true
    -XX:+UseParNewGC
    -XX:+UseConcMarkSweepGC
    -XX:CMSInitiatingOccupancyFraction=75
    -XX:+UseCMSInitiatingOccupancyOnly
    -XX:MaxGCPauseMillis=150
    -XX:InitiatingHeapOccupancyPercent=70
    -XX:NewRatio=2
    -XX:SurvivorRatio=8
    -XX:MaxTenuringThreshold=15
    -XX:+UseCompressedOops
    -XX:+AggressiveOpts
    -XX:+UseFastAccessorMethods
    -XX:-MaxFDLimit
    -Dio.netty.leakDetectionLevel=disabled
