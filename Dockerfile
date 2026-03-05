ARG JDK_VERSION=21

FROM maven:3.9.6-eclipse-temurin-${JDK_VERSION} AS builder

WORKDIR /app

COPY pom.xml mvnw* ./
COPY .mvn .mvn
RUN mvn -B -e -DskipTests dependency:go-offline

COPY . /app
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:${JDK_VERSION}-jdk AS runtime
WORKDIR /app

COPY --from=builder /app/target/*.jar /app/app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XshowSettings:vm -Dfile.encoding=UTF-8"

EXPOSE 16311

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
