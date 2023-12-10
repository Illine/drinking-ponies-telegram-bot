FROM azul/zulu-openjdk-alpine:17

ARG APP_HOME=/opt/drinking-ponies
ARG APP_JAR=drinking-ponies

ENV TZ=Europe/Moscow \
    HOME=$APP_HOME \
    JAR=$APP_JAR

WORKDIR $HOME
COPY build/libs/drinking-ponies-telegram-bot.jar $HOME/$JAR
ENTRYPOINT java $JAVA_OPTS -jar $JAR