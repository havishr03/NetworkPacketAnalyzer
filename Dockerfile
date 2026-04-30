# Use official maven image to build the project
FROM maven:3.8.6-eclipse-temurin-11 AS build
WORKDIR /app
# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src
# Package the application as a .war file
RUN mvn clean package

# Use official Tomcat image for runtime
FROM tomcat:9.0-jdk11-corretto
# Remove default Tomcat applications
RUN rm -rf /usr/local/tomcat/webapps/*
# Copy the built WAR file from the build stage as ROOT.war so it is served at the root path (/)
COPY --from=build /app/target/ROOT.war /usr/local/tomcat/webapps/ROOT.war

# Expose port 8080 (Render's expected web port)
EXPOSE 8080

CMD ["catalina.sh", "run"]
