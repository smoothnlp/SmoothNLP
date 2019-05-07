# Use an offcial Java runtime as a parent image
FROM openjdk:8

# copy smoothnlp-0.2-exec.jar to directory
COPY . /smoothnlp

# install cmake
RUN apt-get update && apt-get install -y git gcc build-essential

# install XGBoost library
RUN git clone --recursive https://github.com/dmlc/xgboost
RUN cd xgboost; make -j4

# Specifiy WorkDirectory
WORKDIR /smoothnlp

# docker port
EXPOSE 8080

# cmd
CMD java -jar smoothnlp-0.2-exec.jar
