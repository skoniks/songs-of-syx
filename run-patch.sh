cp ./steam_appid.txt ./app/
cp ./build/libs/patch.jar ./app/
cd ./app/
./jre/bin/java \
  -Dfml.earlyprogresswindow=false \
  -Dfile.encoding=UTF-8 \
  -Xms512m -Xmx4096m \
  -XstartOnFirstThread \
  -XX:+UseCompressedOops \
  -XX:+UseSerialGC \
  -server \
  -cp "patch.jar:base/script/001_Tutorial.jar:base/script/000_Tutorial.jar:SongsOfSyx.jar" \
  init.MainProcess