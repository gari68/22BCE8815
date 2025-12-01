# webhook-solver

Spring Boot app that:
- On startup calls generateWebhook to get a webhook URL and an accessToken.
- Reads final SQL from `src/main/resources/sql/question1.sql`.
- Sends the SQL to the returned webhook URL with Authorization header.

How to use:
1. Extract the zip.
2. (Optional) Edit `src/main/resources/application.properties` to set your name/regNo/email.
3. Use one of:
   - Build locally with Maven: `mvn -U clean package` then `java -jar target/webhook-solver-0.0.1-SNAPSHOT.jar`
   - Push to GitHub with the included workflow to build on GitHub Actions (see `.github/workflows/build-and-release.yml`).
