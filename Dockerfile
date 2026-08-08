# Multi-stage Dockerfile for Blood Donation Card Management System
# Builder stage: uses Temurin JDK 25 to build the WAR with the Maven wrapper
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app
# Copy sources and Maven wrapper
COPY . .
# Ensure the wrapper is executable, then build (skip tests for faster builds)
RUN chmod +x mvnw || true \
    && ./mvnw -B -DskipTests clean package

# Runtime stage: lightweight Temurin JRE + Tomcat 10.1.30
FROM eclipse-temurin:25-jre AS runtime
ENV CATALINA_HOME=/usr/local/tomcat
ENV PATH=$CATALINA_HOME/bin:$PATH

# Install curl to download Tomcat, create CATALINA_HOME and clean default webapps
RUN apt-get update && apt-get install -y curl --no-install-recommends \
    && mkdir -p $CATALINA_HOME \
    && curl -fSL https://archive.apache.org/dist/tomcat/tomcat-10/v10.1.30/bin/apache-tomcat-10.1.30.tar.gz -o /tmp/tomcat.tar.gz \
    && tar -xzf /tmp/tomcat.tar.gz -C /usr/local \
    && mv /usr/local/apache-tomcat-10.1.30 $CATALINA_HOME \
    && rm /tmp/tomcat.tar.gz \
    && rm -rf $CATALINA_HOME/webapps/* $CATALINA_HOME/logs/* /var/lib/apt/lists/*

# Copy the built WAR from the builder stage into Tomcat webapps as ROOT.war
COPY --from=builder /app/target/bdcms.war $CATALINA_HOME/webapps/ROOT.war

EXPOSE 8080

# Start Tomcat in the foreground
CMD ["catalina.sh", "run"]
