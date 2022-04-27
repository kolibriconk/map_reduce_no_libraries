FROM openjdk:17
COPY ./src/main /usr/src/main
COPY ./File.txt /usr/src/main/java
COPY ./testTocho.txt /usr/src/main/java
COPY ./veryHugeTest.txt /usr/src/main/java
WORKDIR /usr/src/main/java
RUN javac *.java
CMD ["java", "Client", "File.txt", "testTocho.txt", "veryHugeTest.txt"]