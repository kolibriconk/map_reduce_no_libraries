FROM bitnami/java:latest
WORKDIR /my_app
COPY . .
RUN chmod +x TextCounter
WORKDIR /my_app/src/main/java
RUN javac *.java
RUN jar cfm client.jar manifest.mf *.class
WORKDIR /my_app
