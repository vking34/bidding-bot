# Build stage
FROM maven:3.6.1-jdk-8-alpine AS builder
WORKDIR /app
COPY pom.xml ./pom.xml
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn -Dmaven.test.skip=true clean package

# Package stage
FROM openjdk:8u212-jdk-alpine
COPY --from=builder /app/target/auction-bot-v2-0.0.1-SNAPSHOT.jar /usr/local/lib/auction-bot.jar
EXPOSE 8990
ENTRYPOINT ["java", "-jar", "/usr/local/lib/auction-bot.jar"]
