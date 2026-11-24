FROM eclipse-temurin:21-jdk-jammy

# Instala Maven e dependencias nativas para GUI (GTK3, OpenGL, X11, ALSA e fontes)
RUN apt-get update && apt-get install -y --no-install-recommends \
    maven \
    libgl1 \
    libgtk-3-0 \
    libasound2 \
    libxtst6 \
    libxi6 \
    libxrender1 \
    libfontconfig1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Aproveita o cache de camadas do Docker para baixar dependencias
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o codigo-fonte e compila
COPY src ./src
RUN mvn clean compile -DskipTests

CMD ["mvn", "javafx:run"]
