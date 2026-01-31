FROM groovy:jdk17

WORKDIR /app
COPY src/main/groovy /app

CMD ["groovy", "Main.groovy"]
