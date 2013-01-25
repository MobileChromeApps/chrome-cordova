// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.


chromeSpec(function(runningInBackground) {
  describe('chrome.fileSystem', function() {
    describe('getDisplayPath()', function() {
      it('returns the full path of the given file entry', function() {
        // Create a file entry.
        var FileEntry = cordova.require('cordova/plugin/FileEntry');
        var fileEntry = new FileEntry('filename', 'fullpath');

        // Create the callback.
        var getDisplayPathCallback = function(displayPath) {
          expect(displayPath).toEqual(fileEntry.fullPath);
        };

        // Get the display path.
        chrome.fileSystem.getDisplayPath(fileEntry, getDisplayPathCallback);
      });
    });
  });
});

