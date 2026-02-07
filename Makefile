# Makefile: builds the project and runs game/launcher (scriptless)
.PHONY: all build run run-game run-launcher clean

GRADLE := ./gradlew
JRE := ./jre/bin/java
JVM_OPTS := -XstartOnFirstThread \
	-Dfml.earlyprogresswindow=false \
	-Dfile.encoding=UTF-8 \
	-XX:+UseCompressedOops \
	-XX:+UseSerialGC \
	-Xms512m -Xmx4096m


all: build

build:
	$(GRADLE) clean build
	cp ./steam_appid.txt ./app
	cp ./build/libs/patch.jar ./app

run-game: build
	cd ./app && $(JRE) $(JVM_OPTS) -server \
	-cp "patch.jar:base/script/001_Tutorial.jar:base/script/000_Tutorial.jar:SongsOfSyx.jar" \
	init.MainProcess

run-launcher: build
	cd ./app && $(JRE) $(JVM_OPTS) \
	-cp "patch.jar:SongsOfSyx.jar" \
	launcher.Launcher

clean:
	$(GRADLE) clean
