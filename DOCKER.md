# Chạy CSDL bằng Docker (không cần cài MySQL)

CSDL của dự án được đóng gói trong `docker-compose.yml`. Bản đóng gói **chỉ chứa cấu trúc bảng**, không chứa dữ liệu — khi bật lên, CSDL trống và ứng dụng tự tạo dữ liệu khởi tạo từ code (`config/SecurityConfig.java`), nên mỗi máy mới đều là một dự án "sạch".

| File | Nội dung |
| --- | --- |
| `docker/mysql/init/01-schema.sql` | **17 bảng** (chỉ `CREATE TABLE`, không có `INSERT`, không mang `AUTO_INCREMENT` của dữ liệu cũ) |
| `docker-compose.yml` | Service `mysql` (image `mysql:8.0`), volume, healthcheck, mã hoá `utf8mb4` |

## 1. Thông số mặc định

| Mục | Giá trị |
| --- | --- |
| Container | `qldt-mysql` (image `mysql:8.0`) |
| Cổng trên máy | `3308` → `3306` trong container (đổi được, xem mục 5) |
| Database | `qldt` |
| User ứng dụng | `qldt_app` / `QldtApp@2026` |
| Root | `root` / `QldtRoot@2026` |
| Volume dữ liệu | `qldt_mysql_data` |
| Bảng mã | `utf8mb4` / `utf8mb4_unicode_ci`, timezone `+07:00` |

> Cổng **3308** vì 3307 thường bị project khác (ví dụ `doorlock_mysql`) chiếm.

## 2. Chạy lần đầu

```powershell
cd f:\ChoKhoa\qldt
docker compose up -d
docker compose ps          # đợi STATUS = Up ... (healthy), khoảng 20-40 giây
mvn spring-boot:run
```

Lần đầu chạy, CSDL trống → `SecurityConfig` tự tạo tài khoản khởi tạo (mật khẩu chung `123456`):

| Tài khoản | Vai trò |
| --- | --- |
| `admin` | ADMIN |
| `GV001` | LECTURER + DEAN (Trưởng khoa) |
| `GV002` … `GV005` | LECTURER |
| `SV001`, `SV002` | STUDENT |

Các bảng nghiệp vụ (đợt đăng ký, đề tài, nhóm, hội đồng, báo cáo…) **rỗng** — dữ liệu do người dùng nhập lại.

Kiểm tra:

```powershell
docker exec qldt-mysql mysql -uqldt_app -pQldtApp@2026 -N -B qldt -e "show tables; select count(*) from account;"
```

## 3. Mang sang máy khác

1. Copy thư mục dự án (gồm `src/`, `pom.xml`, `docker-compose.yml`, `docker/`).
2. Máy mới chỉ cần **JDK 21**, **Maven**, **Docker Desktop** — không cần cài MySQL.
3. Chạy:
    ```powershell
    docker compose up -d
    mvn spring-boot:run
    ```

> Nếu đã có file báo cáo trong `uploads/` của máy cũ và muốn mang theo thì copy thêm thư mục `uploads/` (tệp đính kèm không nằm trong CSDL).

## 4. Lệnh thường dùng

```powershell
docker compose up -d          # bật DB
docker compose stop           # tắt DB (giữ dữ liệu)
docker compose start          # bật lại
docker compose down           # xoá container, GIỮ volume dữ liệu
docker compose down -v        # xoá container + volume -> lần sau là CSDL trống
docker compose logs -f mysql  # xem log MySQL
```

Reset về trạng thái sạch (rồi chạy lại app để seed tài khoản):

```powershell
docker compose down -v
docker compose up -d
mvn spring-boot:run
```

## 5. Đổi cổng khi bị trùng

```powershell
$env:QLDT_DB_PORT = "3318"
docker compose up -d
```

Sau đó chạy app với URL mới (không cần sửa file):

```powershell
java -jar target\qldt-0.0.1-SNAPSHOT.jar --spring.datasource.url="jdbc:mysql://127.0.0.1:3318/qldt?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
```

(hoặc đặt biến môi trường `SPRING_DATASOURCE_URL`, hoặc sửa `spring.datasource.url` trong `src/main/resources/application.properties`.)

## 6. Cập nhật lại cấu trúc đóng gói (khi có thay đổi bảng)

Chỉ cần xuất lại **schema** từ container đang chạy:

```powershell
cmd /c "docker exec qldt-mysql mysqldump -uroot -pQldtRoot@2026 --default-character-set=utf8mb4 --no-data --skip-triggers --set-gtid-purged=OFF --skip-comments qldt > docker\mysql\init\01-schema.sql"
```

Sau đó bỏ các cụm `AUTO_INCREMENT=<số>` (nếu muốn ID bắt đầu từ 1) và kiểm tra lại file không có `INSERT`.

> Dùng `cmd /c "..."` để giữ UTF-8; nếu dùng `>` của PowerShell 5.1, file sẽ bị ghi thành UTF-16.

## 7. Ghi chú

- `scripts/start-db.ps1` / `scripts/reset-db.ps1` chỉ dùng cho cách cài MySQL trực tiếp trên Windows, không cần khi đã dùng Docker.
- Dữ liệu phát sinh nằm trong volume `qldt_mysql_data`; `docker compose down -v` sẽ xoá sạch, lần `up` sau chỉ nạp lại cấu trúc bảng.
- Đổi mật khẩu trong `docker-compose.yml` thì phải sửa `spring.datasource.password` tương ứng và `docker compose down -v` (user chỉ được tạo ở lần init đầu).
- Có thể bỏ hẳn `docker/mysql/init/01-schema.sql`: `spring.jpa.hibernate.ddl-auto=update` sẽ tự tạo bảng khi app khởi động; file schema chỉ giúp CSDL sẵn sàng ngay sau `docker compose up`.
