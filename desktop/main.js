const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const fs = require('fs');
const os = require('os');
const crypto = require('crypto');

// 数据文件：加密后存在 EXE 所在目录（整目录移动 = 数据随行）
const exeDir = path.dirname(process.execPath);
const dataPath = path.join(exeDir, 'yuanjian-data.json');

// 加密密钥：存在用户配置文件目录（与数据文件分离，即使拿到密文也无法解密）
const keyPath = path.join(process.env.APPDATA || path.join(os.homedir(), 'AppData', 'Roaming'), '渊鉴', 'local.key');

function getKey() {
  try {
    if (fs.existsSync(keyPath)) return fs.readFileSync(keyPath, 'utf8').trim();
  } catch (e) {}
  const k = crypto.randomBytes(32).toString('base64');
  try {
    fs.mkdirSync(path.dirname(keyPath), { recursive: true });
    fs.writeFileSync(keyPath, k, 'utf8');
  } catch (e) {}
  return k;
}
function encrypt(text) {
  const iv = crypto.randomBytes(12);
  const c = crypto.createCipheriv('aes-256-gcm', Buffer.from(getKey(), 'base64'), iv);
  const enc = Buffer.concat([c.update(text, 'utf8'), c.final()]);
  return { iv: iv.toString('base64'), t: enc.toString('base64'), tag: c.getAuthTag().toString('base64') };
}
function decrypt(o) {
  const c = crypto.createDecipheriv('aes-256-gcm', Buffer.from(getKey(), 'base64'), Buffer.from(o.iv, 'base64'));
  c.setAuthTag(Buffer.from(o.tag, 'base64'));
  return Buffer.concat([c.update(Buffer.from(o.t, 'base64')), c.final()]).toString('utf8');
}
function loadData() {
  try {
    if (!fs.existsSync(dataPath)) return null;
    const obj = JSON.parse(fs.readFileSync(dataPath, 'utf8'));
    if (!obj || !obj.iv) return null;
    return JSON.parse(decrypt(obj));
  } catch (e) { return null; }
}
function saveData(str) {
  const enc = encrypt(String(str));
  fs.writeFileSync(dataPath, JSON.stringify(enc), 'utf8');
}

ipcMain.handle('data:load', () => loadData());
ipcMain.handle('data:save', (e, json) => { saveData(json); return true; });
ipcMain.handle('data:path', () => dataPath);

function createWindow() {
  const win = new BrowserWindow({
    width: 1180,
    height: 780,
    minWidth: 920,
    minHeight: 620,
    icon: path.join(__dirname, 'yuanjian-256.png'),
    autoHideMenuBar: true,
    title: '渊鉴',
    backgroundColor: '#F5F6F8',
    webPreferences: {
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js')
    }
  });
  win.setMenuBarVisibility(false);
  win.loadFile(path.join(__dirname, 'assets', 'index.html'));
}

app.whenReady().then(createWindow);
app.on('window-all-closed', () => app.quit());
