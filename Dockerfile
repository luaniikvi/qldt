# Giai đoạn 1: Build file JAR từ mã nguồn bằng Maven và JDK 21
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy file cấu hình maven và mã nguồn
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw

# Tải trước các thư viện phụ thuộc để tận dụng Docker cache
RUN ./mvnw dependency:go-offline -B || true

# Copy toàn bộ mã nguồn và build đóng gói bỏ qua chạy test để deploy nhanh
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Giai đoạn 2: Tạo image chạy nhẹ gọn (chỉ cần JRE 21)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Tạo thư mục uploads cho tệp đính kèm
RUN mkdir -p /app/uploads

# Copy file jar từ giai đoạn builder sang
COPY --from=builder /app/target/*.jar app.jar

# Cổng mặc định
EXPOSE 8080

# Chạy ứng dụng Spring Boot và nhận PORT từ môi trường (Render tự cấp biến PORT)
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
