FROM bitnami/java:latest
WORKDIR /my_app
COPY src/ .
RUN chmod +x TextCounter
WORKDIR /my_app/main/java
RUN javac *.java
RUN jar cfm client.jar manifest.mf *.class
WORKDIR /my_app
