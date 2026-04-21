FROM gradle:jdk21-corretto AS build

WORKDIR /app
COPY ../.. .
RUN gradle :core:bootJar --no-daemon
RUN gradle :boot:shadowJar --no-daemon

FROM amazoncorretto:21

COPY --from=build /app/core/build/libs/core.jar /core.jar
COPY --from=build /app/boot/build/libs/boot.jar /boot.jar

ENV BOOT_PATH="/boot.jar"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/core.jar"]
