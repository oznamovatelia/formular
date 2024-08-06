# Use the official Ubuntu 22.04 base image
FROM ubuntu:22.04
ARG GITHUB_USERNAME
ARG GITHUB_ACCESSTOKEN

# # Print the GitHub username and access token and exit
# RUN echo "GitHub Username: $GITHUB_USERNAME" && \
#     echo "GitHub Access Token: $GITHUB_ACCESSTOKEN" && \
#     exit 1

# Update and install necessary packages
RUN apt-get update && \
    apt-get -y upgrade && \
    apt-get -y install sudo git maven openjdk-11-jdk

# Clone the repository
RUN mkdir -p /opt/uooform/config && \
    cd /tmp && \
    echo https://$GITHUB_USERNAME:$GITHUB_ACCESSTOKEN@github.com/oznamovatelia/formular.git && \
    git clone https://$GITHUB_USERNAME:$GITHUB_ACCESSTOKEN@github.com/oznamovatelia/formular.git && \
    cd /tmp/formular/ && \
    git checkout release/1.0.4

# # Overwrite application.yml file with local version
# COPY application.yml /tmp/formular/server/src/main/resources/application.yml
# COPY server-pom.xml /tmp/formular/server/pom.xml

# Build the application
RUN cd /tmp/formular/server/ && \
    mvn clean package && \
    cp /tmp/formular/server/target/server-1.0.4-exec.jar /opt/uooform/uoo-formular.jar && \
    cp /tmp/formular/server/target/server-1.0.4-exec.jar /opt/uooform/tormail.jar
    # && \
    # cp /tmp/formular/server/src/main/resources/application.yml /opt/uooform/application.yml
    # cp /tmp/formular/server/src/main/resources/application.yml /uoo-formular/application.yml

# # Copy the wrapper script and make it executable
# COPY uooform-wrapper.sh /opt/uooform/uooform-wrapper.sh
# RUN chmod +x /opt/uooform/uooform-wrapper.sh

# Set the working directory
WORKDIR /opt/uooform

EXPOSE 9000 9001
CMD ["java", "-jar", "uoo-formular.jar"]
