FROM azul/zulu-openjdk-alpine:17

ARG APP_HOME=/opt/drinking-ponies
ARG APP_JAR=drinking-ponies.jar

ENV HOME=$APP_HOME \
    JAR=$APP_JAR

WORKDIR $HOME
COPY build/libs/drinking-ponies.jar $HOME/$JAR
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar $JAR"]