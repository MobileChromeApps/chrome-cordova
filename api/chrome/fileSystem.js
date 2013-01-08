// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

define('chrome.fileSystem', function(require, module) {
  var argscheck = cordova.require('cordova/argscheck');
  var exports = module.exports;

  exports.chooseEntry = function(options, callback) {
    // Check arguments.
    argscheck.checkArgs('of', 'chrome.fileSystem.chooseEntry', arguments);

    // Store the selected file entry's path.
    var selectedFilePath = null;

    // When the user selects a file, this method is called.
    // It stores the given file entry path and requests the temporary file system.
    var onFileSelected = function(_selectedFilePath) {
      selectedFilePath = _selectedFilePath;
      window.requestFileSystem(0, 0, onGotFileSystem, null);
    };

    // When the file system is retrieved, this method is called.
    // It retrieves the selected file entry.
    var onGotFileSystem = function(fileSystem) {
      if (!!selectedFilePath && typeof selectedFilePath == 'string') {
        fileSystem.root.getFile(selectedFilePath, { }, onGotFileEntry, null);
      }
    };

    // When the file entry is retrieved, this method is called.
    // It calls the given callback with the file entry.
    var onGotFileEntry = function(fileEntry) {
      if (!!callback && typeof callback == 'function') {
        callback(fileEntry);
      }
    };

    // Send the request over the bridge.
    cordova.exec(onFileSelected, null, 'FileSystem', 'chooseEntry', null);
  };
});
