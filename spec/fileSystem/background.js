function initPage() {
  addActionButton('chrome.fileSystem.chooseEntry()', function() {
    // This method is called when a file entry is retrieved.
    var chooseEntryCallback = function(fileEntry) {
      log('fileEntry.name: ' + fileEntry.name);
      fileEntry.file(onFileRetrieved, null);
    };

    // This method is called when a file is retrieved from a file entry.
    var onFileRetrieved = function(file) {
      log('file.lastModifiedDate: ' + file.lastModifiedDate);
    };

    chrome.fileSystem.chooseEntry({ }, chooseEntryCallback);
  });
}

