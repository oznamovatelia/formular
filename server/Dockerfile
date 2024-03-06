FROM docker.io/library/openjdk:11
EXPOSE 8080

VOLUME /tmp

WORKDIR /opt/app

COPY target/classes/link-aes.p12 /opt/app/BOOT-INF/classes/link-aes.p12
COPY target/classes/pgp_public.asc /opt/app/BOOT-INF/classes/pgp_public.asc

ARG JAR_FILE
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","/opt/app/app.jar", "--spring.config.location=classpath:/,optional:file:///config/security/security.properties,optional:file:///config/security/security.yaml,optional:file:///config/spring/application.properties,optional:file:///config/spring/application.yaml"]





