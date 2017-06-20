# repo-apocalypse

_A time to be [born](https://github.com/guardian/repo-genesis), and a time to die_

Archive old Github repositories to S3.

### Running locally

repo-apocalypse is built using [API Gateway](https://aws.amazon.com/api-gateway/) 
and [Lambda](https://aws.amazon.com/lambda/), but can be run locally as a standard 
web-app. From the SBT prompt, run `~local/re-start` and the app will be started 
attached to port 8080 and will be recompiled and restarted on each save using 
[sbt-revolver](https://github.com/spray/sbt-revolver).

It is configured via a number of [environment variables](https://github.com/guardian/repo-apocalypse/blob/master/src/main/scala/com/gu/repoapocalypse/Env.scala)
which will need to be set to run successfully.
