# ActionBar đồng bộ — 16/10/2026

Yêu cầu mới này thay thế chiều cao/căn dọc ActionBar trong ảnh đối chiếu cũ.

- Mọi màn đang dùng ActionBar chung đều cao **70dp**, bao gồm Language và Permission.
- Title nằm trong vùng chung, mép dưới cách đáy ActionBar **10dp**.
- Title trái, title giữa và logo Home dùng chung vùng căn dọc.
- Các button và nhóm List/Grid có cùng mép trên/dưới với vùng title, không dịch Y riêng.
- Splash và Intro vốn không có ActionBar thì không thêm ActionBar mới.
- Giữ font, màu, vị trí ngang, listener và điều hướng hiện có.

Khai báo trong `values/actionbar_dimens.xml`, `values/actionbar_styles.xml` và
`layout/layout_actionbar.xml`. `actionBarTitleBounds` luôn giữ kích thước khi
một title chuyển sang GONE; vì thế nút không nhảy vị trí khi đổi màn.
70dp không bao gồm status bar/cutout; phần inset hệ thống vẫn do Activity xử lý.

Đã bỏ ghi đè Home 50dp / Tarot 40dp và các offset dọc riêng trong mã cũ.
16 layout include dùng cùng chiều cao; kiểm thử đo cạnh trên/dưới của tất cả
biến thể button ở ba theme và các chiều rộng 320/360/800dp.

Build debug, 38 unit test và 27 kiểm thử Android đều đạt. Ảnh/JSON kiểm chứng:
[Home](actionbar_70dp/home_empty.png), [Settings](actionbar_70dp/settings.png),
[Cards](actionbar_70dp/cards_grid.png), [Profile](actionbar_70dp/profile_step_1_scorpio.png).
Ở density 2×: ActionBar cao 140px; title/button cùng nằm tại `y=144..224`,
đáy ActionBar tại `y=244` (cách 20px = 10dp). Status bar nằm ngoài vùng này.
Chỉ kiểm tra trên emulator; không cài lên điện thoại thật.

Các ảnh trong `review_20261016/current` là bản trước yêu cầu ActionBar 70dp,
không còn là ảnh xác nhận vị trí header hiện tại.
