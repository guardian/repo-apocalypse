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

### Restoring an archived and deleted repo

1. Create a new repo with the same name as the original. It is essential that the name and the Git organisation are the same
2. Download the archived zip file of the original repo from the S3 bucket where it is stored
3. Unzip it
4. ```cd``` into the unzipped folder
5. Run the following git commands ```git push --all origin``` & ```git push mirror origin```

This will fully restore the repo with all tags, branches etc