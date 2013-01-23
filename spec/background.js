var specScripts = [
  'test.chrome.app.runtime.js',
  'test.chrome.app.window.js',
  'test.chrome.fileSystem.js',
  'test.chrome.runtime.js',
  'test.chrome.storage.js',
  'test.pageload.js'
];

function initPage() {
  addActionButton('chrome.fileSystem', function() {
    top.location = 'fileSystem/chromeapp.html';
  });
  addActionButton('chrome.runtime', function() {
    top.location = 'runtime/chromeapp.html';
  });
  addActionButton('chrome.socket', function() {
    top.location = 'socket/chromeapp.html';
  });
}
