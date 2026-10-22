# NOTE.md — Ghi chú triển khai dự án QLĐT

File này dùng để giải thích code/thiết kế thay cho việc comment trong code (theo `documentation/RULE.md` mục 2).

## 1. Trạng thái dự án

**Bước 0 — khảo sát (đã xong)**: ban đầu dự án chỉ có `QldtApplication.java` + `application.properties`, chưa có templates/controller/service, chưa có Git. Công nghệ theo `pom.xml`: Spring Boot 4.1.1, Java 21, Maven; Web MVC, Thymeleaf, Validation, Spring Security, Spring Data JPA, MySQL connector, thymeleaf-extras-springsecurity6. Môi trường: Java 21.0.11 + Maven 3.9.16 → build được.

**Hiện tại**: đã có module **Đợt đăng ký** (in-memory, mục 6) và module **Tài khoản + Spring Security** (in-memory, mục 11). Vẫn **chưa** có JPA/MySQL và chưa khởi tạo Git.

## 2. Nguồn tài liệu tham khảo

| Tài liệu | Nội dung sử dụng |
|---|---|
| `documentation/Đồ án cuối kỳ.md` | Đề bài: hệ thống quản lý đề tài SV (đợt đăng ký, đề tài theo bộ môn, nhóm ≤ 3 SV, GVHD/GVPB, hội đồng phản biện 3–5 GV, chấm điểm, công bố kết quả, tài khoản/phân quyền, thông báo) |
| `documentation/Các lỗi sai thường gặp.md` | Danh sách lỗi phải tránh (business logic ở Controller, `findAll()` + filter trong RAM, service "nửa mùa", trả String lỗi, thiếu `@Transactional`, double-booking) |
| `documentation/RULE.md` | Quy tắc: không comment trong code, ghi chú vào NOTE.md, ghi nguồn, giải thích annotation, hỏi khi chưa rõ, code ngắn gọn, mỗi bước phải xác nhận |
| `documentation/LabSlide/Lab 1 - Controller and Thymeleaf.pdf` | Trang 1–2: cấu trúc package `model`, `dto`, `service`, `controller` + `templates`; trang 3–4: Model vs DTO và Bean Validation; **trang 5–6: `@Service` giữ `private final List<Todo> store = new ArrayList<>()` và `private final AtomicLong sequence = new AtomicLong(1)` để lưu dữ liệu trong RAM**; trang 7–8: `@Controller` + form binding; trang 9–10: template Thymeleaf |
| `documentation/LabSlide/Lab 2 - RestController.pdf` | Trang 1: nhấn mạnh `TodoService` là Spring bean, trang HTML `/todos` và API `/api/v1/todos` **đọc/ghi trên cùng một instance ArrayList**; trang 3: quy ước URL `/api/v1/todos`; trang 4–5: `@RestController` CRUD; trang 7: domain exception (`TodoNotFoundException`) + `@ExceptionHandler` trả 404 |
| `documentation/LabSlide/Lab 4 - Spring Security.pdf` | Trang 1–3: mục tiêu (authentication, role-based authorization, CSRF), cấu trúc `CustomUserDetailsService` + `config/SecurityConfig`; trang 6: `CustomUserDetailsService.loadUserByUsername` và `roles(...)` tự thêm tiền tố `ROLE_`; trang 7: `PasswordEncoder` (BCrypt), `@EnableMethodSecurity`, `SecurityFilterChain`; trang 8: `authorizeHttpRequests` + `formLogin`; trang 9–10: `@PreAuthorize`; trang 11–12: `sec:authorize` để ẩn/hiện nút |
| `documentation/LabSlide/Lab 3..6`, `documentation/sildeLyThuyet/Chapter 04..10`, `Deployment.pdf` | Lộ trình tham khảo tiếp theo: JPA → Security → AOP → Email → Deploy |
| https://docs.spring.io/spring-boot/4.1.1/reference/web/servlet.html | Tài liệu Spring MVC/Thymeleaf (theo `documentation/HELP.md`) |
| https://docs.spring.io/spring-boot/4.1.1/reference/web/spring-security.html | Tài liệu Spring Security |

> Ghi chú: nội dung PDF được trích bằng `pypdf` 6.21.1 (cài bằng pip, **không** thêm vào `pom.xml`) để đọc `Lab 1`, `Lab 2`. Text trích xuất tạm nằm ở `target/lab1.txt`, `target/lab2.txt` (thư mục `target` không commit).

## 3. Quy ước sẽ áp dụng khi code

- Kiến trúc MVC Spring Boot: `model` → `repository` → `service` → `controller`; `templates` cho Thymeleaf.
- Toàn bộ nghiệp vụ (validate chéo, kiểm tra ràng buộc) đặt ở tầng `service`; controller chỉ nhận request và trả view/response.
- Không dùng `findAll()` rồi lọc trong RAM; viết query ở `repository`.
- Lỗi nghiệp vụ biểu diễn bằng domain exception (không trả về String thông báo).
- Các luồng ghi dữ liệu quan trọng sẽ dùng `@Transactional`.
- Không giữ lại code mẫu/todo của bài lab.
- Không comment trong code; mọi giải thích (kể cả ý nghĩa annotation) ghi tại đây.

## 4. Nhật ký giải thích annotation

| Annotation | Tác dụng | Nếu không dùng thì phải code thế nào |
|---|---|---|
| `@SpringBootApplication` (trong `QldtApplication`) | Gộp 3 annotation: `@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan`; khởi động auto-configuration và quét component trong package `project.qldt` | Phải khai báo riêng 3 annotation trên, hoặc cấu hình thủ công toàn bộ bean (DispatcherServlet, ViewResolver, DataSource...) bằng Java Config/XML |
| `@Service` (trên `RegistrationPeriodService`) | Đánh dấu class là bean tầng nghiệp vụ để `@ComponentScan` tạo **một** instance duy nhất (singleton) trong IoC container; nhờ vậy trang Thymeleaf và API dùng chung đúng một `ArrayList` (đúng ý Lab 2 trang 1) | Không dùng: phải tự `@Bean public RegistrationPeriodService registrationPeriodService() {...}` trong class `@Configuration`, hoặc `new RegistrationPeriodService()` thủ công → mỗi lần `new` sẽ có store riêng, HTML và API không còn chia sẻ dữ liệu |
| `@Controller` (trên `RegistrationPeriodController`, `HomeController`) | Đánh dấu bean xử lý HTTP cho view HTML; giá trị `String` trả về được hiểu là **tên template** Thymeleaf, không phải nội dung JSON | Không dùng: phải implement `Controller` interface hoặc khai báo bean + `HandlerMapping` thủ công; nếu dùng `@RestController` thì chuỗi trả về sẽ được ghi thẳng ra body, không render HTML |
| `@RestController` (trên `RegistrationPeriodRestController`) | Gộp `@Controller` + `@ResponseBody`: giá trị trả về (`List`, object, `ResponseEntity`) được chuyển sang JSON bằng Jackson | Không dùng: phải thêm `@ResponseBody` trên từng method (hoặc trên class) và giữ `@Controller` |
| `@RequestMapping` (class-level) | Gắn tiền tố URL chung: `/periods` cho HTML, `/api/v1/periods` cho API | Không dùng: phải ghi đầy đủ đường dẫn trong từng `@GetMapping`/`@PostMapping` |
| `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` | Rút gọn của `@RequestMapping(method = RequestMethod.X)`, ánh xạ HTTP method + path tới method Java | Không dùng: phải viết `@RequestMapping(value="/periods", method=RequestMethod.GET)` cho từng method |
| `@PathVariable` | Lấy phần biến trong URL (ví dụ `{id}` của `/periods/{id}/edit`) và gán vào tham số | Không dùng: phải đọc `HttpServletRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)` rồi parse thủ công |
| `@ModelAttribute("period")` | Bind dữ liệu form vào DTO và đưa DTO vào `Model` với tên `period` (khớp `th:object="${period}"`) | Không dùng: phải khai báo tham số `RegistrationPeriodRequest` (Spring vẫn bind) và tự `model.addAttribute("period", request)` |
| `@RequestBody` (trên tham số của API) | Đọc body JSON của request và deserialize vào DTO | Không dùng: phải tự đọc `InputStream` và gọi `ObjectMapper.readValue(...)` |
| `@Valid` | Kích hoạt Bean Validation trên DTO (chạy `@NotBlank`, `@NotNull`, `@Size`) trước khi vào thân method | Không dùng: phải tự tạo `Validator` và gọi `validator.validate(dto)` trong từng method |
| `BindingResult` (tham số ngay sau `@Valid`) | Giữ kết quả validate để controller kiểm tra `hasErrors()` và render lại form kèm thông báo; nếu thiếu, Spring ném `MethodArgumentNotValidException` | Không dùng: phải bắt `MethodArgumentNotValidException` bằng `@ExceptionHandler` để hiển thị lỗi |
| `@ExceptionHandler` | Ánh xạ exception cụ thể sang response mong muốn: trong API trả `404`/`400` JSON, trong MVC redirect về danh sách kèm flash message | Không dùng: phải dùng `try/catch` tại từng method (trùng lặp), hoặc cấu hình `HandlerExceptionResolver` thủ công |
| `@DateTimeFormat(iso = ...)` | Chỉ định định dạng chuyển `String` ⇄ `LocalDateTime`/`LocalDate` khi bind form (`datetime-local` → `yyyy-MM-dd'T'HH:mm`, `date` → `yyyy-MM-dd`) | Không dùng: phải tự viết `Converter<String, LocalDateTime>` và đăng ký `FormatterRegistry`, hoặc thêm `@InitBinder` trong controller |
| `@NotBlank` | Báo lỗi khi chuỗi `null`, rỗng hoặc chỉ có khoảng trắng | Không dùng: phải tự `if (name == null || name.isBlank())` và tự thêm lỗi vào `BindingResult` |
| `@NotNull` | Báo lỗi khi giá trị `null` (dùng cho enum và các mốc thời gian) | Không dùng: phải tự kiểm tra `null` trong service/controller |
| `@Size(max = 150)` | Giới hạn độ dài chuỗi | Không dùng: phải tự kiểm tra `name.length()` |
| `@Configuration` (trên `SecurityConfig`) | Đánh dấu class chứa định nghĩa bean (`@Bean`) | Không dùng: phải khai báo trong XML `<beans>` hoặc dùng `@Component` (kém chuẩn cho class cấu hình) |
| `@Bean` (method `securityFilterChain`) | Đăng ký object do method trả về làm bean trong IoC container | Không dùng: phải `new` thủ công ở nơi sử dụng, mất khả năng inject và không được Spring quản lý vòng đời |
| `@SpringBootTest` (test `contextLoads`) | Khởi động toàn bộ ApplicationContext để kiểm tra cấu hình có load được không | Không dùng: phải tự `new SpringApplicationBuilder(...).run()` trong test |
| `@EnableMethodSecurity` (trên `SecurityConfig`) | Bật bảo mật ở mức method, để `@PreAuthorize`/`@PostAuthorize` có hiệu lực | Không dùng: `@PreAuthorize` bị bỏ qua hoàn toàn, chỉ còn kiểm tra ở `SecurityFilterChain` (mất lớp bảo vệ thứ 2) |
| `@PreAuthorize("hasRole('DEAN')")` | Kiểm tra quyền **trước khi** method controller chạy; `hasRole('DEAN')` khớp authority `ROLE_DEAN` | Không dùng: phải `if (!request.isUserInRole("ROLE_DEAN")) throw new AccessDeniedException(...)` ở đầu từng method, hoặc chỉ dựa vào URL matcher (nếu cấu hình URL sai thì lọt quyền) |
| `@Override` (`loadUserByUsername`) | Báo cho compiler biết method ghi đè method của interface `UserDetailsService`; sai chữ ký sẽ báo lỗi biên dịch | Không dùng: code vẫn chạy nếu chữ ký đúng, nhưng nếu gõ sai tên/tham số sẽ thành method mới và Spring Security không gọi tới (login luôn thất bại mà không báo lỗi) |
| `@Getter` (Lombok) | Sinh getter cho các field của class lúc biên dịch (annotation processor), không sinh lại getter của lớp cha | Không dùng: phải viết tay `getId()`, `getUsername()`, `getPassword()`... cho từng field như `RegistrationPeriod` hiện tại |
| `@Setter` (Lombok) | Sinh setter cho các field; cần cho form binding của Spring MVC (`th:field`) và cho Jackson khi đọc JSON | Không dùng: phải viết tay từng setter; thiếu setter thì form/JSON không bind được giá trị |

### 4.1. Khái niệm Spring Security dùng ở Bước 1 (không phải annotation)

| Thành phần | Tác dụng | Nếu không dùng thì phải code thế nào |
|---|---|---|
| `SecurityFilterChain` | Chuỗi filter quyết định request nào được vào, redirect login, xử lý CSRF, logout | Phải tự viết servlet filter + `web.xml`/`FilterRegistrationBean` để kiểm tra session, đăng nhập, CSRF |
| `UserDetailsService` (`CustomUserDetailsService`) | Cầu nối giữa kho tài khoản (in-memory `UserService`) và Spring Security khi đăng nhập | Phải tự viết `AuthenticationProvider` gọi `UserService` và so khớp mật khẩu bằng tay |
| `PasswordEncoder` + `BCryptPasswordEncoder` | Băm mật khẩu một chiều có salt; `matches()` để so khớp khi login | Phải tự dùng thư viện BCrypt, tự lưu salt, tự so sánh — dễ sai và không đồng bộ với `UserDetailsService` |
| `CommandLineRunner` (`initUsers`) | Chạy code seed dữ liệu ngay sau khi ApplicationContext khởi động xong | Phải seed trong constructor của service (dễ lỗi thứ tự khởi tạo) hoặc `@PostConstruct` |
| `sec:authorize`, `sec:authentication` (Thymeleaf) | Ẩn/hiện phần HTML theo quyền và in tên người đăng nhập | Phải truyền `Authentication` vào model từ controller rồi dùng `th:if` thủ công ở mọi trang |
| `LogoutSuccessHandler` (lambda trong `.logout(...)`) | Xử lý sau khi logout xong; ở đây redirect về `/home?logout` | Phải tự viết `AuthenticationSuccessHandler`/filter lắng nghe sự kiện logout, hoặc để mặc định (dễ ra 204 hoặc về `/login?logout`) |
| `templates/error.html` (quy ước của Spring Boot, không phải annotation) | `BasicErrorController` tự tìm view tên `error` để render khi có lỗi; tránh trang whitelabel | Phải tự viết `ErrorController` hoặc `@ExceptionHandler` cho mọi exception để trả view lỗi |

## 5. Cách lưu dữ liệu — giống Lab 1 & Lab 2 (in-memory bằng `ArrayList`)

Theo Lab 1 (trang 5–6) và Lab 2 (trang 1), dữ liệu **chưa** dùng JPA/MySQL mà lưu trong RAM của ứng dụng:

- `RegistrationPeriodService` là một `@Service` (singleton) giữ:
  - `private final List<RegistrationPeriod> store = new ArrayList<>();` — nơi chứa dữ liệu.
  - `private final AtomicLong sequence = new AtomicLong(1);` — sinh id tự tăng, thread-safe.
- `findAll()` trả `new ArrayList<>(store)` — bản sao, tránh bên ngoài sửa trực tiếp danh sách.
- `findById()` dùng `stream().filter(...).findFirst().orElseThrow(...)` — ném `RegistrationPeriodNotFoundException`.
- `create()` lấy id bằng `sequence.getAndIncrement()`; `update()` sửa trên object đang có trong `store`; `deleteById()` tìm rồi `store.remove(...)`.
- **Cùng một instance service** được inject vào cả `RegistrationPeriodController` (HTML, `/periods`) và `RegistrationPeriodRestController` (JSON, `/api/v1/periods`) → tạo bằng API thì thấy trên trang Thymeleaf và ngược lại, đúng như Lab 2 trang 1.
- `final` trên `store` và `sequence` để không thể gán lại tham chiếu sau khi khởi tạo (Lab 1 trang 5).
- Hệ quả cần biết: dữ liệu **mất khi restart** ứng dụng (Lab 1 trang 11). Đây là bước tạm, sẽ thay bằng JPA + MySQL ở bước sau.

## 6. Module 1 — Quản lý đợt đăng ký

### 6.1. File đã tạo

| File | Vai trò |
|---|---|
| `model/PeriodType.java` | Enum loại đợt: `COURSE` (Môn học), `RESEARCH` (NCKH), `SPECIALIZED_THESIS` (TLCN), `GRADUATION_THESIS` (KLTN); có `requiresReviewerScoreDeadline()`, `requiresCouncilReportDate()` |
| `model/RegistrationPeriod.java` | Model lưu trong RAM: `id`, `name`, `type`, 4 mốc thời gian GV/SV đăng ký, `reviewerScoreDeadline`, `councilReportDate` |
| `dto/RegistrationPeriodRequest.java` | DTO nhận dữ liệu form/JSON (không có `id`) + Bean Validation + `from(RegistrationPeriod)` |
| `service/RegistrationPeriodService.java` | Toàn bộ nghiệp vụ + `ArrayList` store + `AtomicLong` |
| `exception/RegistrationPeriodNotFoundException.java` | Lỗi không tìm thấy (404) |
| `exception/InvalidRegistrationPeriodException.java` | Lỗi vi phạm nghiệp vụ (400) |
| `controller/RegistrationPeriodController.java` | `@Controller` cho HTML tại `/periods` |
| `controller/api/v1/RegistrationPeriodRestController.java` | `@RestController` cho JSON tại `/api/v1/periods` |
| `controller/HomeController.java` | Trang `/` liệt kê link module |
| `config/SecurityConfig.java` | Cấu hình tạm cho phép tất cả request (xem mục 7) |
| `templates/home.html`, `templates/periods/list.html`, `templates/periods/form.html` | Giao diện Thymeleaf |

### 6.2. Endpoint

| Method | URL | Việc làm |
|---|---|---|
| GET | `/periods` | Danh sách (HTML) |
| GET | `/periods/new` | Form tạo |
| POST | `/periods` | Tạo mới |
| GET | `/periods/{id}/edit` | Form sửa |
| POST | `/periods/{id}` | Cập nhật |
| POST | `/periods/{id}/delete` | Xóa (form HTML không gửi được DELETE) |
| GET | `/api/v1/periods` | Danh sách (JSON) |
| GET | `/api/v1/periods/{id}` | Một đợt (JSON, 404 nếu không có) |
| POST | `/api/v1/periods` | Tạo mới (JSON) |
| PUT | `/api/v1/periods/{id}` | Cập nhật (JSON) |
| DELETE | `/api/v1/periods/{id}` | Xóa (JSON) |

### 6.3. Nghiệp vụ kiểm tra (đặt trong `RegistrationPeriodService.validate`, không đặt ở Controller)

1. Thời điểm kết thúc GV đăng ký phải sau thời điểm bắt đầu.
2. Thời điểm SV đăng ký phải bắt đầu **sau khi** GV đăng ký kết thúc (giai đoạn 1 công bố đề tài → giai đoạn 2 SV đăng ký).
3. Thời điểm kết thúc SV đăng ký phải sau thời điểm bắt đầu.
4. Nếu loại là **TLCN/KLTN**: bắt buộc có `reviewerScoreDeadline`, và hạn này không được trước ngày SV đăng ký kết thúc.
5. Nếu loại là **KLTN**: bắt buộc có `councilReportDate`, và ngày này phải sau `reviewerScoreDeadline`.
6. Nếu loại **không** yêu cầu (Môn học, NCKH) thì `reviewerScoreDeadline`/`councilReportDate` được tự động đặt `null` khi lưu (`apply`).
7. Lỗi nghiệp vụ ném `InvalidRegistrationPeriodException` (không trả String từ service); controller chỉ hiển thị lại form kèm thông báo.

### 6.4. Vì sao vừa có Bean Validation ở DTO vừa có `validate()` ở Service

Hai lớp kiểm tra này **không trùng nhau**, chúng trả lời 2 câu hỏi khác nhau:

| | Bean Validation trên DTO (`@NotBlank`, `@NotNull`, `@Size`) | `validate()` trong Service |
|---|---|---|
| Kiểm tra cái gì | **Từng field riêng lẻ**: có bị bỏ trống không, dài bao nhiêu | **Quan hệ giữa nhiều field** và **quy tắc nghiệp vụ**: thứ tự thời gian, field nào bắt buộc theo `type` |
| Ví dụ trong code | `name` không rỗng, `type` khác null, 4 mốc thời gian khác null | `teacherRegisterEnd > teacherRegisterStart`; `studentRegisterStart >= teacherRegisterEnd`; TLCN/KLTN phải có `reviewerScoreDeadline`; KLTN phải có `councilReportDate` |
| Khi nào chạy | Chỉ khi có `@Valid` (ở controller MVC/API) | Mỗi lần `create()`/`update()` được gọi, bất kể ai gọi |
| Kết quả lỗi | `BindingResult` → hiện dưới từng ô nhập | `InvalidRegistrationPeriodException` → HTML hiện ở khung lỗi chung, API trả 400 kèm mã lỗi |
| Có cần dữ liệu bên ngoài DTO không | Không | Có (sau này: kiểm tra trùng tên đợt, chồng lấn thời gian với đợt khác → cần `store`/repository) |

Lý do phải giữ `validate()` ở Service:

1. **`@Valid` không phải lúc nào cũng chạy.** Annotation trên DTO chỉ được kích hoạt khi có người gọi validator (controller có `@Valid`). Nếu sau này một service khác, một job định kỳ, message consumer hoặc unit test gọi thẳng `periodService.create(request)` mà quên `@Valid`, thì mọi ràng buộc trên DTO **không được kiểm tra**. Đặt bất biến nghiệp vụ trong service thì nó luôn đúng với mọi đường vào.
2. **Ràng buộc liên trường không biểu diễn tự nhiên bằng annotation field-level.** Muốn nhét vào DTO phải dùng `@AssertTrue` ở cấp class, DTO sẽ phình to, biết nghiệp vụ, và không tái sử dụng được cho DTO khác.
3. **Ràng buộc cần dữ liệu ngoài DTO thì DTO không thể làm.** Ví dụ sắp tới: "không được trùng tên đợt", "thời gian đợt mới không chồng lấn đợt đã có" — DTO không giữ danh sách đợt và không gọi được repository. Khi chuyển sang JPA, các rule này **bắt buộc** nằm ở service + query ở repository.
4. **Đúng tài liệu môn học.** Lab 1 trang 3: DTO dùng annotation để validate dữ liệu từ form/JSON "tránh làm rối Model"; `service` là nơi "contains all of the application's business logic". `Các lỗi sai thường gặp.md` cũng yêu cầu nghiệp vụ phải ở Service, không đẩy sang Controller.
5. **An toàn `null`.** Nếu bỏ `validate()`, một request gọi trực tiếp service với `teacherRegisterEnd == null` sẽ ném `NullPointerException` ngay tại dòng `isAfter(...)` thay vì lỗi nghiệp vụ có thông báo rõ ràng (hiện tại `@NotNull` chặn trước ở controller, còn `validate()` là lớp bảo vệ thứ hai).

Phương án thay thế (chưa áp dụng): chuyển 5 rule liên trường sang `@AssertTrue` trong `RegistrationPeriodRequest` để controller không cần `try/catch`; nhưng khi đó rule vẫn không chạy nếu thiếu `@Valid`, và các rule cần dữ liệu (trùng tên, chồng lấn) vẫn phải ở Service → nghiệp vụ bị chia 2 nơi, khó bảo trì. Vì vậy giữ cách hiện tại: **DTO lo hình thức, Service lo nghiệp vụ**.

## 7. Cấu hình tạm thời & lưu ý môi trường

- `application.properties`:
  - `server.port=8080`, `spring.thymeleaf.cache=false` (theo Lab 1 trang 2 — tắt cache để sửa HTML không cần restart).
  - `spring.autoconfigure.exclude=...DataSourceAutoConfiguration,...HibernateJpaAutoConfiguration,...DataJpaRepositoriesAutoConfiguration`: `pom.xml` đã có `spring-boot-starter-data-jpa` + `mysql-connector-j` nhưng **chưa tới bước JPA/MySQL**, nếu không loại trừ thì ứng dụng lỗi ngay khi start (`Failed to configure a DataSource`). Ba dòng loại trừ này sẽ được **xóa** ở bước chuyển sang JPA.
- `config/SecurityConfig.java`: đã chuyển từ "permitAll" tạm thời sang **cấu hình bảo mật thật** (xem mục 11). Form HTML dùng CSRF mặc định (Thymeleaf tự chèn `_csrf`), riêng `/api/**` được miễn CSRF để Postman/curl gọi được (giống Lab 4 trang 13).
- Đã thử loại trừ `SecurityAutoConfiguration` trong properties nhưng Spring Boot 4 báo lỗi: `UserDetailsServiceAutoConfiguration` cần bean `SecurityProperties` do `SecurityAutoConfiguration` tạo ra → không thể loại trừ đơn lẻ, nên dùng `SecurityConfig`.
- Khác biệt Spring Boot 4 so với Boot 3: package auto-configuration đã đổi tên (`org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration`, `org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration`, `org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration`, `org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration`).
- Thymeleaf 3.1.5 đã tích hợp sẵn đối tượng `#temporals` (class `org.thymeleaf.expression.Temporals` trong jar `thymeleaf-3.1.5.RELEASE.jar`) → **không cần** thêm `thymeleaf-extras-java8time` để format `LocalDateTime`/`LocalDate`.
- `#fields` (dùng cho `hasGlobalErrors`, `globalErrors`, `errors`) chỉ hoạt động **bên trong** thẻ có `th:object`; đặt ngoài `<form>` sẽ gây lỗi 500 "Could not bind form errors using expression \"global\"" (đã gặp và đã sửa).

## 8. Kết quả kiểm thử (chạy thật trên `http://localhost:8080`)

| Ca kiểm thử | Kết quả |
|---|---|
| `mvn clean package` | BUILD SUCCESS, `Tests run: 1, Failures: 0, Errors: 0` |
| `GET /api/v1/periods` (rỗng) | `[]` |
| `POST /api/v1/periods` (KLTN hợp lệ, tên tiếng Việt) | 200, trả JSON có `id = 1` |
| `POST /api/v1/periods` (TLCN thiếu `reviewerScoreDeadline`) | 400, `{"error":"INVALID_PERIOD","message":"Đợt TLCN phải có hạn nộp điểm GVPB"}` |
| `GET /api/v1/periods/999` | 404, `{"error":"NOT_FOUND","message":"Không tìm thấy đợt đăng ký có id: 999"}` |
| `PUT /api/v1/periods/1` | 200, dữ liệu cập nhật đúng |
| `GET /periods` (HTML) | 200, hiển thị đúng tên đợt tiếng Việt vừa tạo bằng API → xác nhận HTML và API dùng chung một `ArrayList` |
| `GET /periods/new` | 200 |
| `GET /periods/1/edit` | 200 |
| `POST /periods` (form hợp lệ) | 302 → `/periods` |
| `POST /periods` (bỏ trống tên) | 200, hiện "Tên đợt đăng ký không được để trống" |
| `POST /periods` (SV bắt đầu trước khi GV kết thúc) | 200, hiện "Thời điểm SV đăng ký phải bắt đầu sau khi GV đăng ký kết thúc" |
| `POST /periods/1/delete` (HTML) | 302 → `/periods`, bản ghi biến mất; `GET /api/v1/periods` trả `[]` |
| `GET /` | 200 |

### 8.1. Kiểm thử bảo mật (Bước 1 — chạy thật trên `http://localhost:8080`)

| Ca kiểm thử | Kết quả mong đợi | Kết quả thực tế |
|---|---|---|
| `GET /periods` khi chưa đăng nhập | chặn truy cập | 401 (curl) / 302 → `/login` (trình duyệt gửi `Accept: text/html`) |
| `GET /api/v1/periods` khi chưa đăng nhập | 401 | 401 |
| `GET /login` | 200 | 200 |
| Đăng nhập `admin / 123456` | thành công | 200, trang chủ hiện `admin` |
| admin `GET /periods` | 200 | 200 |
| admin `GET /periods/new` | 403 (admin không phải DEAN) | 403 |
| admin `POST /periods` | 403 | 403 |
| `dean / 123456` `GET /periods/new` | 200 | 200 |
| `dean` `POST /periods` (form + CSRF) | tạo thành công | 200 sau redirect, danh sách có "Dot Test Security" |
| `student / 123456` `GET /periods` | 200 (chỉ xem) | 200 |
| student `GET /periods/new` | 403 | 403 |
| student `GET /periods/1/edit` | 403 | 403 |
| student `POST /periods` | 403 | 403 |
| API `-u admin:123456` GET | 200 | 200 |
| API `-u student:123456` POST | 403 | 403 |
| API `-u admin:123456` POST | 403 | 403 |
| API `-u dean:123456` POST | 200 | 200, JSON trả về `id = 1` |

## 9. Giả định (đã được chốt tại mục 12)

> Mục này giữ lại để tra cứu lịch sử; các điểm dưới đây đã được bạn xác nhận tại mục 12.

1. **Thời gian**: 4 mốc GV/SV đăng ký dùng `LocalDateTime`; `reviewerScoreDeadline` và `councilReportDate` dùng `LocalDate` vì đề bài gọi là "ngày".
2. **Thứ tự giai đoạn**: SV chỉ được bắt đầu đăng ký sau khi GV đăng ký kết thúc (đang kiểm tra `studentRegisterStart >= teacherRegisterEnd`).
3. **Hạn nộp điểm GVPB**: đang so sánh theo `LocalDate` và yêu cầu không trước ngày SV đăng ký kết thúc.
4. **Ngày báo cáo hội đồng**: yêu cầu sau hạn nộp điểm GVPB.
5. **Loại không yêu cầu**: nếu chọn Môn học/NCKH mà người dùng vẫn nhập hạn GVPB/ngày hội đồng thì hệ thống **tự bỏ trống** (không báo lỗi). Nếu bạn muốn báo lỗi thay vì bỏ trống, cần sửa `apply()` + thêm validate.
6. Chưa kiểm tra trùng tên đợt, chưa kiểm tra chồng lấn thời gian giữa các đợt (đề bài không nêu).
7. ~~Chưa có phân quyền "chỉ Trưởng khoa được tạo đợt"~~ — **đã làm ở Bước 1** (mục 11).

## 10. Bước tiếp theo (chờ xác nhận)

- Theo RULE.md, mỗi bước cần bạn xác nhận. Đề xuất **Bước 2**: Bộ môn + hồ sơ GV/SV; màn hình ADMIN tạo/khóa tài khoản và bổ nhiệm/bãi nhiệm `DEAN`; bổ sung `reportDeadline` + `registrationLocked` (DEAN khóa sớm) vào module Đợt đăng ký.

## 11. Bước 1 — Tài khoản & Spring Security (in-memory)

### 11.1. File đã tạo/cập nhật

| File | Vai trò |
|---|---|
| `model/Role.java` | Enum vai trò: `ADMIN`, `LECTURER`, `STUDENT`, `DEAN` |
| `model/Account.java` | Lớp abstract chứa phần chung: `id`, `username`, `password` (đã băm), `fullName`, `email`, `enabled`, `extraRoles`; `getRoles()` = vai trò gốc + `extraRoles` |
| `model/Admin.java` | Tài khoản quản trị, vai trò gốc `ADMIN` |
| `model/Lecturer.java` | Giảng viên: `lecturerCode` (mã GV), vai trò gốc `LECTURER`; `DEAN` nằm trong `extraRoles` |
| `model/Student.java` | Sinh viên: `studentCode` (mã SV), vai trò gốc `STUDENT` |
| `exception/AccountNotFoundException.java` | Không tìm thấy tài khoản (theo username hoặc theo mã) |
| `exception/InvalidAccountException.java` | Dữ liệu tài khoản không hợp lệ (trùng username, trùng mã, mã rỗng) |
| `service/AccountService.java` | Kho tài khoản in-memory (`List<Account>` + `AtomicLong`): `createAdmin`, `createLecturer`, `createStudent`, `assignDean`, `findByUsername`, `findByStudentCode`, `findByLecturerCode`, `findAllStudents`, `findAllLecturers`; mật khẩu băm bằng `PasswordEncoder` |
| `service/CustomUserDetailsService.java` | Chuyển `Account` → `UserDetails`, gắn authority `ROLE_<role>` |
| `config/SecurityConfig.java` | `PasswordEncoder` (BCrypt), `CommandLineRunner` seed tài khoản, `SecurityFilterChain`, `@EnableMethodSecurity` |
| `controller/RegistrationPeriodController.java` | Thêm `@PreAuthorize("hasRole('DEAN')")` cho form tạo, form sửa, tạo, sửa, xóa |
| `controller/api/v1/RegistrationPeriodRestController.java` | Thêm `@PreAuthorize("hasRole('DEAN')")` cho POST/PUT/DELETE |
| `templates/home.html` | Hiện người đăng nhập (`sec:authentication`), nút Đăng xuất, menu theo `sec:authorize` |
| `templates/periods/list.html` | Ẩn nút Tạo/Sửa/Xóa với người không phải DEAN |

### 11.2. Tài khoản seed (mất khi restart, tự seed lại)

| Username | Password | Mã | Roles | Họ tên |
|---|---|---|---|---|
| `admin` | `123456` | — | `ADMIN` | Quản trị hệ thống |
| `dean` | `123456` | `GV001` | `LECTURER` + `DEAN` | Trưởng khoa CNTT |
| `lecturer` | `123456` | `GV002` | `LECTURER` | Giảng viên CNTT |
| `student` | `123456` | `SV001` | `STUDENT` | Sinh viên CNTT |

### 11.3. Quy tắc tài khoản (`AccountService`)

1. `username` không được trống và **không trùng** trong toàn hệ thống.
2. `lecturerCode` / `studentCode` không được trống và không trùng **trong cùng loại** (mã GV trùng chuỗi với mã SV vẫn hợp lệ vì khác không gian).
3. Vai trò gốc suy ra từ lớp: `Admin` → `ADMIN`, `Lecturer` → `LECTURER`, `Student` → `STUDENT` → không thể tạo ra một `Student` lại mang vai trò `LECTURER`.
4. `DEAN` chỉ nằm trong `extraRoles` của `Lecturer`, cấp/bãi qua `assignDean(lecturerCode, true/false)`.
5. Mật khẩu luôn được băm BCrypt trước khi lưu (`applyCommon`).

### 11.4. Quy tắc URL (`SecurityFilterChain`, xét từ trên xuống)

| Rule | Quyền |
|---|---|
| `/`, `/home`, `/login`, `/error`, `/css/**`, `/js/**` | ai cũng vào (trang chủ công khai, khách thấy nút Đăng nhập) |
| `GET /periods/new`, `GET /periods/*/edit` | `DEAN` |
| `GET /periods/**` | đã đăng nhập |
| các method khác `/periods/**` | `DEAN` |
| `/api/**` | đã đăng nhập + **miễn CSRF** |
| còn lại | đã đăng nhập |

- Đăng nhập: form login mặc định của Spring Security tại `/login` (chưa làm trang login riêng), mật khẩu băm BCrypt; API hỗ trợ thêm HTTP Basic để test bằng Postman/curl.
- Logout: `POST /logout` → **luôn** redirect `302` về `/home?logout` (handler tự viết), trang chủ hiện "Bạn đã đăng xuất thành công".
- CSRF bật cho form HTML (Thymeleaf tự thêm `_csrf`), tắt cho `/api/**` (giống Lab 4 trang 13).
- Thứ tự rule rất quan trọng: nếu đặt `GET /periods/**` trước thì `GET /periods/new` sẽ lọt cho mọi người dùng đã đăng nhập (đã gặp lỗi này khi test và đã sửa).

### 11.5. Lưu ý

- Quyền của người dùng được giữ trong **session**: khi ADMIN bổ nhiệm/bãi nhiệm DEAN, người đó phải **đăng xuất rồi đăng nhập lại** mới thấy quyền mới.
- `ADMIN` hiện chưa có màn hình quản trị tài khoản (làm ở Bước 2).
- Bật cả 2 lớp bảo vệ: `SecurityFilterChain` (theo URL) + `@PreAuthorize` (theo method) — giống "defense-in-depth" ở Lab 4 trang 10.

### 11.6. Sự cố "bấm Đăng xuất bị lỗi no explicit mapping for /error" và cách sửa

**Hiện tượng**: bấm Đăng xuất không về được trang chủ, trình duyệt hiện trang lỗi mặc định "This application has no explicit mapping for /error, so you are seeing this as a fallback."

**Nguyên nhân (đã tái hiện bằng curl)**:

1. `HomeController` chỉ map `/`, **không có `/home`** → mở `/home` bị 401/404 rồi rơi vào trang lỗi mặc định.
2. Spring Security 7 chọn `LogoutSuccessHandler` **theo header `Accept`**: request `Accept: text/html` → `302` về `/login?logout`; request khác (`*/*` của curl, `application/json`) → **`204 No Content`**, không redirect. Vì vậy với client không phải browser, logout "im lặng" (không có trang để hiển thị).
   - Nguồn: https://docs.spring.io/spring-security/reference/servlet/authentication/logout.html
   - Nguồn: https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/config/annotation/web/configurers/LogoutConfigurer.html (ghi rõ `logoutSuccessUrl` mặc định là `/login?logout` và là shortcut của `SimpleUrlLogoutSuccessHandler`)
3. Chưa có `templates/error.html` nên mọi lỗi đều hiện trang whitelabel của Spring Boot.

**Cách sửa (đã áp dụng)**:

| Thay đổi | Chi tiết |
|---|---|
| Map thêm `/home` | `@GetMapping({ "/", "/home" })` trong `HomeController` |
| Trang chủ công khai | `permitAll` cho `/` và `/home`; khách thấy nút **Đăng nhập**, người đã đăng nhập thấy tên + **Đăng xuất** (`sec:authorize` + `isAuthenticated()`) |
| Logout luôn redirect | `.logout(logout -> logout.logoutSuccessHandler((request, response, auth) -> response.sendRedirect(request.getContextPath() + "/home?logout")))` → mọi client đều nhận `302`, không phụ thuộc `Accept` |
| Thông báo sau logout | `home.html`: `<p th:if="${param.logout}">Bạn đã đăng xuất thành công.</p>` |
| Trang lỗi riêng | Thêm `templates/error.html` (mã lỗi, mô tả, đường dẫn, link Về trang chủ). Với `Accept: application/json` (API) Spring vẫn trả JSON lỗi thay vì HTML — đúng mong muốn |

**Kiểm thử lại**:

| Ca | Kết quả |
|---|---|
| Anonymous `GET /home` | 200, có nút Đăng nhập, **không** có nút Đăng xuất |
| Sau login `GET /home` | hiện `admin` + nút Đăng xuất |
| `POST /logout` với `Accept: text/html` | 302 → `/home?logout` |
| `POST /logout` với curl (không có Accept html) | 302 → `/home?logout` (trước khi sửa là 204) |
| Trang chủ sau logout | 200 + "Bạn đã đăng xuất thành công" |
| `GET /khong-ton-tai` (đã đăng nhập, `Accept: text/html`) | 404 + trang lỗi riêng (`<p class="code">404</p>`), không còn chữ whitelabel |

### 11.7. Tách model tài khoản (`Account` + `Admin`/`Lecturer`/`Student`) và dùng Lombok

**Vì sao có lớp abstract `Account` thay vì 3 class hoàn toàn độc lập:**

1. 3 class độc lập sẽ lặp lại toàn bộ field đăng nhập (`username`, `password`, `fullName`, `email`, `enabled`) ở 3 nơi; mỗi lần đổi quy tắc mật khẩu/khóa tài khoản phải sửa 3 chỗ.
2. `username` phải **unique toàn hệ thống**, nhưng MySQL **không cho** ràng buộc unique xuyên nhiều bảng → nếu `admin`, `lecturer`, `student` là 3 bảng rời thì không thể chặn 2 tài khoản trùng username.
3. Login (`CustomUserDetailsService`) chỉ tra **một** kho `List<Account>` thay vì thử lần lượt 3 kho.

**Cấu trúc đã triển khai:**

| Class | Field riêng | Vai trò gốc |
|---|---|---|
| `Account` (abstract) | `id`, `username`, `password`, `fullName`, `email`, `enabled`, `extraRoles` | `getBaseRole()` (abstract) |
| `Admin` | — | `ADMIN` |
| `Lecturer` | `lecturerCode` | `LECTURER` (+ `DEAN` trong `extraRoles`) |
| `Student` | `studentCode` | `STUDENT` |

`Account.getRoles()` trả về `{getBaseRole()} ∪ extraRoles` → không thể tạo ra `Student` mang vai trò `LECTURER` hay `Admin` mang `DEAN`.

**Khi chuyển sang JPA (dự kiến ở bước JPA):**

```java
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "account_type")
public abstract class Account { ... }
```

- Bảng `account`: `username` (unique), `password`, `full_name`, `email`, `enabled`.
- Bảng `lecturer`: PK = FK `account.id`, `lecturer_code` unique.
- Bảng `student`: PK = FK `account.id`, `student_code` unique.
- Bảng `admin`: PK = FK `account.id`.
- Chọn `JOINED` thay vì `SINGLE_TABLE`: `username` unique đặt ở đúng 1 bảng, cột mã không bị `NULL` cho loại khác, tra cứu theo mã vẫn có index riêng; đổi lại cần JOIN khi đọc.

**Lombok:**

- `pom.xml` thêm `org.projectlombok:lombok` với `<optional>true</optional>`; version do Spring Boot quản lý = **1.18.46**, tương thích JDK 21.
- Dùng `@Getter`, `@Setter` trên `Account`, `Admin`, `Lecturer`, `Student` (giải thích annotation ở mục 4).
- **Không dùng `@Data`**: `@Data` sinh `toString()` chứa `password` → rất dễ lộ mật khẩu trong log hoặc trang lỗi.
- Khi làm API trả tài khoản (Bước 2): dùng DTO `AccountResponse` **không có** `password`, không trả thẳng entity (đúng nguyên tắc DTO ở Lab 1 trang 3).
- IDE: VS Code cần annotation processing (Java extension thường tự bật). Nếu báo "cannot find symbol getId()" nghĩa là Lombok chưa chạy → kiểm tra extension/annotation processing rồi build lại.

**Kiểm chứng sau refactor:**

| Ca | Kết quả |
|---|---|
| `mvn clean package` | BUILD SUCCESS, **7 test pass** (`QldtApplicationTests` + 6 test `AccountServiceTest`) |
| Test: sinh viên chỉ có `STUDENT`, không có `DEAN` | pass |
| Test: `assignDean("GV001", true)` → có `LECTURER` + `DEAN`, GV002 không bị ảnh hưởng | pass |
| Test: trùng `username` bị chặn | pass |
| Test: trùng `studentCode` bị chặn | pass |
| Test: mật khẩu lưu dạng BCrypt, `matches("123456")` đúng | pass |
| Đăng nhập `admin` / `dean` / `lecturer` / `student` (mật khẩu `123456`) | 200 |
| `admin` + `lecturer` + `student` vào `/periods/new` | 403 |
| `dean` vào `/periods/new` và tạo đợt | 200, đợt mới xuất hiện trong danh sách |

**Chưa thêm (để đúng Bước 2):** `className`/`cohort` cho `Student`, `department` cho `Lecturer` — sẽ bổ sung khi có model `Department`.

## 12. Biên bản chốt kế hoạch (bạn đã xác nhận)

| # | Nội dung đã chốt | Ghi chú triển khai |
|---|---|---|
| 1 | `DEAN` là một vai trò trong tập `roles`, bắt buộc đi kèm `LECTURER`; kiểm tra bằng `hasRole('DEAN')` | Đã làm ở Bước 1 |
| 2 | Nhóm trưởng lưu ở `Team.leaderId`, **không** dùng cờ trên tài khoản sinh viên | Làm ở bước Nhóm |
| 3 | GV tạo đề tài **tự động là GVHD thứ nhất**; được mời thêm 1 GV, **GV được mời phải xác nhận**; DEAN được điều chỉnh GVHD trong giới hạn 1–2 | Làm ở bước Đề tài (cần trạng thái lời mời: `PENDING/ACCEPTED/REJECTED`) |
| 4 | 1 đề tài chỉ cho **1 nhóm**; nhóm đăng ký trước giữ chỗ; đề tài có 3 trạng thái: `AVAILABLE` (chưa được đăng ký) → `PENDING` (chờ duyệt) → `REGISTERED` (đã được đăng ký); nếu bị từ chối quay về `AVAILABLE` | Làm ở bước Đăng ký đề tài |
| 5 | **GVPB là một thành viên của hội đồng**; hội đồng 3–5 GV gồm 1 chủ tịch + 1 thư ký + các thành viên | Lưu ở `CouncilMember.role` |
| 6 | Mỗi GV trong hội đồng nhập **1 điểm 0–10 (1 chữ số thập phân)**; hệ thống tính **trung bình cộng, làm tròn 2 chữ số**; chủ tịch **xác nhận**; DEAN **công bố**; **GVHD không chấm** | Làm ở bước Hội đồng & Điểm |
| 7 | **Môn học và NCKH chỉ GVHD chấm**; **NCKH có công bố điểm**, **Môn học không công bố**; TLCN/KLTN chấm theo hội đồng | Ảnh hưởng bước Điểm & Công bố |
| 8 | DEAN được **khóa đăng ký sớm**; `Period` bổ sung `reportDeadline`; hệ thống **tự chặn nộp báo cáo muộn** | Làm ở Bước 2 (module Period) |
| 9 | Thread-safety: chấp nhận rủi ro ở giai đoạn in-memory, sẽ xử lý khi chuyển JPA bằng `@Transactional` + ràng buộc unique | Ghi nhận, không làm bây giờ |
| 10 | Tách model tài khoản thành `Account` (abstract) + `Admin`/`Lecturer`/`Student`, dùng Lombok `@Getter`/`@Setter`, quản lý tài khoản theo `lecturerCode`/`studentCode` | Đã làm (mở rộng Bước 1) |


