// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

chromeSpec(function(runningInBackground) {
  describe('chrome.fileSystem', function() {
    describe('chooseEntry()', function() {
      it('throws when args are invalid', function() {
        expect(function() { chrome.fileSystem.chooseEntry(); }).toThrow();
        expect(function() { chrome.fileSystem.chooseEntry(1); }).toThrow();
        expect(function() { chrome.fileSystem.chooseEntry(1, function() { }); }).toThrow();
        expect(function() { chrome.fileSystem.chooseEntry(function() { }, { }); }).toThrow();
      });
    });
  });
});

