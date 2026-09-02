FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY . .
RUN ./mvnw clean package
EXPOSE 8080
CMD ["java", "-jar", "target/instagram-bot-0.0.1-SNAPSHOT.jar"]
