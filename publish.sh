#!/bin/bash

sbt clean compile core/doc docs/paradox

ROOT=../angiolep.github.io/projects/akka-wamp
rm -Rf $ROOT
cp -rp docs/target/paradox/site $ROOT
cp -rp core/target/scala-2.12/api $ROOT/api

cd ../angiolep.github.io
git add projects/akka-wamp
git commit -m "Update akka-wamp project docs"
git push




