// This method simply logs a given error.
var onError = function(e) {
  log('Error: ' + e.code);
};

function initPage() {
  var FileReader = cordova.require('cordova/plugin/FileReader');
  var FileTransfer = cordova.require('cordova/plugin/FileTransfer');

  addActionButton('chooseEntry, FileReader.readAsText', function() {
    // This method is called when a file entry is retrieved.
    var chooseEntryCallback = function(fileEntry) {
      fileEntry.file(onFileReceived, onError);
    };

    // This method is called when a file is received from a file entry.
    // It reads the file as text and logs it.
    var onFileReceived = function(file) {
      var reader = new FileReader();
      reader.onload = function(evt) {
        log('Text: ' + evt.target.result);
      };
      reader.onerror = function(evt) {
        log('Error: ' + evt.target.error.code);
      };
      reader.readAsText(file);
    };

    chrome.fileSystem.chooseEntry({ }, chooseEntryCallback);
  });

  addActionButton('chooseEntry, FileReader.readAsDataURL', function() {
    // This method is called when a file entry is retrieved.
    var chooseEntryCallback = function(fileEntry) {
      fileEntry.file(onFileReceived, onError);
    };

    // This method is called when a file is received from a file entry.
    // It reads the file as a data URL and logs it.
    var onFileReceived = function(file) {
      var reader = new FileReader();
      reader.onload = function(evt) {
        log('Data URL: ' + evt.target.result);
      };
      reader.onerror = function(evt) {
        log('Error: ' + evt.target.error.code);
      };
      reader.readAsDataURL(file);
    };

    chrome.fileSystem.chooseEntry({ }, chooseEntryCallback);
  });

  addActionButton('chooseEntry, FileTransfer.upload', function() {
    // This method is called when a file entry is retrieved.
    var chooseEntryCallback = function(fileEntry) {
      fileEntry.file(onFileReceived, onError);
    };

    // This method is called when a file is uploaded.
    var onFileUploaded = function(response) {
      log('Response code: ' + response.responseCode);
    };

    // This method is called when a file is received from a file entry.
    // It uploads the file to a server.
    var onFileReceived = function(file) {
      var transfer = new FileTransfer();
      transfer.upload(file.fullPath, 'http://cordova-filetransfer.jitsu.com/upload', onFileUploaded, onError, { });
    };

    chrome.fileSystem.chooseEntry({ }, chooseEntryCallback);
  });
}
