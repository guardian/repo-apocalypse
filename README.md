# DEPRECATION NOTICE
**Please be aware that repo-apocalyse is considered deprecated, as the bulk of its use case is covered by Github's archiving functionality.**

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

### Getting an archived repo back into github
1. Download the .zip from S3 and unzip locally. This gives you the 'bare' repo.
2. Create a new repo in github. **Important - if there's any possiblity the repo contains secrets, make it private. You can always switch to public later on.**
3. From the directory that you unzipped the bare repo to, check that the remote is set up correctly with:
```
my-app$ git remote -v
origin	git@github.com:guardian/my-app.git (fetch)
origin	git@github.com:guardian/my-app.git (push)
```
4. If the origin urls above match that of your new repo, skip to step 6.
5. If the origin urls are not correct then set it to the new github repo with: `git remote set-url origin git@github.com:[org]/[app].git`.
6. Run `git push origin master`, and you should now see the code in github.
7. Use `git clone` as usual, from wherever you normally put projects locally.
