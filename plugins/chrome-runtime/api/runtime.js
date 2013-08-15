// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var argscheck = cordova.require('cordova/argscheck');
var Event = require('org.chromium.chrome-common.events');
var stubs = require('org.chromium.chrome-common.stubs');
var helpers = require('org.chromium.chrome-common.helpers');

var manifestJson = null;

exports.onSuspend = new Event('onSuspend');
exports.onInstalled = new Event('onInstalled');
exports.onStartup = new Event('onStartup');
exports.onSuspendCanceled = new Event('onSuspendCanceled');
exports.onUpdateAvailable = new Event('onUpdateAvailable');
//exports.onBrowserUpdateAvailable = new Event('onBrowserUpdateAvailable');
//exports.onConnect = new Event('onConnect');
//exports.onConnectExternal = new Event('onConnectExternal');
//exports.onMessage = new Event('onMessage');
//exports.onMessageExternal = new Event('onMessageExternal');


// Uses a trampoline to bind the Cordova pause event on the first call.
var original_onSuspend_addListener = exports.onSuspend.addListener;
exports.onSuspend.addListener = function(f) {
  window.document.addEventListener('pause', function() {
    retval = exports.onSuspend.fire.apply(exports.onSuspend, arguments);
    chrome.storage.internal.set({"shutdownClean":true});
    return retval;
  }, false);
  exports.onSuspend.addListener = original_onSuspend_addListener;
  exports.onSuspend.addListener(f);
};

// Uses a trampoline to bind the Cordova resume event on the first call.
var original_onSuspendCanceled_addListener = exports.onSuspendCanceled.addListener;
exports.onSuspendCanceled.addListener = function(f) {
  window.document.addEventListener('resume', function() {
    chrome.storage.internal.remove("shutdownClean");
    return exports.onSuspendCanceled.fire.apply(exports.onSuspendCanceled, arguments);
  }, false);
  exports.onSuspendCanceled.addListener = original_onSuspendCanceled_addListener;
  exports.onSuspendCanceled.addListener(f);
};

exports.getManifest = function() {
  if (!manifestJson) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', 'manifest.json', false /* sync */);
    xhr.send(null);
    manifestJson = eval('(' + xhr.responseText + ')'); //JSON.parse(xhr.responseText);
  }
  return manifestJson;
};

// This is an extension for vanilla cordova apps
exports.setManifest = function(manifest) {
  manifestJson = manifest;
  calculateAndCacheAppId();
}

exports.getBackgroundPage = function(callback) {
  try {
  var mobile = require('org.chromium.chrome-bootstrap.mobile.impl');
  } catch(e) {}
  argscheck.checkArgs('f', 'chrome.runtime.getBackgroundPage', arguments);
  setTimeout(function() {
    callback(typeof mobile !== 'undefined' ? mobile.bgWindow : undefined);
  }, 0);
};

exports.getURL = function(subResource) {
  argscheck.checkArgs('s', 'chrome.runtime.getURL', arguments);
  if (subResource.charAt(0) == '/') {
    subResource = subResource.slice(1);
  }
  var prefix = location.href.replace(/[^\/]*$/, '');
  return prefix + subResource;
};

exports.reload = function() {
  location.reload();
};

function calculateAndCacheAppId() {
  var appId;
  try {
    require('org.chromium.chrome-bootstrap.bootstrap');
    var key = exports.getManifest()['key'];
    if (typeof key === 'undefined') {
      // For development, we want a consistent ID even without a public key.  Chrome uses the app path instead of name.
      appId = helpers.mapAppNameToAppId(exports.getManifest()['name']);
    } else {
      try {
        appId = helpers.mapAppKeyToAppId(key);
      } catch (e) {
        // If you are a mobile chrome app, and you do have a key in your manifest, and its invalid, we shouldn't pretend to return a valid appId
        console.error('Your manifest file has an invalid \'key\', cannot produce application id.');
        // leaving appId undefined.
      }
    }
  } catch (e) {
    // TODO: return fully qualified name given during cordova project creation
    appId = exports.getManifest()['name'];
  }
  exports.__defineGetter__("id", function() {
    return appId;
  });
  return appId;
}
exports.__defineGetter__("id", calculateAndCacheAppId);

stubs.createStub(exports, 'requestUpdateCheck', function(){});
