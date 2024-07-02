FROM cs-hub.imgo.tv/devops/spinnaker/clouddriver-base:v0.0.1

MAINTAINER weizhi@mgtv.com
COPY clouddriver-web/build/install/clouddriver /opt/clouddriver

USER root
RUN mkdir -p /var/log/spinnaker && \
    chmod 755 -R /var/log/spinnaker && \
    chown -R spinnaker:nogroup /var/log/spinnaker

USER spinnaker

CMD ["/opt/clouddriver/bin/clouddriver"]
