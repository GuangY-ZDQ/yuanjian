const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('NativeFile', {
  load: () => ipcRenderer.invoke('data:load'),
  save: (json) => ipcRenderer.invoke('data:save', json),
  path: () => ipcRenderer.invoke('data:path')
});
