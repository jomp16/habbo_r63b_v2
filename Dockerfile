FROM alpine/git AS DOWNLOAD_SOURCES
ENV APP_HOME="/opt/habbo_r63b_v2"
WORKDIR $APP_HOME
RUN git clone --depth=1 --recurse-submodules https://git.rwx.ovh/Habbo/habbo_r63b_v2.git .

FROM gradle:jdk8 AS BUILD_SERVER
ENV APP_HOME="/opt/habbo_r63b_v2"
WORKDIR $APP_HOME
COPY --from=DOWNLOAD_SOURCES $APP_HOME .
RUN gradle --no-daemon assemble
WORKDIR $APP_HOME/launcher/build/distributions
RUN tar xvf launcher-*.tbz2 -C .
RUN rm launcher-*.tbz2
RUN rm launcher-*.zip

FROM openjdk:8 AS SERVER
ENV APP_HOME="/opt/habbo_r63b_v2"
WORKDIR $APP_HOME
COPY --from=BUILD_SERVER $APP_HOME/launcher/build/distributions/ .
CMD ["java", "-XX:+UseG1GC", "-XX:+UseStringDeduplication", "-jar", "launcher-0.1.0-SNAPSHOT.jar"]