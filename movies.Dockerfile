# Use the same Tomcat base image as before
FROM tomcat:10.0-jdk11-temurin

# Remove the default ROOT web application from the base image
RUN rm -rf /usr/local/tomcat/webapps/ROOT

# Copy the specialized 'fabflix-movies.war' file (built with the 'movies' profile)
# and deploy it as the ROOT application.
COPY target/fabflix-movies.war /usr/local/tomcat/webapps/ROOT.war
