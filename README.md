# Học Rubik CFOP

App Android đơn giản giúp học giải Rubik 3×3 theo phương pháp **CFOP 2-look**
(bản CFOP cho người mới học — đúng chuẩn CFOP, chỉ gọn hơn bản đầy đủ 78 công thức).

## Cách dùng

1. **Nhập khối**: chạm vào từng ô trên hình khối rồi chọn màu cho đúng với khối thật
   của anh (đặt mặt vàng lên trên, mặt xanh lá ra trước). Hoặc bấm **Xáo trộn** để lấy
   một khối ngẫu nhiên, hay **Nhập ký hiệu** để dán công thức xáo trộn
   (ví dụ: `R U R' U' R' F R2 U' R' U' R U R' F'`).
2. Bấm **Giải khối này** — app tính cách giải rồi hướng dẫn từng nước.
3. **Màn hướng dẫn**: mỗi bước hiện một nước xoay (R, U, R'...). Bấm **Bước tiếp**
   để xoay theo, hoặc **Tự chạy** để app tự chạy từng bước. Hình khối cập nhật
   theo từng nước.

## Các bước CFOP trong app

| Bước | Nội dung |
| --- | --- |
| Chữ thập trắng | Đưa 4 cạnh trắng xuống mặt dưới |
| Cặp F2L 1–4 | Theo cách người chơi: chọn cặp dễ nhất → (nếu cần) hất viên kẹt lên → xoay U về 1 trong 41 case → làm công thức chuẩn của case đó |
| OLL: định hướng cạnh | Lật cạnh tầng trên cho mặt trên thành một màu |
| OLL: định hướng góc | Xoay góc tầng trên cho mặt trên thành một màu |
| PLL: hoán vị góc | Đổi chỗ góc tầng trên về đúng vị trí |
| PLL: hoán vị cạnh | Đổi chỗ cạnh tầng trên về đúng vị trí |

## Ký hiệu nước xoay

R = mặt phải, L = mặt trái, U = mặt trên, D = mặt dưới, F = mặt trước,
B = mặt sau, M = lớp giữa, x = xoay cả khối theo chiều R. Không dấu = xoay
theo chiều kim đồng hồ (nhìn từ phía mặt đó), `'` = ngược chiều kim đồng hồ,
`2` = xoay 180°.

## Build APK

Mỗi lần push code lên GitHub, GitHub Actions tự build file APK debug.
Vào tab **Actions** → chọn lần chạy mới nhất → tải file `rubik-cfop-apk`
về điện thoại rồi cài đặt (cho phép cài từ nguồn không xác định).

Build thủ công:

```bash
gradle :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Bộ giải (solver)

Bộ giải viết bằng Java (`app/src/main/java/vn/cfop/rubik/solver/`), có bản
tham chiếu bằng Python (`python/solver.py`) kèm bộ test (`python/test_solver.py`,
`java-test/TestSolver.java`). Bộ test giải 100 khối xáo trộn ngẫu nhiên và
kiểm tra từng bước: chữ thập ≤ 8 nước, F2L giữ nguyên chữ thập, OLL/PLL
đúng công thức chuẩn, khối cuối cùng đã giải xong.

## Giấy phép

Dùng cho mục đích cá nhân, học tập.

## F2L theo 41 case (cách người chơi)

- `solver/F2lCases.java` là **nguồn duy nhất** của 41 case (số thứ tự + công thức người chơi).
  Thế mẫu, nhóm (24/6/6/5), mô tả cách nhận ra, khoá nhận diện đều được **tính** từ công thức.
  Màn Luyện tập (`TrainingData.kt`) và bộ giải dùng chung bảng này.
- `solver/F2lHuman.java` lập kế hoạch cho từng cặp: nhận diện case (xoay U tối đa 3 lần là khớp
  đúng 1 trong 41 thế mẫu), nếu viên đang kẹt ở khe khác thì hất lên bằng `R U R'` rồi nhận diện lại.
  Khe không ở trước-phải thì xoay cả khối (`y`) rồi xoay lại. Không có tìm kiếm của máy (IDA*),
  không có nước B/D/d.
- Mỗi bước F2L trong "Giải khối này" có `Stage.note`: số case /41, nhóm, cách nhận ra, công thức chuẩn.
- Test: `java-test/F2lTest.java` (bảng 41 case phải đủ 41 khoá khác nhau, mỗi công thức giải đúng
  thế mẫu, giải ngẫu nhiên không có lỗi, chỉ dùng U R L F y).
