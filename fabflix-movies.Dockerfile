# ---- Build Stage ----
# This stage uses a Maven image to reliably build your .war file from source.
FROM maven:3.8.5-openjdk-17 AS build

# Set the working directory for the build
WORKDIR /app

# Copy only the pom.xml first to use Docker's cache efficiently
COPY pom.xml .

# Copy your entire source code directory
COPY src ./src

# Run the Maven build command. It uses the 'movies' profile from your pom.xml
# to correctly package the application, including login.jsp.
RUN mvn clean package -P movies -DskipTests


# ---- Final Stage ----
# This stage creates the final, lean image using a standard Tomcat base.
FROM tomcat:10.0-jdk17-corretto

# It's good practice to remove the default application that comes with Tomcat.
RUN rm -rf /usr/local/tomcat/webapps/ROOT

# Copy the 'fabflix-movies.war' file that was just built in the stage above
# into Tomcat's webapps directory. Renaming it to ROOT.war makes it the
# default application, which is what your Ingress expects.
COPY --from=build /app/target/fabflix-movies.war /usr/local/tomcat/webapps/ROOT.war

# Expose the port Tomcat listens on.
EXPOSE 8080

# The command to start the Tomcat server when the container launches.
CMD ["catalina.sh", "run"]