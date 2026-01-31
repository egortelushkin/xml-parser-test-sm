FROM groovy:5.0-jdk17

WORKDIR /app
COPY src/main/groovy/XmlDbUpdater.groovy .

CMD ["groovy", "XmlDbUpdater.groovy"]
