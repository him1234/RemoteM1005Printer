# Remote M1005 Printer Android App

这个 Android App 用来控制 Orange Pi Zero 上的 HP LaserJet M1005 打印/扫描服务，并在 Android 系统里注册一个打印服务。

## UI 规范

- 最低支持 Android 10
- 支持暗黑模式
- 界面尽量符合 Android 最新原生设计语言

## 功能

- 配置 Orange Pi Web 后端地址，例如 `http://192.168.1.50:8080`
- 查看打印机与扫描仪状态
- 唤醒 LCD 背光
- 从手机选择 PDF、Word、图片等文件上传到 `/print`
- 从手机触发扫描，可选择 PDF 或 PNG 输出
- 扫描结果先进入 App 内预览页，再选择保存或放弃
- 点击保存时调用 Android 系统文件选择器，每次由用户选择保存位置
- 注册 Android `PrintService`，在其它 App 的系统打印界面里选择 `HP LaserJet M1005 @ Orange Pi`

## 重要限制

Android 不允许普通 App 静默启用打印服务。安装 APK 后必须手动启用一次：

1. 打开 App，填写 Orange Pi 地址并保存。
2. 点 `打开 Android 打印服务设置`。
3. 进入系统打印设置后启用 `Orange Pi HP M1005 打印服务`。
4. 在 WPS、浏览器、相册等 App 里选择“打印”，选择 `HP LaserJet M1005 @ Orange Pi`。

系统打印模式下，Android 会先把内容渲染成 PDF，再由本 App 上传到 Orange Pi `/print`。这样 Word 文档的中文字体由手机端 App/WPS 渲染，通常可以避开 Orange Pi LibreOffice 中文字体缺失导致的乱码。

## 编译

用 Android Studio 打开本目录：

```text
android/RemoteM1005Printer
```

然后执行：

```bash
./gradlew buildAllApks
```

生成的 APK 通常在：

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

## 在线编译 APK

项目已经内置 GitHub Actions：

```text
.github/workflows/android-build.yml
```

把本目录作为 GitHub 仓库上传后，进入 `Actions`，手动运行 `Build Android APK`，构建完成后在 `Artifacts` 下载 `RemoteM1005Printer-debug-apk` 和 `RemoteM1005Printer-release-apk`。

## 后端要求

Orange Pi 后端需要已经运行 `printer-web.service`，并提供这些接口：

- `POST /print`
- `POST /scan`
- `POST /lcd/wake`
- `GET /api/status`
- `GET /api/scanner`

如果 `config.yaml` 里设置了 `web.access_pin`，请在 App 里填写同一个 PIN。
