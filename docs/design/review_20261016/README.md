# ST311 — Đối chiếu giao diện ngày 16/10/2026

Ảnh nguồn: 7 bảng PNG trong `/home/lequyen/Documents/folder311`.
Không sửa Splash, Language, Intro và Permission. ActionBar vẫn dùng chung,
nhưng Profile có kiểu tiêu đề căn trái riêng, không đổi kiểu của onboarding.

## Xem ảnh chạy mới

Ảnh chụp native trên emulator 720×1600, density 320 dpi; không phải ảnh dựng bằng code.

| Nhóm | Ảnh kiểm tra |
| --- | --- |
| Chọn cung / tạo hồ sơ | [Chọn cung](current/profile_step_1_scorpio.png), [Hồ sơ](current/profile_step_2.png) |
| Home | [Chưa có lịch sử](current/home_empty.png), [Có lịch sử](current/home_recent.png) |
| Settings / bottom sheet | [Settings](current/settings.png), [Tên](current/settings_edit_name.png), [Ngày sinh](current/settings_edit_birthday.png), [Cung](current/settings_edit_sign.png) |
| Spreads / chọn bài | [Danh sách](current/spreads.png), [Hướng dẫn xáo bài](current/guidance.png), [Scroll](current/selection_scroll.png), [Classic](current/selection_classic.png) |
| Kết quả | [Daily](current/result_daily.png), [Weekly](current/result_weekly.png), [Quick](current/result_quick.png), [Yes/No](current/result_yes_no.png), [Universe](current/result_universe.png), [Celtic](current/result_celtic.png) |
| Cards | [List](current/cards_list.png), [Grid](current/cards_grid.png), [Chi tiết](current/cards_detail.png) |
| History | [Trống](current/history_empty.png), [Có dữ liệu](current/history_data.png) |
| Horoscope | [Bố cục khi chưa có dữ liệu](current/horoscope_unavailable.png) |

Mỗi PNG có JSON cùng tên ghi tọa độ view, cỡ chữ, chiều cao dòng, viewport,
trạng thái tải ảnh và trạng thái màn hình. `current/manifest.json` ghi toàn bộ
kết quả và kiểm tra khôi phục preferences/không thay đổi dữ liệu đọc bài.

## Các mốc đã căn theo ảnh

- Home: khối Horoscope `(40,314)-(680,714)`; Quick Actions bắt đầu tại `y=796`.
- Profile: dải bước tại `y=1032`; nút Next/Previous tại `y=1100`.
- Icon cung: bỏ cả phần mép pha trắng; không phóng to ký hiệu để che viền.
- Cards: item list 108dp, cách 8dp; grid 3 cột, tỷ lệ ảnh 4:7.
- History: item 108dp, cách 12dp; ảnh thu nhỏ 56dp, nhóm lá căn giữa.
- Spreads: item 64dp, cách 12dp; chọn bài tính kích thước từ chiều rộng thực.
- Ngày sinh: 3 ô trắng, khoảng cách 6dp, vùng chọn 40dp và viền vàng.
- Hướng dẫn xáo bài: bottom sheet, nút Continue xanh bo tròn; không dùng hộp thoại giữa màn.
- Bottom sheet hướng dẫn cuối nằm tại `(0,224)-(720,1600)`; pixel chính giữa mép
  dưới màu trắng, không còn vùng hở do padding navigation bar.
- Luận bài: giãn dòng 21sp và căn đều; hướng dẫn xáo bài giữ nhịp dòng 16sp như mẫu.

## Phạm vi kiểm chứng và phần chưa giống tuyệt đối

Build debug, 38 JVM test và 25 Android test đều đạt trên bản cuối, gồm chụp
27 ảnh native và kiểm tra không còn padding thanh điều hướng dưới bottom sheet.
`current/manifest.json` xác nhận preferences được khôi phục và dữ liệu đọc bài
không thay đổi. Test chạy không đồng nghĩa so sánh pixel tự động toàn màn đã đạt.

Không tuyên bố toàn bộ màn hình giống 100% từng pixel. Các khác biệt còn lại:

- Ảnh hệ thống/status bar, nét chữ và NumberPicker phụ thuộc Android/thiết bị.
- Ảnh lá bài hiện lấy theo JSON/Glide, không phải toàn bộ cùng bản artwork trong design.
- Chưa có dữ liệu Horoscope, lời giải kết hợp hai lá và ý nghĩa đảo ngược trong JSON;
  giữ nhãn thiếu nội dung, không bịa dự đoán để làm ảnh giống mẫu.
- Giữ nút View Reading xác nhận lựa chọn và các cảnh báo kiểm tra dữ liệu.
- Texture trang trí Spreads mới có phần artwork trích sẵn, chưa khớp toàn bộ nền.
- Một số màn dài chỉ chụp viewport đầu/viewport bổ sung; JSON ghi phần chưa chụp hết.

Không cài bản này lên điện thoại thật. Chưa kiểm chứng release/R8 hoặc quảng cáo.
