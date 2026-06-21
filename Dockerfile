FROM azul/zulu-openjdk-alpine:17 AS builder

WORKDIR /builder

RUN apk add --no-cache binutils
RUN jlink \
      --add-modules java.base,java.compiler,java.desktop,java.instrument,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.sql.rowset,java.transaction.xa,java.xml,jdk.crypto.ec,jdk.crypto.cryptoki,jdk.jfr,jdk.management,jdk.net,jdk.unsupported,jdk.zipfs \
      --no-header-files \
      --no-man-pages \
      --strip-debug \
      --compress=2 \
      --output /javaruntime

COPY build/libs/drinking-ponies.jar app.jar

RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM alpine:3.21

RUN apk add --no-cache tzdata && \
    addgroup -S app && adduser -S -G app -H -D app

ENV JAVA_HOME=/opt/java PATH="/opt/java/bin:$PATH"
COPY --from=builder /javaruntime $JAVA_HOME

WORKDIR /app
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./

USER app
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
