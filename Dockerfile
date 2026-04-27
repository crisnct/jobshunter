# =========================
# STAGE 1: Build
# =========================
FROM eclipse-temurin:25-jdk-jammy AS build

RUN apt-get update && apt-get install -y \
    maven \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# ✅ Accept build argument
ARG VITE_API_BASE_URL

# ✅ Expose it as env so Vite can read it
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# Copy entire project (multi-module)
COPY . .

# Build app
RUN mvn -pl application -am clean package -DskipTests

# =========================
# STAGE 2: Runtime
# =========================
FROM eclipse-temurin:25-jdk-jammy

WORKDIR /app

# Install OS dependencies necessary fir Chromium / Playwright
RUN apt-get update && apt-get install -y \
    ca-certificates \
    curl \
    \
    # Core Chromium deps
    libnss3 \
    libatk-bridge2.0-0 \
    libgtk-3-0 \
    libdrm2 \
    libxkbcommon0 \
    libgbm1 \
    libasound2 \
    libx11-xcb1 \
    libxshmfence1 \
    libxrandr2 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    \
    # Media / codecs (Ubuntu Jammy)
    libopus0 \
    libvpx7 \
    libx264-163 \
    \
    # GStreamer (minimum viable set)
    gstreamer1.0-plugins-base \
    gstreamer1.0-plugins-good \
    gstreamer1.0-libav \
    \
    # Fonts / shaping
    fonts-liberation \
    libharfbuzz-icu0 \
    libgraphene-1.0-0 \
    libwoff1 \
    libwebpdemux2 \
    libwebpmux3 \
    libavif13 \
    \
    # Misc
    libevent-2.1-7 \
    libatomic1 \
    libxslt1.1 \
    libenchant-2-2 \
    libsecret-1-0 \
    libhyphen0 \
    libgles2 \
    libgtk-4-1 \
    gstreamer1.0-gl \
    gstreamer1.0-plugins-bad \
    libmanette-0.2-0 \
    \
    && rm -rf /var/lib/apt/lists/*

# Copy application jar
COPY --from=build /app/application/target/jobshunter-1.0.0.jar /app/app.jar

# Environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENV TZ=UTC

EXPOSE 8443

# Run the app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
