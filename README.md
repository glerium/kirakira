# kirakira

QQ bot service built with Spring Boot and NapCat/OneBot.

## Local production configuration

Secrets are intentionally not versioned. On the server, create `runtime/application.properties` from `src/main/resources/application.example.properties` and supply the real MySQL values.

Build with:

```bat
mvn package
```

Run the packaged bot with:

```bat
scripts\run-production.bat
```

The launcher loads the ignored runtime file through Spring Boot's external configuration mechanism.
