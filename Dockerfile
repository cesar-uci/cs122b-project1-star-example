# Start with a base image containing a compatible Tomcat and Java version.
# Your pom.xml uses Jakarta Servlet API 5.0, which requires Tomcat 10.
# Your code is compiled for Java 1.8, which will run on JDK 11.
FROM tomcat:10.0-jdk11-temurin

# Set a label for the image maintainer (optional, good practice)
LABEL maintainer="Cesar Gonzalez (21385718)"

# Good practice to remove the default webapps to keep the image clean.
RUN rm -rf /usr/local/tomcat/webapps/*

# Your pom.xml <finalName> is `cs122b-project1-star-example`.
# This command copies the .war file built by Maven into the `target` directory.
# It renames the file to `ROOT.war` inside the container, which makes your
# application accessible at the root URL (e.g., http://<ip>:8080/).
COPY target/cs122b-project1-star-example.war /usr/local/tomcat/webapps/ROOT.war

# Expose the port that Tomcat runs on from inside the container.
EXPOSE 8080

# This is the command that runs when the container starts.
# It starts the Tomcat server in the foreground.
CMD ["catalina.sh", "run"]