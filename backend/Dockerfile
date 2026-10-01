# ==========================================
# ЭТАП 1: Сборка (Build)
# ==========================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Копируем pom.xml и скачиваем зависимости (кэширование слоев)
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем исходники и собираем jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==========================================
# ЭТАП 2: Запуск (Run)
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Создаем группу, пользователя И папку хранилища
RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && mkdir -p /app/storage \
    && chown -R appuser:appgroup /app

# Копируем готовый jar
COPY --from=build /app/target/*.jar app.jar

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]