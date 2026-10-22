1. Tất cả tài liệu tham khao được đặt trong thư mục `documentation`

2. Khi code không được comment vào trong code mà phải note vào 1 File `NOTE.md` ở thư mục gốc để giải thích

3. Được phép tra cứu trên mạng để đọc thêm tài liệu, note rõ lại nguồn tài liệu tham khảo vào `NOTE.md`

4. Khi dùng bất khì anotation gì cũng phải note lại: anotation đó có tác dụng là gì, nếu không dùng nó thì phải code như thế nào

5. Nếu không rõ, hoặc không biết thông tin gì thì hãy hỏi lại tôi, không được phép tự ý triển khai 

6. Code phải ngắn gọn, đơn giản, dễ hiểu

7. Không được mắc các lỗi sai được liệt kê trong `Các lỗi sai thường gặp.md`



# Luồng triển khai như sau:
- Luồng tổng quát: Triển khai backend -> Triển khai frontend -> Deploy lên cloud. 
- Backend: triển khai từng module, tính năng từ từ, không được triển khai hết tất cả từ ban đầu (Tham khảo các slide triển khai trong thư mục `LabSlide`), triển khai theo chuẩn mô hình MVC của spring boot
- Về phần database tôi muốn dùng MySQL, nhưng trước khi triển khai JPA thì phải hoàn thành triển khai dùng bộ nhớ cua brower trước
- Mỗi bước cần phải có sự xác nhận của tôi thì mới được sang bước tiếp theo
