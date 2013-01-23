function initPage() {
  var FileReader = cordova.require('cordova/plugin/FileReader');

  addActionButton('chooseEntry, readAsDataURL', function() {
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
}

