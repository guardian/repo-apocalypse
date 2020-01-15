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

### Using an archived repository

To bring an archived repo back to life:
1. Download the .zip from S3 and unzip it locally to get the 'bare' repo
2. Clone from the local bare repo directory: `git clone /path/to/bare-repo`. Git will unpack the source files from the bare repo
3. Create a new repo in github (remember to make it private if the repo contains secrets!)
4. Update the remote to point at github (rather than locally): `git remote set-url origin git@github.com:[org]/[app].git`
