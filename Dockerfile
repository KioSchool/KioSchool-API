FROM amazoncorretto:17
ARG JAR_FILE=build/libs/KioSchool-API.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=prod","-jar","/app.jar"]