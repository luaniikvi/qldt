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
| `@RequestParam` | Lấy tham số từ query string hoặc body form (ví dụ `password` khi đặt lại mật khẩu, `dean=true/false` khi bổ nhiệm) và gán vào tham số method | Không dùng: phải gọi `request.getParameter("password")` rồi tự parse kiểu dữ liệu |
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
| `@Test` (JUnit 5) | Đánh dấu method là một test case để JUnit platform runner nhận diện và thực thi | Không dùng: runner bỏ qua method, không chạy kiểm thử |
| `@BeforeEach` (JUnit 5) | Thực thi method chuẩn bị trước mỗi bài test để reset trạng thái sạch sẽ, tránh phụ thuộc chéo giữa các test | Không dùng: phải gọi phương thức setup thủ công ở đầu mỗi test method |
| `@TempDir` (JUnit 5) | Tự động tạo thư mục tạm thời trên đĩa cho từng test và tự dọn dẹp sau khi chạy | Không dùng: phải tự `Files.createTempDirectory()` và tự viết hàm xóa đệ quy khi test hoàn tất |
| `@AutoConfigureMockMvc` (Spring Boot WebMvc Test) | Tự động cấu hình bean `MockMvc` cho phép gửi HTTP request giả lập kiểm thử controller mà không cần chạy Web Server thật | Không dùng: phải tự dùng `MockMvcBuilders.webAppContextSetup(context).build()` phức tạp |
| `@WithMockUser` (Spring Security Test) | Giả lập người dùng đã đăng nhập với username và role/authorities chỉ định trong `SecurityContext` khi gọi request | Không dùng: phải tự tạo `UsernamePasswordAuthenticationToken` và gán vào `SecurityContextHolder` trước mỗi request |
| `@Autowired` (Spring Framework) | Tự động tìm và inject bean từ ApplicationContext vào field trong test hoặc component | Không dùng: phải inject qua constructor hoặc gọi `applicationContext.getBean(...)` |

### 4.1. Khái niệm Spring Security dùng ở Bước 1 (không phải annotation)

| Thành phần | Tác dụng | Nếu không dùng thì phải code thế nào |
|---|---|---|
| `SecurityFilterChain` | Chuỗi filter quyết định request nào được vào, redirect login, xử lý CSRF, logout | Phải tự viết servlet filter + `web.xml`/`FilterRegistrationBean` để kiểm tra session, đăng nhập, CSRF |
| `UserDetailsService` (`CustomUserDetailsService`) | Cầu nối giữa kho tài khoản (in-memory `AccountService`) và Spring Security khi đăng nhập | Phải tự viết `AuthenticationProvider` gọi `AccountService` và so khớp mật khẩu bằng tay |
| `PasswordEncoder` + `BCryptPasswordEncoder` | Băm mật khẩu một chiều có salt; `matches()` để so khớp khi login | Phải tự dùng thư viện BCrypt, tự lưu salt, tự so sánh — dễ sai và không đồng bộ với `UserDetailsService` |
| `CommandLineRunner` (`initAccounts`) | Chạy code seed dữ liệu ngay sau khi ApplicationContext khởi động xong | Phải seed trong constructor của service (dễ lỗi thứ tự khởi tạo) hoặc `@PostConstruct` |
| `sec:authorize`, `sec:authentication` (Thymeleaf) | Ẩn/hiện phần HTML theo quyền và in tên người đăng nhập | Phải truyền `Authentication` vào model từ controller rồi dùng `th:if` thủ công ở mọi trang |
| `LogoutSuccessHandler` (lambda trong `.logout(...)`) | Xử lý sau khi logout xong; ở đây redirect về `/home?logout` | Phải tự viết `AuthenticationSuccessHandler`/filter lắng nghe sự kiện logout, hoặc để mặc định (dễ ra 204 hoặc về `/login?logout`) |
| `Authentication` (tham số method controller) | Spring Security inject thông tin người đang đăng nhập (ở Bước 3 dùng `authentication.getName()` để lấy mã GV) | Không dùng: phải gọi `SecurityContextHolder.getContext().getAuthentication()` trong từng method |
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

- Theo RULE.md, mỗi bước cần bạn xác nhận.
- **Bước 2 đã xong** — xem mục 13.
- Đề xuất **Bước 3 — Module Đề tài**: GV tạo đề tài (tự là GVHD thứ nhất), mời thêm 1 GV và **GV được mời phải xác nhận**; **GVHD phải cùng bộ môn** với GVHD thứ nhất (dùng `Lecturer.department`); DEAN duyệt/từ chối/công bố danh sách đề tài theo đợt. Trạng thái đề tài: `AVAILABLE` → `PENDING` → `REGISTERED` (chốt ở mục 12 #4).
- **Bước 3 đã xong** — xem mục 14.
- Đề xuất **Bước 4 — Module Nhóm sinh viên**: SV tạo nhóm (mình là nhóm trưởng), mời thành viên (tối đa 3, thành viên phải xác nhận), mỗi SV chỉ thuộc 1 nhóm trong đợt, chuyển quyền nhóm trưởng; chuẩn bị cho Bước 5 đăng ký đề tài.
- **Bước 4 đã xong** — xem mục 15.
- **Bước 5 đã xong** — xem mục 17 (kèm tính năng hủy lời mời/xóa ở mục 16).
- **Bước 6 đã xong** — xem mục 18.
- **Bước 7 đã xong** — xem mục 19.
- **Bước 8 (JPA + MySQL) đã xong** — xem mục 21.
- **Bước 9 đã xong** — xem mục 22.
- **Bước 10 đã xong** — xem mục 24.
- **Bước 11 đã xong (rút gọn)** — chỉ xuất Excel, bỏ email theo yêu cầu — xem mục 25.
- **Bước 12 (phần biểu đồ) đã xong** — xem mục 26.
- Phần **đóng gói deploy** (profile `prod`, Dockerfile) sẽ làm khi bạn yêu cầu.

## 11. Bước 1 — Tài khoản & Spring Security (in-memory)

### 11.1. File đã tạo/cập nhật

| File | Vai trò |
|---|---|
| `model/Role.java` | Enum vai trò: `ADMIN`, `LECTURER`, `STUDENT`, `DEAN` |
| `model/Account.java` | Lớp abstract chứa phần chung: `id`, `username`, `password` (đã băm), `fullName`, `email`, `enabled`, `extraRoles`; `getRoles()` = vai trò gốc + `extraRoles` |
| `model/Admin.java` | Tài khoản quản trị, vai trò gốc `ADMIN` |
| `model/Lecturer.java` | Giảng viên: `lecturerCode` (mã GV, đồng thời là username đăng nhập), `department` (bộ môn), vai trò gốc `LECTURER`; `DEAN` nằm trong `extraRoles` |
| `model/Student.java` | Sinh viên: `studentCode` (mã SV, đồng thời là username đăng nhập), vai trò gốc `STUDENT` |
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

| Đăng nhập | Password | Vai trò | Bộ môn | Họ tên |
|---|---|---|---|---|
| `admin` | `123456` | `ADMIN` | — | Quản trị hệ thống |
| `GV001` | `123456` | `LECTURER` + `DEAN` | Công nghệ phần mềm | Trưởng khoa CNTT |
| `GV002` | `123456` | `LECTURER` | Hệ thống thông tin | Giảng viên CNTT |
| `SV001` | `123456` | `STUDENT` | — | Sinh viên CNTT |

### 11.3. Quy tắc tài khoản (`AccountService`)

1. **Đăng nhập bằng mã**: với `Lecturer`/`Student`, `username = lecturerCode/studentCode` (ví dụ `GV001`, `SV001`); `Admin` dùng username riêng (`admin`).
2. Mã đăng nhập không được trống và **không trùng trên toàn hệ thống** (mã GV trùng mã SV cũng bị chặn) vì `username` là duy nhất.
3. `Lecturer.department` (bộ môn) **bắt buộc**, không được rỗng; `Student` **không** có lớp/khóa (đề bài không yêu cầu).
4. Vai trò gốc suy ra từ lớp: `Admin` → `ADMIN`, `Lecturer` → `LECTURER`, `Student` → `STUDENT` → không thể tạo ra một `Student` lại mang vai trò `LECTURER`.
5. `DEAN` chỉ nằm trong `extraRoles` của `Lecturer`, cấp/bãi qua `assignDean(lecturerCode, true/false)`.
6. Mật khẩu luôn được băm BCrypt trước khi lưu (`applyCommon`).

### 11.4. Quy tắc URL (`SecurityFilterChain`, xét từ trên xuống)

| Rule | Quyền |
|---|---|
| `/`, `/home`, `/login`, `/error`, `/css/**`, `/js/**` | ai cũng vào (trang chủ công khai, khách thấy nút Đăng nhập) |
| `/admin/**` | `ADMIN` |
| `GET /periods/new`, `GET /periods/*/edit` | `DEAN` |
| `GET /periods/**` | đã đăng nhập |
| các method khác `/periods/**` | `DEAN` |
| `/api/**` | đã đăng nhập + **miễn CSRF** |
| còn lại | đã đăng nhập |

- Đăng nhập: **trang login riêng** tại `/login` (`loginPage("/login")` + `LoginController` + `templates/login.html`), mật khẩu băm BCrypt; API hỗ trợ thêm HTTP Basic để test bằng Postman/curl.
- Logout: `POST /logout` → **luôn** redirect `302` về `/home?logout` (handler tự viết), trang chủ hiện "Bạn đã đăng xuất thành công".
- CSRF bật cho form HTML (Thymeleaf tự thêm `_csrf`), tắt cho `/api/**` (giống Lab 4 trang 13).
- Thứ tự rule rất quan trọng: nếu đặt `GET /periods/**` trước thì `GET /periods/new` sẽ lọt cho mọi người dùng đã đăng nhập (đã gặp lỗi này khi test và đã sửa).

### 11.5. Lưu ý

- Quyền của người dùng được giữ trong **session**: khi ADMIN bổ nhiệm/bãi nhiệm DEAN, người đó phải **đăng xuất rồi đăng nhập lại** mới thấy quyền mới.
- `ADMIN` đã có màn hình quản trị tài khoản tại `/admin/accounts` (Bước 2, xem mục 13).
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
| `Lecturer` | `lecturerCode`, `department` | `LECTURER` (+ `DEAN` trong `extraRoles`) |
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
| `mvn clean package` | BUILD SUCCESS, **12 test pass** (`QldtApplicationTests` + 11 test `AccountServiceTest`) |
| Test: sinh viên chỉ có `STUDENT`, không có `DEAN` | pass |
| Test: `assignDean("GV001", true)` → có `LECTURER` + `DEAN`, GV002 không bị ảnh hưởng | pass |
| Test: trùng `username` bị chặn | pass |
| Test: trùng `studentCode` bị chặn | pass |
| Test: mật khẩu lưu dạng BCrypt, `matches("123456")` đúng | pass |
| Test: đăng nhập bằng mã `SV001` / `GV001` tìm đúng tài khoản | pass |
| Test: mã GV trùng mã SV bị chặn | pass |
| Test: `department` rỗng bị chặn | pass |
| Đăng nhập `admin` / `GV001` / `GV002` / `SV001` (mật khẩu `123456`) | 200 |
| Đăng nhập bằng username cũ `dean` | bị từ chối (`/login?error`) |
| `admin` + `GV002` + `SV001` vào `/periods/new` | 403 |
| `GV001` (DEAN) vào `/periods/new` và tạo đợt | 200, đợt mới xuất hiện trong danh sách |

**Chưa thêm:** `className`/`cohort` cho `Student` — đề bài không yêu cầu (theo xác nhận của bạn).

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
| 11 | **Đăng nhập bằng mã GV/SV** (`GV001`, `SV001`); `admin` vẫn là `admin / 123456`; `Student` không có lớp/khóa; `Lecturer` có `department` (bộ môn) | Đã làm |
| 12 | **Quy tắc cho module Đề tài: GVHD của đề tài phải cùng bộ môn** | Sẽ kiểm tra trong `TopicService` ở Bước 3; dữ liệu so sánh đã có ở `Lecturer.department` |
| 13 | `Lecturer.department` **giữ dạng chuỗi**, không tạo model `Department` | Đã làm; khi nhập cần thống nhất cách viết để so sánh bộ môn chính xác |
| 14 | Đề tài: GV tạo trong thời gian GV đăng ký của đợt, bộ môn lấy theo GV tạo, GVHD 1–2 và **phải cùng bộ môn**, chỉ sửa khi `PENDING`/`REJECTED`, DEAN duyệt/từ chối (kèm lý do)/công bố theo đợt | Đã làm ở Bước 3 (mục 14) |
| 15 | Nhóm SV: nhóm trưởng lưu ở `Team.leaderId`, tối đa 3 thành viên có xác nhận, mỗi SV 1 nhóm/đợt, chuyển quyền nhóm trưởng, thành viên rời nhóm; **tạo nhóm không giới hạn thời gian** (thời gian SV đăng ký + khóa đợt chỉ dùng khi đăng ký đề tài) | Đã làm ở Bước 4 (mục 15) |
| 16 | Cho phép **hủy lời mời** (GVHD và thành viên nhóm) và **xóa GVHD thứ hai / xóa thành viên** (chỉ người tạo đề tài / nhóm trưởng) | Đã làm (mục 16) |
| 17 | **Bước 5**: nhóm trưởng đăng ký 1 đề tài đã công bố trong thời gian SV đăng ký + đợt chưa khóa; `AVAILABLE` → `PENDING` → GVHD duyệt `REGISTERED` / từ chối trả về `AVAILABLE` (kèm lý do); **được hủy đăng ký trước khi được duyệt** | Đã làm (mục 17) |
| 18 | **Bước 6**: chỉ nhóm trưởng nộp báo cáo khi đề tài `REGISTERED`; PDF/DOC/DOCX ≤ 10MB; chặn sau `reportDeadline`; cho **nộp lại** (tăng phiên bản); GVHD xem/tải báo cáo | Đã làm (mục 18) |
| 19 | **Bước 7**: DEAN lập hội đồng 3–5 GV cho đề tài `REGISTERED` (đúng 1 chủ tịch, 1 thư ký, không có GVHD); mỗi thành viên chấm 0–10 (1 chữ số thập phân); chủ tịch tổng hợp điểm trung bình; DEAN công bố; SV trong nhóm + GVHD xem kết quả đã công bố | Đã làm (mục 19) |
| 20 | **Bước 8**: dữ liệu lưu trong **MySQL** qua JPA (`@Entity` + `JpaRepository`), dùng **instance MySQL riêng (port 3307)** với tài khoản `qldt_app` (không dùng root); test chạy trên H2 `MODE=MySQL` | Đã làm (mục 21) |
| 21 | **Bước 9**: DEAN xem thống kê theo đợt (số đề tài, nhóm, báo cáo, hội đồng, điểm trung bình), xuất CSV và API JSON `/api/statistics`; code được format tự động bằng `formatter-maven-plugin` | Đã làm (mục 22) |
| 22 | **Bước 10**: thông báo trong hệ thống khi mời nhóm/mời GVHD, duyệt-từ chối đăng ký, nộp báo cáo, công bố điểm; nhắc hạn nộp báo cáo & nhắc lịch hội đồng (chống trùng), badge số chưa đọc trên menu | Đã làm (mục 24) |
| 23 | **Bước 11**: xuất bảng thống kê ra **.xlsx** (Apache POI) — bỏ email theo yêu cầu; JDBC thêm `createDatabaseIfNotExist`, `useSSL=false`, `serverTimezone=UTC`; xóa thư mục `db`, thay bằng `scripts/start-db.ps1` + `scripts/reset-db.ps1` | Đã làm (mục 25) |
| 24 | **Bước 12 (phần 1)**: biểu đồ thống kê theo đợt (trạng thái đề tài, tiến độ, điểm theo hội đồng) render server-side bằng HTML/CSS, không phụ thuộc CDN | Đã làm (mục 26) |
| 25 | **Xác nhận mật khẩu**: `AccountRequest.confirmPassword`, service kiểm tra khớp mật khẩu khi tạo tài khoản, form admin có ô xác nhận | Đã làm (mục 27) |
| 26 | **DEAN hủy đăng ký đề tài** (`/dean/registrations`, kèm lý do, gửi thông báo) và **nộp báo cáo mọi định dạng** (bỏ `accept` + bỏ whitelist, vẫn giới hạn 10MB) | Đã làm (mục 28) |
| 27 | **Sửa hiển thị báo cáo**: khối báo cáo luôn hiển thị, flash kèm tên tệp + phiên bản, `no-store`, service flush + kiểm tra lại bản ghi | Đã làm (mục 29) |
| 28 | **Sửa tải báo cáo**: `Content-Disposition` theo chuẩn RFC 5987 (`filename*=UTF-8'...`) để trình duyệt lưu đúng tên tiếng Việt thay vì `download` | Đã làm (mục 30) |











## 13. Bước 2 — Quản trị tài khoản, cấu hình Đợt, trang login

### 13.1. File mới/cập nhật

| File | Vai trò |
|---|---|
| `model/AccountType.java` | Enum loại tài khoản `ADMIN`, `LECTURER`, `STUDENT` + `label` tiếng Việt |
| `model/Role.java` | Bổ sung `label` để hiển thị (Quản trị / Giảng viên / Sinh viên / Trưởng khoa) |
| `dto/AccountRequest.java` | DTO tạo tài khoản: `type`, `code`, `fullName`, `email`, `password`, `department` + Bean Validation |
| `service/AccountService.java` | Thêm `findById`, `setEnabled` (khóa/mở), `resetPassword`, `setDean(id, boolean)`, `createAccount(request)` |
| `controller/AccountAdminController.java` | `/admin/accounts`: danh sách, form tạo, tạo, khóa, mở khóa, bổ nhiệm/bãi nhiệm DEAN, đặt lại mật khẩu; `@PreAuthorize("hasRole('ADMIN')")` |
| `templates/admin/accounts/list.html`, `form.html` | Giao diện quản trị tài khoản |
| `controller/LoginController.java`, `templates/login.html` | Trang đăng nhập riêng (thay trang mặc định của Spring Security) |
| `model/RegistrationPeriod.java` | Thêm `reportDeadline`, `registrationLocked`; chuyển sang Lombok `@Getter/@Setter` |
| `dto/RegistrationPeriodRequest.java` | Thêm `reportDeadline`; chuyển sang Lombok |
| `service/RegistrationPeriodService.java` | `validateReportDeadline` + `setRegistrationLocked` |
| `controller/RegistrationPeriodController.java` | Thêm `POST /periods/{id}/lock` và `/unlock` (chỉ DEAN) |
| `templates/periods/list.html`, `form.html` | Cột "Hạn nộp báo cáo", "Trạng thái", nút Khóa/Mở khóa |
| `config/SecurityConfig.java` | `/admin/**` → `ADMIN`; `formLogin.loginPage("/login")` |
| `src/test/.../AccountServiceTest.java` | Thêm 4 test (khóa/mở, reset mật khẩu, chỉ GV được làm DEAN, tạo từ `AccountRequest`) |
| `src/test/.../RegistrationPeriodServiceTest.java` | Mới, 5 test cho nghiệp vụ Period |

### 13.2. Nghiệp vụ

1. **Tạo tài khoản** (ADMIN): nhập loại + mã + họ tên + email + mật khẩu; nếu là giảng viên thì **bắt buộc bộ môn**. Mã không được trùng toàn hệ thống, mật khẩu 6–50 ký tự, email đúng định dạng (DTO).
2. **Khóa / mở khóa**: đặt `enabled`; tài khoản bị khóa không đăng nhập được (`DisabledException`).
3. **Đặt lại mật khẩu**: mật khẩu mới ≥ 6 ký tự, được băm BCrypt lại.
4. **Bổ nhiệm / bãi nhiệm DEAN**: chỉ áp dụng cho `Lecturer`; gọi `setDean(id, true/false)`.
5. **Hạn nộp báo cáo** (`reportDeadline`): **bắt buộc** (`@NotNull` trong DTO + kiểm tra trong service), phải **sau ngày SV đăng ký kết thúc**.
6. **Khóa đăng ký sớm** (`registrationLocked`): DEAN bật/tắt bằng nút trên danh sách đợt. Hiện chỉ lưu trạng thái; việc **chặn đăng ký** sẽ dùng ở Bước 5.
7. **Trang login riêng** tại `/login`: form POST `/login` (CSRF tự chèn), hiện lỗi khi `?error`, hiện thông báo khi `?logout`.

### 13.3. Kiểm thử

- `mvn clean package`: **BUILD SUCCESS**, `Tests run: 21, Failures: 0` (1 context + 15 `AccountServiceTest` + 5 `RegistrationPeriodServiceTest`).
- Chạy thật trên `http://localhost:8080`:

| Ca | Kết quả |
|---|---|
| `GET /login` | trang login riêng hiển thị đúng |
| `admin / 123456` | vào `/home`, thấy link "Quản trị tài khoản" |
| `GET /admin/accounts` (admin) | 200, danh sách có `GV001`, `SV001`, cột Bộ môn/Trạng thái |
| `GV001` (không phải ADMIN) vào `/admin/accounts` | 403 |
| Tạo `GV003` qua form | 200, xuất hiện trong danh sách |
| Khóa `GV003` | 200, trạng thái "Đã khóa"; đăng nhập `GV003` bị từ chối (`/login?error`) |
| Đặt lại mật khẩu `SV001` → `999999` | đăng nhập `SV001/999999` thành công, `SV001/123456` bị từ chối |
| Bổ nhiệm DEAN cho `GV002` | danh sách hiện "Trưởng khoa"; `GV002` đăng nhập lại vào `/periods/new` = 200, `/admin/accounts` = 403 |
| Tạo đợt có `reportDeadline = 01/02/2027` | danh sách hiển thị đúng `01/02/2027` |
| Tạo đợt có hạn nộp trước ngày SV kết thúc | hiện "Hạn nộp báo cáo phải sau thời điểm SV đăng ký kết thúc" |
| Khóa / mở khóa đợt | trạng thái đổi "Đã khóa" ↔ "Đang mở" |

### 13.4. Giới hạn hiện tại

- Chưa có API JSON cho quản trị tài khoản (chỉ giao diện MVC); có thể bổ sung sau nếu cần dùng cho mobile.
- ADMIN có thể tự khóa chính mình hoặc bãi nhiệm hết Trưởng khoa (dữ liệu in-memory sẽ reset khi restart). Nếu cần chặn triệt để, thêm kiểm tra "không được khóa tài khoản ADMIN duy nhất" ở `AccountService`.
- Đổi vai trò DEAN cần **đăng nhập lại** mới có hiệu lực (authority lưu trong session).
- `reportDeadline` là **bắt buộc**; form đã có `th:errors="*{reportDeadline}"` để hiện lỗi khi bỏ trống.
- Danh sách tài khoản/đợt chưa phân trang (dữ liệu in-memory, số lượng nhỏ).

### 13.5. Ẩn/hiện ô "Bộ môn" theo loại tài khoản (JavaScript phía client)

- Yêu cầu: ô **Bộ môn** chỉ hiện khi chọn loại **Giảng viên**.
- `th:if="*{type != null and type.name() == 'LECTURER'}"` **không đáp ứng được**: Thymeleaf render một lần ở server; lúc mở form `type = null` nên khối bị loại khỏi HTML, khi người dùng đổi select thì trang không render lại → khối vẫn ẩn (chỉ hiện sau khi submit và server trả lại form).
- Cách làm hiện tại: **luôn render** `<div id="departmentBlock">` và dùng JavaScript trong `form.html`:
  - `th:field="*{type}"` sinh ra `id="type"`; `th:field="*{department}"` sinh ra `id="department"` → JS lấy phần tử theo các id này.
  - `syncDepartment()`: nếu `type === 'LECTURER'` thì hiện khối và đặt `required`; ngược lại ẩn khối, bỏ `required` và xoá giá trị đã nhập.
  - Gọi `syncDepartment()` ngay khi tải trang để xử lý trường hợp **server render lại form sau lỗi** (select đã chọn Giảng viên).
- Vì khối luôn được render nên khi người dùng tắt JavaScript thì ô Bộ môn vẫn hiện (chấp nhận được) và **server vẫn kiểm tra** `department` bắt buộc với giảng viên ở `AccountService.validateDepartment`.
- Đã kiểm chứng: HTML trả về có `id="type"`, `id="department"`, `id="departmentBlock"` và script; submit thiếu bộ môn → 200 + "Bộ môn của giảng viên không được để trống", option `LECTURER` vẫn `selected` để JS hiện lại khối.

## 14. Bước 3 — Module Đề tài

### 14.1. File mới/cập nhật

| File | Vai trò |
|---|---|
| `model/Topic.java` | Đề tài: `name`, `description`, `period`, `primaryAdvisor`, `department`, `advisors` (1–2), `status`, `availability`, `rejectionReason`, `invitation` |
| `model/AdvisorInvitation.java` | Lời mời GVHD thứ hai: `lecturer`, `status`, `responseReason` |
| `model/TopicStatus.java` | `PENDING`, `APPROVED`, `REJECTED`, `PUBLISHED` (+ label) |
| `model/TopicAvailability.java` | `AVAILABLE`, `PENDING`, `REGISTERED` (+ label) — dùng cho Bước 5 |
| `model/InvitationStatus.java` | `PENDING`, `ACCEPTED`, `REJECTED` (+ label) |
| `dto/TopicRequest.java` | Form đề tài: `name`, `description`, `periodId` + validation + `from(Topic)` |
| `exception/TopicNotFoundException.java`, `InvalidTopicException.java` | Lỗi không tìm thấy / vi phạm nghiệp vụ |
| `service/TopicService.java` | Toàn bộ nghiệp vụ đề tài (xem 14.2) |
| `controller/TopicController.java` | `/topics`: danh sách, tạo, sửa, mời GVHD, phản hồi lời mời (LECTURER) |
| `controller/TopicReviewController.java` | `/dean/topics`: duyệt, từ chối, công bố theo đợt (DEAN) |
| `controller/PublishedTopicController.java` | `/published-topics`: danh sách đề tài đã công bố (mọi người đã đăng nhập) |
| `templates/topics/list.html`, `form.html`, `pending.html`, `published.html` | Giao diện |
| `config/SecurityConfig.java` | Thêm rule `/dean/**`, `/topics/**`, `/published-topics` |
| `templates/home.html` | Menu theo vai trò: Đề tài của tôi / Duyệt và công bố đề tài / Đề tài đã công bố |
| `src/test/.../TopicServiceTest.java` | 12 test cho nghiệp vụ đề tài |

### 14.2. Nghiệp vụ (đặt trong `TopicService`)

1. **Tạo đề tài**: chỉ trong khoảng `teacherRegisterStart`–`teacherRegisterEnd` của đợt (đề bài: "theo đúng thời gian quy định"); đề tài **thuộc bộ môn của GV tạo** (`topic.department = primaryAdvisor.department`); người tạo tự động là **GVHD thứ nhất**; trạng thái `PENDING`.
2. **Sửa đề tài**: chỉ người tạo, chỉ khi `PENDING` hoặc `REJECTED`; sau khi sửa (kể cả đề tài vừa bị từ chối) quay về `PENDING` và xoá lý do từ chối.
3. **Mời GVHD thứ hai**: tối đa 2 GVHD; không mời chính mình; không mời khi đang có lời mời chờ; **bắt buộc cùng bộ môn** với GVHD thứ nhất.
4. **Phản hồi lời mời**: chỉ đúng GV được mời mới phản hồi; đồng ý → thêm vào `advisors` và đánh dấu `ACCEPTED`; từ chối → **phải nhập lý do**, đánh dấu `REJECTED` (người tạo có thể mời GV khác).
5. **Duyệt / từ chối**: chỉ DEAN, chỉ khi `PENDING`; từ chối **phải nhập lý do** (`rejectionReason`).
6. **Công bố**: DEAN công bố **theo đợt**, chuyển toàn bộ đề tài `APPROVED` của đợt sang `PUBLISHED` và đặt `availability = AVAILABLE`; nếu đợt không có đề tài đã duyệt thì báo lỗi.
7. **Tra cứu**: GV thấy đề tài mình hướng dẫn + lời mời đang chờ; SV/GV xem danh sách đã công bố ở `/published-topics`.

### 14.3. Phân quyền

| Đường dẫn | Quyền |
|---|---|
| `/topics/**` | `LECTURER` (DEAN cũng có `LECTURER` nên dùng được) |
| `/dean/topics/**` | `DEAN` |
| `/published-topics` | đã đăng nhập |

Bảo vệ 2 lớp: rule trong `SecurityFilterChain` + `@PreAuthorize` trên cả 3 controller.

### 14.4. Kiểm thử

- `mvn clean package`: **BUILD SUCCESS**, `Tests run: 33, Failures: 0` (1 context + 15 account + 5 period + 12 topic).
- Chạy thật trên `http://localhost:8080`:

| Ca | Kết quả |
|---|---|
| DEAN tạo đợt có thời gian GV đăng ký bao quanh hôm nay | 200 |
| `GV001` tạo đề tài | 200, trạng thái "Chờ duyệt", bộ môn "Công nghệ phần mềm" |
| `GV001` mời `GV002` (Hệ thống thông tin) | bị chặn: *"Giảng viên được mời phải cùng bộ môn..."* ✔ |
| Tạo `GV004` cùng bộ môn (ADMIN) rồi mời | 302, hiện "Chờ phản hồi", nút mời bị ẩn |
| `GV004` đăng nhập, đồng ý lời mời | 302, đề tài có **2 GVHD** |
| DEAN thấy đề tài chờ duyệt → duyệt | 302 |
| DEAN bấm "Công bố đề tài của đợt" | 302, đề tài chuyển `PUBLISHED` |
| `SV001` xem `/published-topics` | thấy đề tài đã công bố |
| `SV001` vào `/topics` | 403 |
| `GV002` vào `/dean/topics` | 403 |
| `GV001` (DEAN) vào `/admin/accounts` | 403 |

### 14.5. Giả định & giới hạn hiện tại

1. Đề tài **không cho chọn bộ môn** — luôn lấy theo bộ môn của GV tạo (bảo đảm quy tắc cùng bộ môn).
2. Chỉ được tạo/sửa đề tài **trong thời gian GV đăng ký** của đợt; đợt bị `registrationLocked` **chưa** chặn việc tạo đề tài (khoá này dùng cho SV ở Bước 5).
3. Đề tài đã `APPROVED`/`PUBLISHED` thì **không sửa được**; muốn sửa phải quay lại quy trình (hiện chưa có chức năng "hủy duyệt").
4. Chưa giới hạn số đề tài mỗi GV, chưa chống trùng tên đề tài trong cùng đợt.
5. Chưa có API JSON cho module đề tài (chỉ MVC) và chưa gửi email thông báo khi mời/duyệt (Lab 6).
6. `#lists.size(...)` dùng trong template để đếm số GVHD — khi lên JPA sẽ đổi sang quan hệ `@ManyToMany`/bảng trung gian.

## 15. Bước 4 — Module Nhóm sinh viên

### 15.1. File mới/cập nhật

| File | Vai trò |
|---|---|
| `model/Team.java` | Nhóm: `period`, `leader`, `members` (1–3, gồm nhóm trưởng), `invitations` |
| `model/TeamInvitation.java` | Lời mời vào nhóm: `student`, `status`, `responseReason` |
| `dto/TeamRequest.java` | Form tạo nhóm: `periodId` + validation |
| `dto/TeamView.java` | Record cho view: `team`, `leader`, `member`, `invited` (tính theo người đang đăng nhập) |
| `exception/TeamNotFoundException.java`, `InvalidTeamException.java` | Lỗi không tìm thấy / vi phạm nghiệp vụ nhóm |
| `service/TeamService.java` | Toàn bộ nghiệp vụ nhóm (xem 15.2) |
| `controller/TeamController.java` | `/teams`: danh sách, tạo nhóm, mời, phản hồi lời mời, chuyển nhóm trưởng, rời nhóm (STUDENT) |
| `templates/teams/list.html`, `form.html` | Giao diện nhóm |
| `config/SecurityConfig.java` | Thêm rule `/teams/**` → `STUDENT` |
| `templates/home.html` | Menu "Nhóm của tôi" cho sinh viên |
| `src/test/.../TeamServiceTest.java` | 11 test nghiệp vụ nhóm |

### 15.2. Nghiệp vụ (`TeamService`)

1. **Tạo nhóm**: chỉ cần đợt tồn tại — **không** yêu cầu thời gian nằm trong `studentRegisterStart`–`studentRegisterEnd` và **không** phụ thuộc `registrationLocked` (nhóm là bước chuẩn bị; hai điều kiện này chỉ dùng cho **đăng ký đề tài** ở Bước 5); người tạo là **nhóm trưởng** và là thành viên đầu tiên.
2. **Mỗi SV chỉ thuộc 1 nhóm trong 1 đợt**: chặn khi tạo nhóm, khi mời và khi nhận lời mời (kiểm tra lại tại thời điểm chấp nhận).
3. **Tối đa 3 thành viên**: tính cả lời mời đang chờ (`members.size() + pendingInvitations < 3`) để tránh nhận quá số lượng.
4. **Mời thành viên**: chỉ nhóm trưởng, mời bằng mã SV; không mời chính mình, không mời người đã trong nhóm/đã có lời mời chờ/đã thuộc nhóm khác cùng đợt.
5. **Phản hồi lời mời**: chỉ đúng SV được mời; đồng ý → thêm vào `members` (kiểm tra lại sức chứa + 1 nhóm/đợt); từ chối → **phải nhập lý do**.
6. **Chuyển nhóm trưởng**: chỉ nhóm trưởng hiện tại, người nhận phải là thành viên trong nhóm.
7. **Rời nhóm**: chỉ thành viên (không phải nhóm trưởng); nhóm trưởng phải chuyển quyền trước.

### 15.3. Phân quyền

- `/teams/**` → `STUDENT` (rule trong `SecurityFilterChain` + `@PreAuthorize` trên controller).
- Home hiển thị "Nhóm của tôi" cho sinh viên.

### 15.4. Kiểm thử

- `mvn clean package`: **BUILD SUCCESS**, `Tests run: 44, Failures: 0` (1 context + 15 account + 5 period + 12 topic + 11 team).
- Chạy thật trên `http://localhost:8080` (đợt bất kỳ, không cần hôm nay nằm trong thời gian SV đăng ký):

| Ca | Kết quả |
|---|---|
| SV001 tạo nhóm | 302, nhóm có 1 thành viên (SV001 là nhóm trưởng) |
| Tạo nhóm cho đợt đã hết thời gian SV đăng ký / đợt đang khóa | vẫn tạo được (không phụ thuộc thời gian/khóa) |
| SV001 mời SV002 | 302, hiện "Chờ phản hồi" |
| SV002 đồng ý | 302, nhóm có 2 thành viên |
| SV001 mời SV003, SV003 đồng ý | 302, nhóm đủ 3 người → **nút mời bị ẩn** |
| SV001 mời SV004 khi đã đủ 3 | hiện lỗi *"Nhóm đã đủ 3 thành viên..."* |
| SV002 (đang ở nhóm 1) tạo nhóm mới | bị chặn, form hiện lỗi |
| SV001 chuyển quyền nhóm trưởng cho SV002 | 302, SV002 thấy nút mời, SV001 thấy nút **Rời nhóm** |
| SV001 rời nhóm | 302 |
| SV001 tạo nhóm mới sau khi đã rời | thành công (không còn thuộc nhóm nào trong đợt) |
| `GV001` vào `/teams` | 403 |
| `SV001` vào `/topics` | 403 |

### 15.5. Sự cố phát hiện và đã sửa trong bước này

1. `RegistrationPeriodService.validateReportDeadline` bị sửa sai: gọi **vô điều kiện** và kiểm tra `request == null` (thay vì `getReportDeadline() == null`) → **NullPointerException** khi đợt không nhập hạn báo cáo (làm hỏng toàn bộ test tạo đợt). Đã sửa: chỉ gọi khi cần, kiểm tra đúng trường, và vì `reportDeadline` là bắt buộc nên service ném lỗi rõ ràng khi thiếu.
2. `RegistrationPeriodRequest` có `@NotNull` cho `reportDeadline` nhưng `periods/form.html` **thiếu `th:errors`** cho trường này → submit thiếu deadline bị trả lại form mà **không hiện lỗi**. Đã thêm dòng lỗi + cập nhật hint "(bắt buộc, phải sau ngày SV đăng ký kết thúc)".
3. Vì `reportDeadline` bắt buộc, các test/helper tạo đợt (`RegistrationPeriodServiceTest`, `TopicServiceTest`, `TeamServiceTest`) đã được bổ sung deadline.

### 15.6. Giả định & giới hạn

1. Nhóm **không phụ thuộc thời gian** `studentRegisterStart`–`studentRegisterEnd` và **không bị chặn bởi `registrationLocked`**; hai điều kiện này chỉ áp dụng khi **đăng ký đề tài** (Bước 5).
2. Nhóm **không có tên riêng** (đề bài không yêu cầu) — hiển thị "Nhóm của <nhóm trưởng>".
3. Nhóm trưởng **không được rời nhóm**; muốn rời phải chuyển quyền trước. Chưa có chức năng "giải tán nhóm" nên nhóm 1 người vẫn tồn tại.
4. Lời mời đã từ chối vẫn lưu lại để tra cứu; nhóm trưởng có thể mời SV khác.
5. Đề tài được gắn vào nhóm ở Bước 5 (mục 17); chưa có API JSON cho module nhóm.

## 16. Hủy lời mời & xóa GVHD / thành viên

### 16.1. Thay đổi nghiệp vụ

- `InvitationStatus` bổ sung `CANCELLED` ("Đã hủy") — dùng chung cho lời mời GVHD và lời mời vào nhóm.
- `TopicService.cancelAdvisorInvitation(topicId, actor)`: **người tạo đề tài** hủy lời mời GVHD đang `PENDING`; sau đó có thể mời GV khác.
- `TopicService.removeCoAdvisor(topicId, lecturerCode, actor)`: **người tạo đề tài** xóa GVHD thứ hai khi đề tài còn sửa được (`PENDING`/`REJECTED`); lời mời tương ứng chuyển `CANCELLED`, GVHD thứ nhất vẫn giữ.
- `TeamService.cancelInvitation(teamId, studentCode, actor)`: **nhóm trưởng** hủy lời mời đang chờ.
- `TeamService.removeMember(teamId, studentCode, actor)`: **nhóm trưởng** xóa thành viên (không cho tự xóa mình); lời mời `ACCEPTED` tương ứng chuyển `CANCELLED` để giữ lịch sử.

Lịch sử lời mời **không bị xóa** — chỉ đổi trạng thái, nên có thể tra cứu và mời lại người cũ.

### 16.2. Route mới

| Method | URL | Quyền |
|---|---|---|
| POST | `/topics/{id}/invitation/cancel` | người tạo đề tài (LECTURER) |
| POST | `/topics/{id}/advisor/remove` (`lecturerCode`) | người tạo đề tài |
| POST | `/teams/{id}/invitation/cancel` (`studentCode`) | nhóm trưởng (STUDENT) |
| POST | `/teams/{id}/members/remove` (`studentCode`) | nhóm trưởng |

### 16.3. Kiểm thử

- Unit: `TopicServiceTest` +4 (hủy lời mời & mời lại, chỉ người tạo được hủy, xóa GVHD thứ hai, chỉ người tạo được xóa), `TeamServiceTest` +4 (hủy & mời lại, chỉ nhóm trưởng được hủy, xóa thành viên, không xóa nhóm trưởng).
- Runtime: mời `SV002` → **Hủy lời mời** → mời lại → `SV002` đồng ý → **Xóa** `SV002`; thành viên trở về 1 và nút xóa biến mất.

## 17. Bước 5 — Đăng ký đề tài

### 17.1. File mới/cập nhật

| File | Vai trò |
|---|---|
| `model/Team.java` | Thêm `topic` (đề tài nhóm đã đăng ký) và `registrationRejectionReason` |
| `dto/RegistrationView.java` | Record `(topic, team)` cho màn duyệt đăng ký |
| `exception/InvalidRegistrationException.java` | Lỗi nghiệp vụ đăng ký |
| `service/TopicRegistrationService.java` | Đăng ký / hủy / duyệt / từ chối / danh sách chờ duyệt |
| `service/TopicService.java` | Thêm `findAvailablePublished()` |
| `service/TeamService.java` | Thêm `findByTopicId(topicId)` |
| `controller/TeamController.java` | `GET/POST /teams/{id}/register`, `POST /teams/{id}/registration/cancel` |
| `controller/RegistrationReviewController.java` | `/registrations`: danh sách chờ duyệt, duyệt, từ chối |
| `templates/teams/register.html` | Chọn đề tài để đăng ký |
| `templates/registrations/list.html` | Màn duyệt đăng ký của GVHD |
| `templates/teams/list.html` | Cột "Đề tài": tên đề tài + trạng thái (**Chờ duyệt / Đã duyệt**), nút đăng ký/hủy (khi chờ duyệt), lý do bị từ chối (khi chưa có đề tài) |
| `config/SecurityConfig.java` | `/registrations/**` → `LECTURER` |
| `templates/home.html` | Menu "Duyệt đăng ký đề tài" cho giảng viên |
| `src/test/.../TopicRegistrationServiceTest.java` | 12 test |

### 17.2. Nghiệp vụ (`TopicRegistrationService`)

1. **Chỉ nhóm trưởng** được đăng ký và hủy đăng ký.
2. Mỗi nhóm chỉ đăng ký **1 đề tài**.
3. **Chỉ bước này** kiểm tra thời gian `studentRegisterStart`–`studentRegisterEnd` và `registrationLocked`.
4. Đề tài phải **đã công bố**, `AVAILABLE` và **cùng đợt** với nhóm.
5. Đăng ký → đề tài `PENDING` (giữ chỗ, nhóm khác không đăng ký được).
6. **GVHD duyệt** → `REGISTERED`; **từ chối** (bắt buộc lý do) → đề tài về `AVAILABLE`, gỡ khỏi nhóm và lưu lý do vào `team.registrationRejectionReason`.
7. **Hủy đăng ký** chỉ khi đề tài còn `PENDING` (trước khi được duyệt) — đúng xác nhận của bạn; đã `REGISTERED` thì không hủy.

### 17.3. Phân quyền

| Đường dẫn | Quyền |
|---|---|
| `/teams/{id}/register`, `/teams/{id}/registration/cancel` | `STUDENT` (service kiểm tra thêm phải là nhóm trưởng) |
| `/registrations/**` | `LECTURER` (service kiểm tra thêm phải là GVHD của đề tài) |

### 17.4. Kiểm thử

- `mvn clean package`: **BUILD SUCCESS**, `Tests run: 64, Failures: 0`.
- Chạy thật:

| Ca | Kết quả |
|---|---|
| SV001 mở `/teams/1/register` | thấy đề tài đã công bố của đợt |
| SV001 đăng ký đề tài | 302, nhóm hiện đề tài + trạng thái "Chờ duyệt", có nút **Hủy đăng ký** |
| SV001 hủy đăng ký (trước khi duyệt) | 302, nhóm trở về "Chưa đăng ký đề tài" |
| SV001 đăng ký lại | 302 |
| GV001 vào `/registrations` | thấy nhóm + thành viên đang chờ duyệt |
| GV001 duyệt | 302, đề tài `REGISTERED`, nút hủy của SV biến mất |
| SV001 hủy sau khi duyệt | bị chặn, hiện lỗi *"Đề tài đã được duyệt, không thể hủy đăng ký"* |

### 17.5. Giả định & giới hạn

1. Mỗi đề tài chỉ cho **1 nhóm**; đã `REGISTERED` thì không đổi/hủy.
2. Bất kỳ GVHD nào của đề tài đều duyệt được (không chỉ người tạo).
3. Lý do từ chối lưu ở `Team` và hiển thị cho nhóm; khi đăng ký lại sẽ xóa lý do cũ.
4. Nhãn trạng thái `TopicAvailability` đã đổi: `PENDING` → "Chờ duyệt", `REGISTERED` → "Đã duyệt" (hiển thị ở cột Đề tài của nhóm, danh sách đề tài của GV và trang đề tài đã công bố).
4. Chưa có thông báo/email khi duyệt/từ chối (Lab 6) và chưa có API JSON.
5. Trong unit test, để chuyển từ giai đoạn GV sang giai đoạn SV, test **chỉnh trực tiếp mốc thời gian** của `RegistrationPeriod` (dữ liệu in-memory) sau khi đã tạo đề tài — không ảnh hưởng code nghiệp vụ.



## 18. Bước 6 — Nộp báo cáo

### 18.1. File mới/cập nhật

| File | Vai trò |
|---|---|
| `model/Report.java` | Báo cáo: `team`, `originalFileName`, `storedFileName`, `size`, `version`, `submittedAt` |
| `exception/InvalidReportException.java`, `ReportNotFoundException.java` | Lỗi nghiệp vụ / không tìm thấy báo cáo |
| `service/ReportService.java` | Nộp báo cáo, kiểm tra quyền + hạn + định dạng + dung lượng, lưu tệp vào `uploads/`, nộp lại tăng `version`; `findByAdvisor` cho GVHD |
| `controller/ReportController.java` | `/reports` (danh sách báo cáo của GVHD), `/reports/{id}/download` |
| `controller/TeamController.java` | Thêm `POST /teams/{id}/report` (multipart) và gắn `report` vào `TeamView` |
| `dto/TeamView.java` | Thêm trường `report` |
| `templates/teams/list.html` | Khối "Báo cáo": hạn nộp, tệp + phiên bản + thời điểm, form nộp/nộp lại |
| `templates/reports/list.html` | Màn danh sách báo cáo cho GVHD + link tải |
| `config/SecurityConfig.java` | `/reports/**` → `LECTURER` |
| `templates/home.html` | Menu "Báo cáo đề tài" cho giảng viên |
| `application.properties` | `spring.servlet.multipart.max-file-size=10MB`, `max-request-size=12MB`, `app.upload-dir=uploads` |
| `src/test/.../ReportServiceTest.java` | 8 test |

### 18.2. Nghiệp vụ (`ReportService`)

1. **Chỉ nhóm trưởng** được nộp báo cáo.
2. Đề tài của nhóm phải ở trạng thái **`REGISTERED`** (đã được GVHD duyệt đăng ký).
3. **Chặn sau `reportDeadline`**: so sánh theo ngày, nộp trong đúng ngày hạn vẫn hợp lệ.
4. Tệp hợp lệ: **PDF / DOC / DOCX**, không rỗng, **tối đa 10MB**.
5. Tệp lưu tại `uploads/team-{teamId}.{ext}` (tạo thư mục nếu chưa có); metadata lưu trong bộ nhớ.
6. **Cho nộp lại**: ghi đè tệp cũ, tăng `version`, cập nhật `submittedAt`.
7. GVHD xem danh sách báo cáo của đề tài mình và tải tệp; SV không vào được `/reports`.

### 18.3. Kiểm thử

- `mvn clean package`: **BUILD SUCCESS**, `Tests run: 72, Failures: 0` (8 test mới cho `ReportService`).
- Chạy thật:

| Ca | Kết quả |
|---|---|
| SV001 nộp `bc1.pdf` | 302, trang nhóm hiện "Báo cáo: bc1.pdf (v1)", hạn nộp 10/11/2026, giờ nộp |
| GV001 vào `/reports` | thấy tệp `bc1.pdf` |
| GV001 tải `/reports/1/download` | 200 |
| SV001 vào `/reports` | **403** |
| SV001 nộp lại | 302, "bc2.pdf (v2)" |
| Đổi `reportDeadline` về 22/10/2026 rồi nộp | bị chặn, hiện lỗi *"Đã quá hạn nộp báo cáo (hạn: 2026-10-22)"*, báo cáo cũ vẫn giữ |

### 18.4. Giả định & giới hạn

1. Metadata báo cáo vẫn **in-memory** (mất khi restart); tệp nằm trên đĩa tại thư mục `uploads/`.
2. Chỉ lưu **bản mới nhất** + số phiên bản, không lưu lịch sử từng lần nộp.
3. Hạn nộp so sánh theo **ngày** (nộp trong ngày hạn vẫn được).
4. Chưa có thông báo/email khi nộp hoặc nhắc hạn (Lab 6).
5. Chưa có API JSON cho module báo cáo.


## 19. Bước 7 — Hội đồng & chấm điểm

### 19.1. File mới/cập nhật

| File | Vai trò |
|---|---|
| `model/CouncilRole.java` | Enum vai trò: `CHAIR` (Chủ tịch), `SECRETARY` (Thư ký), `MEMBER` (Thành viên) |
| `model/CouncilMember.java` | Thành viên hội đồng: `lecturer`, `role`, `score`, `comment`, `scoredAt` |
| `model/Council.java` | Hội đồng: `name`, `topic`, danh sách `members` |
| `model/TopicResult.java` | Kết quả: `council`, `averageScore`, `published`, `publishedAt` |
| `exception/InvalidCouncilException.java`, `CouncilNotFoundException.java` | Lỗi nghiệp vụ |
| `service/CouncilService.java` | Lập hội đồng, chấm điểm, tổng hợp, công bố, truy vấn kết quả |
| `dto/CouncilView.java` | Gói dữ liệu cho màn hình: hội đồng + vai trò/điểm của tôi + kết quả + đã chấm đủ |
| `controller/CouncilController.java` | `/councils` (GV): hội đồng của tôi, `POST /{id}/score`, `POST /{id}/confirm` |
| `controller/CouncilAdminController.java` | `/dean/councils`: lập hội đồng, `POST /results/{id}/publish` |
| `controller/ResultController.java` | `/results`: SV xem kết quả nhóm mình, GVHD xem kết quả đề tài mình hướng dẫn |
| `templates/councils/list.html`, `councils/dean.html`, `results/list.html` | Giao diện |
| `config/SecurityConfig.java` | `/councils/**` → `LECTURER`; thêm GV004, GV005 vào dữ liệu mẫu |
| `templates/home.html` | Menu "Lập hội đồng và công bố điểm", "Hội đồng của tôi", "Kết quả..." |
| `src/test/.../CouncilServiceTest.java` | 15 test |

### 19.2. Nghiệp vụ (`CouncilService`)

1. Chỉ lập hội đồng cho đề tài **`REGISTERED`** và **mỗi đề tài chỉ 1 hội đồng**.
2. Hội đồng **3–5 GV**, đúng **1 chủ tịch** và **1 thư ký**; không trùng GV; **GVHD (kể cả đồng hướng dẫn) không được tham gia**.
3. Mỗi thành viên chấm **1 điểm 0–10, tối đa 1 chữ số thập phân**, kèm nhận xét; sửa được đến khi công bố.
4. **Chỉ chủ tịch** được tổng hợp; chỉ tổng hợp khi **tất cả thành viên đã chấm**; điểm trung bình = trung bình cộng, **làm tròn 2 chữ số**.
5. **DEAN công bố** kết quả (mỗi kết quả 1 lần); sau khi công bố **khóa điểm** (không sửa, không tổng hợp lại).
6. Xem kết quả: **SV trong nhóm** của đề tài và **GVHD** của đề tài; người khác không thấy.

### 19.3. Kiểm thử

- `mvn clean package`: BUILD SUCCESS, `Tests run: 87` (15 test mới).
- Chạy thật (GV001 = DEAN làm chủ tịch, GV003 thư ký, GV004 thành viên; GV002 là GVHD):

| Ca | Kết quả |
|---|---|
| DEAN lập hội đồng | 302, `/dean/councils` hiện "HD1", 3 thành viên đúng vai trò |
| GV001/GV003/GV004 chấm 8.0 / 7.5 / 8.5 | lưu điểm, hiện trong bảng |
| GV001 "Tổng hợp điểm" | điểm trung bình **8.0** |
| DEAN "Công bố điểm" | trạng thái "Đã công bố 23/10/2026 23:21", form chấm ẩn |
| SV001 `/results` | thấy "De tai 7 — HD1 — 8.0" |
| SV002 (không thuộc nhóm) | "Chưa có kết quả nào được công bố" |
| GV002 (GVHD) | thấy 8.0 |
| GV003 (thành viên, không phải GVHD) | không thấy |

### 19.4. Giả định & giới hạn

1. Dữ liệu hội đồng/điểm vẫn **in-memory**, mất khi restart.
2. **1 hội đồng / 1 đề tài**; chưa gom nhiều đề tài vào một hội đồng, chưa có lịch/ngày bảo vệ, phòng bảo vệ.
3. Chưa có biên bản họp, nhận xét tổng hợp của chủ tịch, hay tệp đính kèm.
4. Chưa có API JSON, chưa xuất Excel/PDF bảng điểm.
5. Điểm nhập tối đa 1 chữ số thập phân; điểm trung bình làm tròn 2 chữ số.

## 20. Bước 8 — Kiểm thử toàn diện hệ thống (E2E Browser UI, Business Logic Tests, Security & Controllers)

### 20.1. Kiểm thử giao diện và tương tác người dùng (E2E Browser Subagent)
- **Môi trường**: Ứng dụng chạy trên port 8080 với dữ liệu seed ban đầu.
- **Kịch bản đã kiểm tra**:
  1. Truy cập trang chủ `/` và `/home`: hiển thị đúng danh mục chức năng theo quyền người dùng chưa đăng nhập.
  2. Đăng nhập sai: nhập sai mật khẩu tại `/login` → hiển thị banner thông báo lỗi màu đỏ *"Tài khoản hoặc mật khẩu không chính xác"*.
  3. Đăng nhập thành công với tài khoản Admin (`admin`/`123456`) → chuyển hướng tới `/home`, thanh điều hướng hiển thị tên tài khoản và menu quản trị.
  4. Quản lý tài khoản tại `/admin/accounts`:
     - Danh sách tải đầy đủ các tài khoản seed.
     - Mở form thêm mới tài khoản `/admin/accounts/new`.
     - Kiểm thử logic client-side JavaScript ẩn/hiện trường "Bộ môn": khi chọn loại "Sinh viên" trường bộ môn tự động ẩn và xóa giá trị; khi chuyển sang "Giảng viên" trường bộ môn tự động hiển thị lại.
     - Tạo tài khoản giảng viên mới: Mã `GV099`, Tên `TS. Tran Test`, Email `gv099@qldt.local`, Bộ môn `Khoa học máy tính`. Submit form thành công, chuyển hướng về danh sách và xuất hiện ngay tài khoản mới.
  5. Đăng xuất: Nhấn nút "Đăng xuất" trên navbar → chuyển hướng về `/home?logout` kèm thông báo xanh *"Bạn đã đăng xuất thành công"*, các menu quản trị bị ẩn.

### 20.2. Bộ kiểm thử tự động (Automated Test Suite)
Đã triển khai hệ thống unit test, integration test và mock MVC controller test kiểm tra toàn bộ logic nghiệp vụ, nâng tổng số bài test lên **231 tests**, kết quả **100% PASS (0 Failure, 0 Error, 0 Skipped)**:

| Phân lớp kiểm thử | Lớp kiểm thử (Test Class) | Số lượng test | Nội dung kiểm thử |
|---|---|---|---|
| Service Logic | `AccountServiceTest` & `AccountServiceExtraTest` | 28 tests | CRUD tài khoản, mã duy nhất, email duy nhất, khóa/mở khóa, đổi mật khẩu, bổ nhiệm trưởng bộ môn, chỉ 1 trưởng bộ môn/bộ môn, quyền đăng nhập |
| Service Logic | `RegistrationPeriodServiceTest` & `RegistrationPeriodServiceExtraTest` | 19 tests | Tạo đợt, validate thời gian GV/SV, điều kiện TLCN/KLTN, cập nhật, xóa, khóa đăng ký |
| Service Logic | `TopicServiceTest` & `TopicServiceExtraTest` | 31 tests | Tạo đề tài trong thời hạn GV, giới hạn tối đa đề tài/GV, trùng tên, duyệt đề tài, công bố đề tài, đề tài đa bộ môn |
| Service Logic | `TeamServiceTest` & `TeamServiceExtraTest` | 27 tests | Tạo nhóm, mời thành viên, phản hồi lời mời, giới hạn tối đa 3 SV, không mời SV đã có nhóm, rời nhóm, giải tán nhóm |
| Service Logic | `TopicRegistrationServiceTest` & `TopicRegistrationServiceExtraTest` | 19 tests | Đăng ký đề tài trong thời hạn SV, khóa đợt, trùng đề tài, GV duyệt đăng ký, hủy đăng ký, chỉ nhóm trưởng thao tác |
| Service Logic | `ReportServiceTest` & `ReportServiceExtraTest` | 16 tests | Nộp báo cáo PDF/ZIP/DOCX, kiểm tra hạn nộp, chặn tệp sai định dạng hoặc > 20MB, cập nhật phiên bản (v1, v2), tải tệp |
| Service Logic | `CouncilServiceTest` & `CouncilServiceExtraTest` | 24 tests | Lập hội đồng 3-5 GV, 1 chủ tịch, 1 thư ký, GVHD không được tham gia, chấm điểm 0-10, tổng hợp điểm, công bố kết quả, khóa điểm sau công bố |
| Service Logic | `CustomUserDetailsServiceTest` | 5 tests | Tải UserDetails theo username, kiểm tra username không tồn tại, tài khoản bị khóa, ánh xạ roles và authorities |
| Integration | `FullProjectLifecycleIntegrationTest` | 1 test | Kiểm thử toàn bộ vòng đời dự án: Tạo tài khoản → Mở đợt → GV đăng ký đề tài → Dean duyệt/công bố → SV lập nhóm → Đăng ký đề tài → GV duyệt nhóm → SV nộp báo cáo → Lập hội đồng → Chấm điểm → Tổng hợp điểm → Công bố kết quả |
| Controller & Security | `SecurityAccessTest` | 16 tests | Kiểm tra bảo mật URL: truy cập công khai `/`, `/home`, `/login`; chặn 401/302 người dùng chưa đăng nhập; chặn 403 khi sai vai trò trên các endpoint admin/dean/lecturer/student |
| Controller & Security | `AccountAdminControllerTest` | 9 tests | MVC Controller: xem danh sách, hiển thị form, validate lỗi, tạo mới, khóa/mở khóa, đặt lại mật khẩu |
| Controller & Security | `RegistrationPeriodControllerTest` | 8 tests | MVC Controller: xem danh sách, form tạo/sửa, validate thời gian, cập nhật, xóa đợt |
| Controller & Security | `TopicControllerTest` | 8 tests | MVC Controller: xem đề tài của tôi, form đề tài, công bố đề tài, phê duyệt đề tài của trưởng bộ môn |
| Controller & Security | `TeamControllerTest` | 7 tests | MVC Controller: danh sách nhóm, tạo nhóm, mời thành viên, đăng ký đề tài |
| Controller & Security | `CouncilControllerTest` | 6 tests | MVC Controller: danh sách hội đồng, form chấm điểm, tổng hợp điểm, lập hội đồng |
| Controller & Security | `ReportControllerTest` | 6 tests | MVC Controller: nộp báo cáo, danh sách báo cáo, tải file báo cáo |
| Sanity | `QldtApplicationTests` | 1 test | Khởi động ApplicationContext của ứng dụng |
| **Tổng cộng** | **17 test classes** | **231 tests** | **Tất cả vượt qua (BUILD SUCCESS, 0 failures, 0 errors)** |



## 21. Bước 8 — JPA + MySQL

### 20.1. Instance MySQL riêng (không dùng root của máy)

- Tạo instance mới, độc lập với service MySQL80 có sẵn: `mysqld --initialize-insecure --datadir=db\data`, chạy ở **port 3307**.
- `db/init.sql`: tạo database `qldt`, `qldt_test`; tạo **`qldt_admin`** (quản trị instance) và **`qldt_app`** (tài khoản app dùng); **xóa `root`** của instance mới.
- Ứng dụng kết nối bằng `qldt_app`, không dùng root.
- `db/start-db.ps1` / `db/stop-db.ps1` để bật/tắt instance.

### 20.2. File mới/cập nhật

| File | Vai trò |
|---|---|
| `repository/*Repository.java` (9 file) | `Account`, `Lecturer`, `Student`, `RegistrationPeriod`, `Topic`, `Team`, `Report`, `Council`, `TopicResult` |
| `model/*.java` | Thêm `@Entity`, `@Table`, `@Id/@GeneratedValue`, quan hệ `@ManyToOne/@ManyToMany/@OneToOne/@ElementCollection`; `Account` dùng `@Inheritance(JOINED)` + `@DiscriminatorColumn` |
| `service/*.java` | Bỏ `ArrayList`/`AtomicLong`, dùng repository + `@Transactional`; nghiệp vụ giữ nguyên |
| `config/SecurityConfig.java` | Seed dữ liệu mẫu **idempotent** (`existsByUsername`) |
| `src/main/resources/application.properties` | DataSource MySQL 3307 (`qldt_app`), `ddl-auto=update`, `open-in-view=false` |
| `src/test/resources/application-test.properties` | H2 in-memory `MODE=MySQL`, `ddl-auto=create-drop` |
| `pom.xml` | Thêm `com.h2database:h2` (test scope) |
| `db/init.sql`, `db/start-db.ps1`, `db/stop-db.ps1` | Tạo/khoi động instance MySQL riêng |
| `src/test/java/project/qldt/support/Beans.java` | Cầu nối lấy bean trong test |

### 20.3. Bảng dữ liệu

`account` (+ `lecturer`, `student`, `admin`, `account_role`), `registration_period`, `topic`, `topic_advisor`, `advisor_invitation`, `team`, `team_member`, `team_invitation`, `report`, `council`, `council_member`, `topic_result`.

### 20.4. Kiểm thử

- `mvn clean package`: BUILD SUCCESS, **231 test** pass.
- Test chạy trên H2 (`MODE=MySQL`) với profile `test`, mỗi test trong transaction rollback; seed tài khoản mẫu dùng cho test controller, test service dùng mã riêng `GV9xx`/`SV9xx`.
- Chạy thật trên MySQL:

| Ca | Kết quả |
|---|---|
| Khởi động app | tạo 12 bảng, seed 1 admin + 5 GV + 2 SV |
| Tạo đợt → đề tài → duyệt → công bố → nhóm → mời/tham gia → đăng ký → duyệt | OK |
| Lập hội đồng → chấm 8.0/7.5/8.5 → tổng hợp 8.0 → công bố | OK |
| SV và GVHD xem kết quả | OK |
| Nộp báo cáo PDF | 302, bảng `report` có `bc-jpa.pdf` v1 |
| **Restart app** rồi xem lại hội đồng/điểm | dữ liệu **vẫn còn** (HD-JPA, 8.0, đã công bố) |

### 20.5. Giả định & giới hạn

1. Dùng `ddl-auto=update` (chưa dùng Flyway/Liquibase).
2. Mật khẩu DB nằm trong `application.properties` (phục vụ bài lab).
3. Instance MySQL chạy thủ công bằng script, chưa cài thành Windows service.
4. Quan hệ dùng `EAGER` để template đọc được khi `open-in-view=false`; dữ liệu nhỏ nên chưa tối ưu N+1.
5. Tệp báo cáo vẫn lưu trên đĩa, chỉ metadata nằm trong DB.


## 22. Bước 9 — Thống kê & xuất kết quả

### 22.1. Chuẩn hoá định dạng code

- Thêm `config/eclipse-formatter.xml` (tab = 4, lineSplit 120) và plugin `formatter-maven-plugin` trong `pom.xml`.
- Chạy `mvn formatter:format` để format toàn bộ `src/main` + `src/test` (99 → 106 file), `mvn formatter:validate` để kiểm tra.
- Từ nay code mới đều được format lại bằng plugin này trước khi kết thúc bước.

### 22.2. File mới/cập nhật

| File | Vai trò |
|---|---|
| `dto/PeriodStatistics.java` | Số liệu tổng hợp của một đợt |
| `dto/TopicStatisticsRow.java` | Một dòng thống kê theo đề tài |
| `dto/StatisticsResponse.java` | Gói JSON cho API |
| `service/StatisticsService.java` | Tính số liệu, tạo các dòng, xuất CSV |
| `controller/StatisticsController.java` | `/dean/statistics`, `/dean/statistics/export` |
| `controller/StatisticsApiController.java` | `/api/statistics` (JSON) |
| `templates/statistics/index.html` | Trang thống kê + chọn đợt + link xuất CSV |
| `templates/home.html` | Menu "Thống kê & xuất kết quả" cho DEAN |
| `src/test/.../StatisticsServiceTest.java` | 5 test |

### 22.3. Nghiệp vụ (`StatisticsService`)

1. Thống kê theo **đợt**: tổng đề tài, số đã công bố, số đã có nhóm (`REGISTERED`), số nhóm, số báo cáo đã nộp, số hội đồng, số kết quả đã công bố.
2. **Điểm trung bình** = trung bình các kết quả **đã công bố**, làm tròn 2 chữ số; đợt chưa có kết quả → 0.
3. Mỗi dòng: đề tài, GVHD, nhóm trưởng, số thành viên, tệp báo cáo, hội đồng, điểm, trạng thái công bố.
4. Xuất **CSV** (UTF-8 có BOM để Excel đọc đúng tiếng Việt), có escape dấu phẩy/nháy.
5. API JSON `/api/statistics?periodId=` trả `summary` + `rows`.
6. Chỉ **DEAN** xem thống kê/API/cSV; giảng viên thường bị chặn.

### 22.4. Kiểm thử

- `mvn clean package`: BUILD SUCCESS, **236 test** pass (5 test mới cho `StatisticsService`).
- Chạy thật trên MySQL (đợt "Dot JPA" đã có đủ dữ liệu):

| Ca | Kết quả |
|---|---|
| `GET /dean/statistics?periodId=1` | 200, hiện card thống kê, đề tài "De tai JPA", điểm **8.0** |
| `GET /dean/statistics/export?periodId=1` | 200, CSV có tiêu đề + dòng dữ liệu + điểm 8.0 |
| `GET /api/statistics?periodId=1` | 200, JSON có `summary`, `rows`, điểm 8.0 |
| GV002 (không phải DEAN) gọi trang/API thống kê | **bị chặn** |

### 22.5. Giả định & giới hạn

1. Xuất **CSV** (mở bằng Excel) chứ không phải `.xlsx` thật.
2. Số liệu tính trực tiếp từ DB mỗi lần xem, chưa cache và chưa có biểu đồ.
3. API chỉ dành cho DEAN, chưa có phiên bản public.
4. Chưa thống kê theo khoa/bộ môn hay theo học kỳ.


## 23. Khai báo thuộc tính `app.*` (hết cảnh báo "unknown property")

- Thêm `config/AppProperties.java`: `@ConfigurationProperties(prefix = "app")` với thuộc tính `uploadDir` (mặc định `uploads`) và `uploadPath()`.
- `QldtApplication` thêm `@ConfigurationPropertiesScan` để Spring đăng ký lớp cấu hình.
- `ReportService` nhận `AppProperties` thay cho `@Value("${app.upload-dir:uploads}")`.
- `pom.xml` thêm `spring-boot-configuration-processor` (optional) để sinh `META-INF/spring-configuration-metadata.json`.
- Kết quả: file metadata có `"name": "app.upload-dir"` gắn với `project.qldt.config.AppProperties` → IDE hết cảnh báo.
- Đã kiểm tra: `mvn formatter:format` + `mvn clean package` → **236 test pass**, app khởi động bình thường.


## 24. Bước 10 — Thông báo & nhắc hạn

### 24.1. File mới/cập nhật

| File | Vai trò |
|---|---|
| `model/Notification.java` | Thông báo: người nhận, loại, tiêu đề, nội dung, liên kết, thời điểm, đã đọc |
| `model/NotificationType.java` | `INVITATION`, `APPROVAL`, `REJECTION`, `RESULT`, `REMINDER` |
| `repository/NotificationRepository.java` | Truy vấn theo người nhận, đếm chưa đọc, chống trùng tiêu đề |
| `service/NotificationService.java` | Gửi, danh sách, đếm chưa đọc, đánh dấu đã đọc (kiểm tra đúng chủ) |
| `service/ReminderService.java` | Nhắc hạn nộp báo cáo, nhắc lịch báo cáo hội đồng, chống gửi trùng |
| `controller/NotificationController.java` | `/notifications`, `POST /{id}/read`, `POST /read-all` |
| `controller/ReminderAdminController.java` | `/dean/reminders` + 2 nút gửi nhắc (DEAN) |
| `controller/NotificationAdvice.java` | Thêm `unreadNotifications` cho mọi trang (badge ở menu) |
| `templates/notifications/list.html`, `templates/reminders/index.html` | Giao diện |
| `templates/home.html` | Menu "Thông báo (N)" + "Gửi nhắc hạn" |
| `exception/InvalidNotificationException.java`, `NotificationNotFoundException.java` | Lỗi nghiệp vụ |
| `TeamService`, `TopicService`, `TopicRegistrationService`, `ReportService`, `CouncilService` | Gửi thông báo tại các mốc nghiệp vụ |
| `src/test/.../NotificationServiceTest.java` | 10 test |

### 24.2. Nghiệp vụ

| Sự kiện | Người nhận |
|---|---|
| Mời vào nhóm | Sinh viên được mời |
| Phản hồi lời mời nhóm | Nhóm trưởng |
| Mời làm GVHD | Giảng viên được mời |
| Nhóm đăng ký đề tài | Các GVHD của đề tài |
| Duyệt / từ chối đăng ký | Nhóm trưởng |
| Nộp báo cáo | Các GVHD |
| Công bố điểm bảo vệ | Thành viên nhóm + GVHD |
| Nhắc hạn nộp báo cáo | Thành viên nhóm chưa nộp |
| Nhắc lịch báo cáo hội đồng | Thành viên hội đồng |

- Mỗi người chỉ nhận **1 thông báo cho mỗi tiêu đề** (chống trùng khi bấm nhắc nhiều lần).
- Chỉ chủ sở hữu mới đánh dấu đã đọc được thông báo của mình.
- Menu hiển thị số thông báo chưa đọc cho mọi tài khoản đã đăng nhập.

### 24.3. Kiểm thử

- `mvn clean package`: BUILD SUCCESS, **246 test** pass (10 test mới).
- Chạy thật trên MySQL:

| Ca | Kết quả |
|---|---|
| SV001 mời SV002 vào nhóm | SV002 vào `/notifications` thấy "Lời mời vào nhóm", chưa đọc = 1 |
| GVHD duyệt đăng ký | SV001 thấy "Đăng ký đề tài được duyệt" |
| DEAN gửi nhắc hạn nộp báo cáo | SV001 thấy "Nhắc hạn nộp báo cáo đợt Dot Thong Bao", chưa đọc = 2 |
| Bấm gửi nhắc lần 2 | không sinh thêm thông báo (chống trùng) |
| Menu trang chủ | hiện "Thông báo (2)" |

### 24.4. Giả định & giới hạn

1. Thông báo **trong hệ thống**, chưa gửi email/SMS.
2. Nhắc hạn do DEAN bấm gửi, chưa có lịch tự động (`@Scheduled`).
3. Bảng `notification` mới được thêm nên các sự kiện cũ trước Bước 10 không có thông báo.
4. Instance MySQL local dùng chứng chỉ tự ký nên cấu hình `useSSL=false` cho kết nối nội bộ (xem mục 25).


## 25. Cấu hình JDBC, dọn thư mục `db`, xuất Excel

### 25.1. Cấu hình kết nối

`spring.datasource.url=jdbc:mysql://127.0.0.1:3307/qldt?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC`

- `createDatabaseIfNotExist=true`: ứng dụng **tự tạo database** nếu chưa có (đã test: drop database → chạy app → database + 17 bảng + dữ liệu mẫu tự sinh).
- `useSSL=false`: instance MySQL local dùng chứng chỉ tự ký nên tắt SSL cho kết nối nội bộ.
- `serverTimezone=UTC`.

### 25.2. Dọn thư mục `db` và script reset

- **Đã xóa** thư mục `db` (init.sql, start-db.ps1, stop-db.ps1, logs, data).
- Thư mục dữ liệu MySQL chuyển ra ngoài project: `%LOCALAPPDATA%\qldt-mysql\data` (không commit vào source).
- `scripts/start-db.ps1`: bật instance MySQL port 3307 với datadir mới.
- `scripts/reset-db.ps1`: `DROP DATABASE` + tạo lại database `qldt` (utf8mb4), xóa các tệp trong `uploads`; sau đó chạy lại app để tạo bảng và seed.

Cách dùng:

```powershell
.\scripts\start-db.ps1
.\scripts\reset-db.ps1
mvn spring-boot:run
```

### 25.3. Xuất Excel (thay cho email)

- **Bỏ email/SMTP** theo yêu cầu (không cần cấu hình server, phù hợp deploy cloud).
- Thêm `org.apache.poi:poi-ooxml:5.3.0`.
- `StatisticsService.exportXlsx(periodId)`: tạo workbook với tiêu đề in đậm, tự giãn cột, ghi điểm dạng số.
- `GET /dean/statistics/export-xlsx?periodId=...` trả tệp `.xlsx`; trang thống kê có thêm link **Xuất Excel** (giữ nguyên Xuất CSV).

### 25.4. Kiểm thử

- `mvn clean package`: BUILD SUCCESS, **247 test** pass (thêm 1 test đọc lại file `.xlsx` để kiểm tra tiêu đề và điểm).
- Chạy thật:

| Ca | Kết quả |
|---|---|
| `/dean/statistics?periodId=1` | 200, có link `export-xlsx` |
| `/dean/statistics/export-xlsx?periodId=1` | 200, `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, 3639 bytes, magic bytes `PK` (file zip hợp lệ) |
| `/dean/statistics/export?periodId=1` | 200 (CSV vẫn hoạt động) |

### 25.5. Lưu ý khi deploy

1. Đổi `spring.datasource.*` bằng biến môi trường (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).
2. Nếu DB cloud yêu cầu SSL, bỏ `useSSL=false` (hoặc đặt `sslMode=REQUIRED`).
3. `ddl-auto=update` phù hợp demo; môi trường thật nên dùng migration (Flyway/Liquibase).


## 26. Bước 12 (phần 1) — Biểu đồ thống kê

### 26.1. File mới/cập nhật

| File | Vai trò |
|---|---|
| `dto/StatisticsChart.java` | Một thanh biểu đồ: nhãn, giá trị, phần trăm |
| `dto/StatisticsCharts.java` | 3 nhóm: trạng thái, tiến độ, điểm theo hội đồng |
| `service/StatisticsService.java` | Thêm `charts(periodId)` và `percent(value, total)` |
| `controller/StatisticsController.java` | Đưa `charts` vào model |
| `templates/statistics/index.html` | 3 khối biểu đồ + CSS thanh ngang |
| `src/test/.../StatisticsServiceTest.java` | Thêm test biểu đồ |

### 26.2. Cách làm

- Biểu đồ **render phía server bằng HTML/CSS** (thanh ngang, `width: %`), **không dùng Chart.js/CDN** → chạy được offline và khi deploy cloud không phụ thuộc mạng ngoài.
- **Biểu đồ 1 — Đề tài theo trạng thái**: 4 trạng thái (`Chờ duyệt`, `Đã duyệt`, `Bị từ chối`, `Đã công bố`), phần trăm trên tổng đề tài của đợt.
- **Biểu đồ 2 — Tiến độ trong đợt**: đề tài đã công bố, đã có nhóm, số nhóm, báo cáo đã nộp, hội đồng, kết quả đã công bố; phần trăm trên giá trị lớn nhất.
- **Biểu đồ 3 — Điểm trung bình theo hội đồng** (thang 10): độ dài thanh = điểm × 10, màu xanh lá.

### 26.3. Kiểm thử

- `mvn clean package`: BUILD SUCCESS, **248 test** pass.
- Chạy thật (đợt có 1 đề tài đã công bố):

| Ca | Kết quả |
|---|---|
| `GET /dean/statistics?periodId=1` | 200, có khối `charts`, **3 biểu đồ**, **10 thanh** (4 trạng thái + 6 tiến độ), có thanh **100.0%** |
| `GET /dean/statistics/export-xlsx?periodId=1` | 200, 3718 bytes |

### 26.4. Giới hạn

1. Chưa có biểu đồ theo thời gian (theo tuần/tháng) hay so sánh nhiều đợt.
2. Chưa xuất biểu đồ thành ảnh/PDF.
3. Màu và kích thước cố định trong CSS, chưa có bộ lọc tuỳ chọn.
4. Số liệu tính lại mỗi lần mở trang, chưa cache.


## 27. Xác nhận mật khẩu khi tạo tài khoản

### 27.1. Thay đổi

| File | Nội dung |
|---|---|
| `dto/AccountRequest.java` | Thêm `confirmPassword` với `@NotBlank(message = "Vui lòng xác nhận mật khẩu")` và `@Size(max = 50)` |
| `service/AccountService.java` | `createAccount()` gọi `validatePasswords()`: mật khẩu không trống, ≥ 6 ký tự, có xác nhận, và **phải khớp** (không khớp → `InvalidAccountException("Mật khẩu xác nhận không khớp")`) |
| `templates/admin/accounts/form.html` | Thêm ô **Xác nhận mật khẩu** (`type=password`, `th:field="*{confirmPassword}"`, `th:errors`) |
| `AccountServiceExtraTest` | +3 test: không khớp, thiếu xác nhận, khớp thì tạo được |
| `AccountAdminControllerTest`, `SecurityAccessTest` | Bổ sung param `confirmPassword` cho các POST tạo tài khoản |

### 27.2. Kiểm chứng

- `mvn clean package`: BUILD SUCCESS, **251 test** pass.
- Chạy thật:

| Ca | Kết quả |
|---|---|
| `GET /admin/accounts/new` | 200, form có ô `confirmPassword` |
| POST tạo tài khoản với mật khẩu `123456`, xác nhận `654321` | 200 (ở lại form), hiện lỗi, **không tạo** tài khoản |
| POST với xác nhận `123456` | Tạo thành công, chuyển về `/admin/accounts`, tài khoản xuất hiện trong danh sách |

### 27.3. Lưu ý

1. Chỉ luồng **tạo tài khoản qua form admin** yêu cầu xác nhận; các hàm nội bộ `createAdmin/createLecturer/createStudent` (seed dữ liệu mẫu, test) không yêu cầu.
2. Kiểm tra khớp mật khẩu nằm ở **service** (không chỉ ở form) để tránh bypass.
3. `confirmPassword` không lưu vào DB.


## 28. DEAN hủy đăng ký đề tài + nộp báo cáo mọi định dạng

### 28.1. DEAN hủy đăng ký đề tài (xử lý ngoại lệ)

| File | Nội dung |
|---|---|
| `service/TopicRegistrationService.java` | Thêm `cancelByDean(topicId, reason)`: bắt buộc lý do, phải có nhóm đăng ký, đưa đề tài về `AVAILABLE`, gỡ đề tài khỏi nhóm, lưu lý do, gửi thông báo cho sinh viên + GVHD |
| `controller/DeanRegistrationController.java` | `/dean/registrations` (danh sách đăng ký) và `POST /{topicId}/cancel`, chỉ DEAN |
| `templates/registrations/dean.html` | Bảng đăng ký + ô nhập lý do + nút **Hủy đăng ký** |
| `templates/home.html` | Menu **Hủy đăng ký đề tài** cho DEAN |
| `src/test/.../RegistrationCancelTest.java` | 5 test: hủy khi chờ duyệt, hủy khi đã duyệt, thông báo, thiếu lý do, đề tài chưa có nhóm |

### 28.2. Nộp báo cáo mọi định dạng

- **Nguyên nhân lỗi** "đã chọn file mà vẫn báo chưa chọn tệp": input file có `accept=".pdf,.doc,.docx"` nên trình duyệt **loại bỏ** các tệp khác → form gửi lên rỗng → service báo *"Vui lòng chọn tệp báo cáo"*.
- Bỏ `accept` trong `templates/teams/list.html`, thêm gợi ý "Chấp nhận mọi định dạng tệp, tối đa 10MB".
- `ReportService`: bỏ whitelist `ALLOWED_EXTENSIONS`; vẫn chặn tệp rỗng và tệp > 10MB.
- Cập nhật test: `nopDuocTepBatKyDinhDang` (`.txt`), `chapNhanMoiDinhDangTep` (`txt`, `zip`, không phần mở rộng).

### 28.3. Kiểm chứng

- `mvn clean package`: BUILD SUCCESS, **256 test** pass.
- Chạy thật trên MySQL:

| Ca | Kết quả |
|---|---|
| Trang nhóm | không còn `accept`, có gợi ý định dạng |
| Nộp tệp `.txt` | 302, trang nhóm hiện tên tệp vừa nộp |
| `GET /dean/registrations` | 200, có ô nhập lý do + nút hủy |
| DEAN hủy đăng ký | 200, đề tài về **AVAILABLE**, `team.topic = NULL`, lưu lý do, **3 thông báo** cho 2 SV + GVHD |
| Khôi phục dữ liệu sau kiểm thử | đặt lại `REGISTERED`, `team.topic_id = 1`, xóa báo cáo test |

### 28.4. Lưu ý

1. Hủy đăng ký **không xóa nhóm**; nhóm có thể đăng ký lại đề tài khác nếu còn trong thời gian sinh viên đăng ký.
2. DEAN hủy được cả đăng ký **đang chờ duyệt** và **đã duyệt**.
3. Tệp báo cáo vẫn giới hạn **10MB**, lưu tên gốc và tệp `team-{id}.{ext}` trên đĩa.
4. File `.zip/.rar`… nộp được nhưng người tải về cần phần mềm tương ứng để mở.


## 29. Sửa hiển thị báo cáo sau khi nộp

### 29.1. Nguyên nhân

1. `input type="file"` có `accept=".pdf,.doc,.docx"` → trình duyệt **loại bỏ** tệp khác, form gửi lên rỗng, service báo *"Vui lòng chọn tệp báo cáo"*.
2. Khối hiển thị báo cáo nằm **bên trong** điều kiện `availability == REGISTERED`, nên khi trạng thái đổi (hoặc trình duyệt hiển thị trang cũ) thì không thấy `v.report`.
3. Trang `/teams` không chặn cache trình duyệt → bấm upload xong quay lại trang cũ (vẫn `v.report = null`).

### 29.2. Đã sửa

| File | Thay đổi |
|---|---|
| `templates/teams/list.html` | Bỏ `accept`; khối **Báo cáo** chuyển ra ngoài điều kiện `REGISTERED` (chỉ cần có đề tài); thêm `title="Mã báo cáo: ..."` để đối chiếu DB; thêm `<meta http-equiv="Cache-Control" content="no-store">` |
| `controller/TeamController.java` | Sau khi nộp, flash message kèm **tên tệp + phiên bản**: *"Đã nộp báo cáo: <tên> (phiên bản N)"* |
| `service/ReportService.java` | Sau `save` gọi `flush()` và **kiểm tra lại** `findByTeamId`; nếu không thấy bản ghi thì ném lỗi rõ ràng thay vì im lặng |

### 29.3. Kiểm chứng (chạy thật trên MySQL)

| Ca | Kết quả |
|---|---|
| Nộp `bao-cao-v3.txt` | 302; DB `report` có `original_file_name = bao-cao-v3.txt`, `version = 3` |
| Trang `/teams` sau khi nộp | hiện **Báo cáo: bao-cao-v3.txt (v3)**, kèm tooltip "Mã báo cáo: …" |
| Flash message | "Đã nộp báo cáo: bao-cao-v3.txt (phiên bản 3)" |
| Header trang | có `Cache-Control: no-store` |
| `mvn clean package` | BUILD SUCCESS, **256 test** pass |

### 29.4. Cách kiểm tra nếu vẫn thấy null

1. **Dừng app cũ** (port 8080) rồi chạy lại bản mới: `mvn spring-boot:run` hoặc `java -jar target\qldt-0.0.1-SNAPSHOT.jar`.
2. Nhấn `Ctrl+F5` để bỏ cache trang `/teams`.
3. Kiểm tra DB: `select * from report;` (phải có dòng với `team_id` của nhóm).
4. Nếu DB có dòng mà trang vẫn trống: xem tooltip "Mã báo cáo" — nếu không có tooltip nghĩa là trang đang là bản cũ.


## 30. Sửa lỗi tải báo cáo bị đặt tên "download"

### 30.1. Nguyên nhân

- `Content-Disposition` chỉ gửi `filename="<tên gốc>"` dạng thô. Khi tên tệp có **ký tự ngoài ASCII (tiếng Việt)**, giá trị header không hợp lệ theo chuẩn HTTP → trình duyệt bỏ qua tên, lấy đoạn cuối URL (`/reports/{id}/download`) làm tên tệp → lưu thành **`download`**.
- Đã kiểm chứng: tên ASCII (`bao-cao-nhom.pdf`) cho header đúng; tên `Báo cáo nhóm 1.pdf` trước khi sửa chỉ có `filename="..."` (không có `filename*`).

### 30.2. Đã sửa

| File | Thay đổi |
|---|---|
| `controller/ReportController.java` | Dùng `ContentDisposition.attachment().filename(name, StandardCharsets.UTF_8)` → header chuẩn **RFC 5987**: `filename="..."; filename*=UTF-8%27%27...`; thêm `downloadFileName()` trả `bao-cao-{id}{ext}` khi tên gốc rỗng |
| `ReportControllerTest` | Test tải báo cáo kiểm tra header chứa `attachment`, `filename*=UTF-8'` và đúng tên tệp |

### 30.3. Kiểm chứng

Header sau khi sửa (tệp `Báo cáo nhóm 1.pdf`):

```
HTTP/1.1 200
Content-Disposition: attachment; filename="Bao cao nhom 1.pdf";
  filename*=UTF-8%27%27B%C3%A1o%20c%C3%A1o%20nh%C3%B3m%201.pdf
Content-Type: application/octet-stream
Content-Length: 11
```

- `mvn clean package`: BUILD SUCCESS, **256 test** pass.
- Trình duyệt sẽ lưu đúng tên **Báo cáo nhóm 1.pdf** (nhờ `filename*`).

### 30.4. Lưu ý

1. Chỉ **giảng viên hướng dẫn** của đề tài được tải báo cáo; người khác bị chặn kèm thông báo.
2. Nếu tệp trên đĩa bị xoá, hệ thống báo *"Tệp báo cáo không còn trên máy chủ"*.
3. Nếu vẫn nhận tệp tên `download`: **dừng app cũ trên port 8080** và chạy lại bản mới, sau đó `Ctrl+F5` trang `/reports`.


## 31. Hiển thị thành viên hội đồng gộp vai trò và điểm

- `templates/councils/list.html`: ô "Giảng viên" đổi thành `|${m.lecturer.fullName} - ${m.lecturer.lecturerCode} (${m.role.label}): ${m.score != null ? m.score : '}|`.
- Kiểm chứng trên dữ liệu thật (hội đồng 1): hiển thị `Giảng viên CNTT - GV002 (Chủ tịch): 10`; thành viên chưa chấm hiển thị `Giảng viên 3 - GV003 (Thư ký):` (điểm để trống).
- Hai cột "Vai trò" và "Điểm" phía sau vẫn giữ nguyên (nếu muốn bỏ để tránh trùng lặp thì báo để sửa).
- Sửa luôn `templates/councils/dean.html`: biểu thức cũ bị sai (`${m.lecturerCode}`, thiếu `m.lecturer.`) làm trang `/dean/councils` lỗi 500 — nay dùng cùng định dạng `Họ tên - Mã (Vai trò): Điểm`; đã kiểm tra `/dean/councils` và `/councils` đều hiển thị đúng.

