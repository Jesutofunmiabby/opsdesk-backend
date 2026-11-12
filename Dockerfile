# Two stages: the first has the full JDK and Maven and builds the JAR; the
# second has only the Java runtime and runs it. The final image keeps nothing
# from the build stage except the JAR, so it is smaller and has no build tools.

# ---- Stage 1: build ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy only what is needed to download dependencies first. Docker reuses this
# layer until pom.xml or the wrapper changes, so a code change does not
# download every dependency again.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline

# Then the code. Tests run in CI and before every push, not here.
COPY src src
RUN ./mvnw -B -q -DskipTests package

# ---- Stage 2: run ----
FROM eclipse-temurin:25-jre
WORKDIR /app

# Run as an ordinary user, not root.
RUN groupadd --system opsdesk && useradd --system --gid opsdesk opsdesk
USER opsdesk

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
