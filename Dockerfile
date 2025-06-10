FROM tomcat:10.1-jdk11-corretto
RUN yum install -y unzip
RUN rm -rf /usr/local/tomcat/webapps/*
COPY target/fabflix.war /usr/local/tomcat/webapps/fabflix.war
RUN mkdir -p /usr/local/tomcat/conf/Catalina/localhost && \
    unzip /usr/local/tomcat/webapps/fabflix.war META-INF/context.xml -d /tmp/ && \
    mv /tmp/META-INF/context.xml /usr/local/tomcat/conf/Catalina/localhost/fabflix.xml && \
    rm -rf /tmp/META-INF
EXPOSE 8080
EXPOSE 8443
CMD ["catalina.sh", "run"]