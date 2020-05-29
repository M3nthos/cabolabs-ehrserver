FROM java
ENV DEBIAN_FRONTEND noninteractive
RUN echo "deb [check-valid-until=no] http://cdn-fastly.deb.debian.org/debian jessie main" > /etc/apt/sources.list.d/jessie.list
RUN echo "deb [check-valid-until=no] http://archive.debian.org/debian jessie-backports main" > /etc/apt/sources.list.d/jessie-backports.list
RUN sed -i '/deb http:\/\/deb.debian.org\/debian jessie-updates main/d' /etc/apt/sources.list
RUN apt-get -o Acquire::Check-Valid-Until=false update
RUN apt-get install -y mysql-server

# install grails
RUN curl -L https://github.com/grails/grails-core/releases/download/v3.3.10/grails-3.3.10.zip  -o /grails.zip
RUN unzip /grails.zip -d /opt
ADD . /app

WORKDIR /app

ENV GRAILS_HOME /opt/grails-3.3.10
ENV PATH $GRAILS_HOME/bin:$PATH

EXPOSE 8090
RUN grails dependency-report
#RUN ["/bin/bash", "-c", "chmod +x /app/docker-entrypoint.sh"]
COPY ./docker-entrypoint.sh ./
RUN chmod +x /app/docker-entrypoint.sh

# Define default command.
ENTRYPOINT ["sh", "/app/docker-entrypoint.sh"]
#ENTRYPOINT ["/app/docker-entrypoint.sh"]
CMD ["grails", "-Dserver.port=8090", "run-app"]

