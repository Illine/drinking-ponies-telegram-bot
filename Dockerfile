FROM azul/zulu-openjdk-alpine:17

ARG APP_HOME=/opt/drinking-ponies-telegram-bot
ARG APP_JAR=drinking-ponies-telegram-bot

ENV TZ=Europe/Moscow \
    HOME=$APP_HOME \
    JAR=$APP_JAR

WORKDIR $HOME
COPY build/libs/drinking-ponies-telegram-bot.jar $HOME/$JAR
ENTRYPOINT java "$JAVA_OPTS" -jar "$JAR"