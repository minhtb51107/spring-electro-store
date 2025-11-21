# --- STAGE 1: BUILD ---
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# 1. Copy file cấu hình Gradle trước (để tận dụng cache layer của Docker)
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# 2. QUAN TRỌNG: Cấp quyền thực thi cho gradlew
RUN chmod +x ./gradlew

# 3. Copy toàn bộ source code còn lại
COPY src ./src

# 4. Chạy lệnh build
# Thêm --stacktrace để nếu lỗi thì biết tại sao
RUN ./gradlew clean build -x test --no-daemon --stacktrace

# --- STAGE 2: RUNTIME ---
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy file jar từ Stage 1
COPY --from=builder /app/build/libs/*.jar app.jar

# Tạo thư mục logs và uploads
RUN mkdir -p /app/logs /app/uploads

# Biến môi trường
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]