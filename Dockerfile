#
#FROM maven:3.8.2-jdk-11 AS build-env
#WORKDIR /app
#
#LABEL maintainer="krlsedu@gmail.com"
#
#COPY pom.xml ./
#RUN mvn dependency:go-offline
#
#COPY . ./
#RUN mvn package -DskipTests

FROM adoptopenjdk/openjdk11:alpine-jre
WORKDIR /opt/app

ARG JAR_FILE=target/csctracker-notify-sync.jar

COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-Dfile.encoding=UTF-8","-jar","app.jar"]