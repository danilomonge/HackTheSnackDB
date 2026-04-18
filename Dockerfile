FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN cd ms-java-user-management-pecus && mvn clean package -DskipTests -s ../settings.xml

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/ms-java-user-management-pecus/target/ms-java-user-management-pecus-0.0.1.jar app.jar
EXPOSE 8080
CMD ["java", "-Xmx350m", "-Dspring.profiles.active=render", "-jar", "app.jar"]
