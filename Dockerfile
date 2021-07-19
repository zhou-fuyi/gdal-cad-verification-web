FROM osgeo/gdal:ubuntu-full-latest

MAINTAINER Fuyi

WORKDIR /app

COPY . .

RUN apt-get update && \
    apt-get install -y openjdk-11-jdk && \
    apt-get install -y maven && \
    mvn clean package

CMD java -jar /target/gdal-cad-verification-web-0.0.1-SNAPSHOT.jar
