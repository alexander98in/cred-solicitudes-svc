
# Dockerfile para cred-solicitud-svc
FROM eclipse-temurin:17-jdk-alpine

# Crear usuario no-root para mayor seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Directorio de trabajo
WORKDIR /app

# Copiar el JAR
COPY applications/app-service/build/libs/cred-solicitud-svc.jar app.jar

# Variables de entorno
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom"

# Exponer puerto
EXPOSE 8081

# Cambiar al usuario no-root
USER appuser

# Punto de entrada
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]