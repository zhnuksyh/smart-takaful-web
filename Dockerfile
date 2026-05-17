# syntax=docker/dockerfile:1

FROM node:20-alpine AS css-build
WORKDIR /workspace

COPY package*.json ./
RUN npm ci

COPY tailwind.config.js postcss.config.js ./
COPY src/main/java ./src/main/java
COPY src/main/resources/templates ./src/main/resources/templates
COPY src/main/resources/static/css/input.css ./src/main/resources/static/css/input.css
RUN npm run build:css

FROM eclipse-temurin:17-jdk-alpine AS app-build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src ./src
COPY --from=css-build /workspace/src/main/resources/static/css/app.css ./src/main/resources/static/css/app.css
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=app-build /workspace/target/*.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=dev
ENV PORT=8080

EXPOSE 8080

USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
