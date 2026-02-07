cd ./resources/
cp ../steam_appid.txt .
./jre/bin/java \
  -Dfml.earlyprogresswindow=false \
  -Dfile.encoding=UTF-8 \
  -Xms512m -Xmx4096m \
  -XstartOnFirstThread \
  -XX:+UseCompressedOops \
  -XX:+UseSerialGC \
  -server \
  -cp "base/script/001_Tutorial.jar:base/script/000_Tutorial.jar:SongsOfSyx.jar" \
  init.MainProcess