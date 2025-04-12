FROM gradle:7.3.3-jdk17 AS builder

WORKDIR /build

COPY . /build

RUN ./gradlew clean build -x test


LABEL maintainer="juintination"

LABEL email="juintin@kakao.com"


FROM khipu/openjdk17-alpine

WORKDIR /app

COPY --from=builder /build/build/libs/*-SNAPSHOT.jar ./app.jar

EXPOSE 8080

ENTRYPOINT [ "java" ]

CMD [ "-jar", "app.jar" ]
