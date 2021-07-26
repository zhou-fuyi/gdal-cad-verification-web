FROM osgeo/gdal:ubuntu-small-latest

MAINTAINER Fuyi

RUN apt-get update && \
    apt install build-essential -y && \
	apt-get install wget -y && \
	wget http://ftp.gnu.org/gnu/libredwg/libredwg-0.12.4.tar.gz && \
    tar -zxvf libredwg-0.12.4.tar.gz && \
    cd libredwg-0.12.4/ && \
    ./configure && \
    make && \
    make check && \
    make install && \
    make clean && \
    ldconfig

WORKDIR /app

COPY . .

RUN apt-get install -y openjdk-11-jdk && \
    apt-get install -y maven && \
    mvn clean package

CMD java -jar target/gdal-cad-verification-web-0.0.1-SNAPSHOT.jar