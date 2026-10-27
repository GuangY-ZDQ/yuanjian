# 渊鉴 · 本地加密账号管理工具

> 深水为鉴，明镜映渊。

一款以 **账号管理为核心、密码生成为辅助** 的本地加密保险库。所有数据在落盘时即被加密（**永不明文**），同一份单文件 HTML 源码可构建 **安卓 APK / Windows 桌面 / 网页版** 三端。

## 功能
- **账号管理**：记录 网站/App、账号名、当前与初始邮箱、密码、密保问题、生日、自由备注
- **历史密码**：独立弹窗表格，展示「原密码 → 修改密码 → 修改时间」，默认星号遮罩
- **密码生成**：随机密码（长度/字符集可调）＋ 记忆短语（中文自动转拼音、按音节打散穿插）
- **强度检测**：zxcvbn（主流强度评估，双 CDN 回退）
- **安全**：可选主密码（PBKDF2 60 万次迭代）＋ AES-256-GCM 多层加密
- **云端同步**：WebDAV（坚果云等）自动比对恢复 / 退出自动上传 ＋ 手动带时间戳存档
- **三端**：网页版（浏览器）、Windows EXE（数据存 EXE 同目录加密）、安卓 APK

## 安全模型（多层加密，落盘永不明文）
1. 保险库内容用随机 **设备密钥 DK** 做 AES-256-GCM 加密，每块独立随机 IV＋认证标签；
2. 开启主密码后，DK 再由 **PBKDF2(主密码, 随机盐, 600000 次)** 派生的密钥二次包裹，并从本地移除设备密钥——只有输入主密码才能解出 DK；
3. Windows 桌面版主进程还会对整份数据文件再做一层 AES-256-GCM（密钥存于系统用户目录，与数据文件分离）。

因此即使黑客拿到本地文件，仍需同时具备**设备密钥**与**主密码**两层才能解出明文。

## 构建
- **网页版**：`lark-cli apps +deploy --file-path ./账号管理工具.html --app-id <id>`
- **Windows**：`npx electron-packager . 渊鉴 --platform=win32 --arch=x64 --out=dist --icon=yuanjian.ico --overwrite --asar`（需 wine64 写 exe 图标）
- **安卓 APK**：进入 `android/`，`ANDROID_HOME=/path/to/sdk ./build.sh`（手打工具链 aapt2 → javac → d8 → zipalign → apksigner；详见 docs）

详细步骤见 [docs/渊鉴-资料文档.md](docs/渊鉴-资料文档.md)。

## 目录结构
```
├── 账号管理工具.html   # 三端共用的唯一源码（单文件自包含）
├── android/            # 安卓 APK 工程（AndroidManifest + MainActivity + res 图标 + build.sh）
├── desktop/            # Windows 桌面包壳（Electron main.js / preload.js / package.json）
├── design/             # LOGO 设计资产（SVG）
├── docs/               # 资料文档与构建手册
└── README.md
```

## 开发者
开发者：**泽渊** · AI 协作：豆包（Doubao）
