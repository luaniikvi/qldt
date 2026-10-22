# Những Lỗi sai thường gặp phải tránh

- Tầng service hoàn toàn không chứa nghiệp vụ kiểm tra chính sách đặt phòng hoặc các nghiệp vụ khác, đưa hết code nghiệp vụ business logic đặt ở tầng Controller -> tầng Controller phải biết quá nhiều logic nghiệp vụ, khó tái sử dụng business logic

- Tầng service gọi findAll() load hết dữ liệu từ DB lên RAM sau đó mới filter. Bản chất cách làm này gây ảnh hưởng đến hiệu năng (performance) của hệ thống. Ví dụ khi app có hàng triệu booking thì sao? Chỗ này nếu đúng phải viết câu custom SQL ở tầng Repository để filter ở tầng database vì sẽ tận dụng được index scan của DB. Cách làm hiện tại thì độ phức tạp sẽ là O(N) thay vì index scan DB.

- Có nhóm trong cùng một request tạo booking, tầng service gọi findAll() nhiều lần liên tiếp để kiểm tra từng điều kiện riêng biệt thay vì gộp query 1 lần (ngụy trang rất khéo bằng cách chia nhỏ function). Làm cách này thì sao RAM chịu nổi?

- Dùng service nửa mùa. Service làm 1 nửa, 1 nửa lại đá trách nhiệm qua Controller.

- Có nhóm lười đến nỗi còn không chịu xóa todo code của bài lab, để y nguyên rồi chèn code meeting room vào. Hai bài này là khác nhau mà.

- Service trả về chuỗi string thuần túy (vd: "Cannot cancel booking less than 30 minutes before start time.") để biểu diễn thông báo lỗi thay vì throw domain exception (vd: BookingNotFoundException, BookingCancellationException) hoặc trả về Result Object.

- Nguy cơ Double-Booking khi xử lý đồng thời (lỗi này thầy nói thêm cho các nhóm biết chứ không trừ điểm lỗi này). Các nhóm chỉ kiểm tra count == 0 rồi mới thực hiện save(). Khi có 2 request gửi đồng thời cùng mili-giây: cả hai đều thấy phòng trống và cùng ghi vào DB, dẫn đến phòng bị trùng lịch thực tế (Double-Booking).

- Bài học có học @Transactional nhưng ít thấy nhóm dùng @Transactional cho các luồng lưu dữ liệu quan trọng (lỗi này thầy nói thêm cho các nhóm biết chứ không trừ điểm lỗi này).