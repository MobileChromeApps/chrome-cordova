// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var exports = module.exports;
var exec = cordova.require('cordova/exec');
var socket = cordova.require('org.chromium.chrome.socket.socket');

function StorageChange(oldValue, newValue) {
    this.oldValue = oldValue;
    this.newValue = newValue;
}

function _jsonReplacer(key) {
    // Don't use the value passed in since it has already gone through toJSON().
    var value = this[key];
    // Refer to:
    // chrome/src/content/renderer/v8_value_converter_impl.cc&l=165
    if (value && (typeof value == 'object' || typeof value == 'function')) {
        var typeName = Object.prototype.toString.call(value).slice(8, -1);
        if (typeName != 'Array' && typeName != 'Object') {
            value = {};
        }
    }
    return value;
}

function _scrubValues(o) {
    if (typeof o != 'undefined') {
        var t = JSON.stringify(o, _jsonReplacer);
        return JSON.parse(t);
    }
}

function _calculateChanges(oldKeyVals, newKeyVals) {
    var ret = {};
    for(var key in newKeyVals) {
        if (newKeyVals.hasOwnProperty(key)) {
            ret[key] = new StorageChange(oldKeyVals[key], newKeyVals[key]);
        }
    }
    return ret;
}

function _convertToObject(obj) {
    var ret;
    if (Array.isArray(obj)) {
        ret = {};
        for(var i = 0; i < obj.length; i++) {
            ret[obj[i]] = undefined;
        }
    } else if (typeof obj == 'object') {
        ret = obj;
    } else if (typeof obj === 'string') {
        ret = {};
        ret[obj] = undefined;
    }
    return ret;
}

function StorageArea(namespace, changedEvent) {
    this._namespace = namespace;
    this._changedEvent = changedEvent;
}

StorageArea.prototype._getAreaName = function() {
    return this._namespace;
};

StorageArea.prototype.get = function(keys, callback) {
    if (typeof keys == 'function') {
        callback = keys;
        keys = null;
    } else if (typeof keys === 'string') {
        keys = [keys];
    }
    var win = callback && function(args) {
        callback(args);
    };
    var fail = callback && function() {
        callback();
    };
    var param = _scrubValues(keys);
    exec(win, fail, 'ChromeStorage', 'get', [this._namespace, param]);
};

StorageArea.prototype.getBytesInUse = function(keys, callback) {
    if (typeof keys == 'function') {
        callback = keys;
        keys = null;
    } else if (typeof keys === 'string') {
        keys = [keys];
    }
    var win = callback && function(bytes) {
        callback(bytes);
    };
    var fail = callback && function() {
        callback(-1);
    };
    var param = _scrubValues(keys);
    exec(win, fail, 'ChromeStorage', 'getBytesInUse', [this._namespace, param]);
};

StorageArea.prototype.set = function(keyVals, callback) {
    if (typeof keyVals == 'function') {
        callback = keyVals;
        keyVals = null;
    }
    var self = this;
    var param = _scrubValues(keyVals);
    var fail = callback && function() {
        callback(-1);
    };
    var win;
    if(self._changedEvent && self._changedEvent.hasListeners()) {
        win = function(oldKeyVals) {
            if(callback) {
                callback(0);
            }
            var newKeyVals = _convertToObject(param);
            var storageChanges = _calculateChanges(oldKeyVals, newKeyVals);
            self._changedEvent.fire(storageChanges, self._getAreaName());
        };
    } else {
        win = callback && function() {
            callback(0);
        };
    }
    exec(win, fail, 'ChromeStorage', 'set', [self._namespace, param]);
};

StorageArea.prototype.remove = function(keys, callback) {
    if (typeof keys == 'function') {
        callback = keys;
        keys = null;
    } else if (typeof keys === 'string') {
        keys = [keys];
    }
    var self = this;
    var param = _scrubValues(keys);
    var fail = callback && function() {
        callback(-1);
    };
    var win;
    if(self._changedEvent && self._changedEvent.hasListeners()) {
        win = function(oldKeyVals) {
            if(callback) {
                callback(0);
            }
            var newKeyVals = _convertToObject(Object.keys(oldKeyVals));
            var storageChanges = _calculateChanges(oldKeyVals, newKeyVals);
            self._changedEvent.fire(storageChanges, self._getAreaName());
        };
    } else {
        win = callback && function() {
            callback(0);
        };
    }
    exec(win, fail, 'ChromeStorage', 'remove', [self._namespace, param]);
};

StorageArea.prototype.clear = function(callback) {
    var self = this;
    var fail = callback && function() {
        callback(-1);
    };
    var win;
    if(self._changedEvent && self._changedEvent.hasListeners()) {
       win = function(oldKeyVals) {
           if(callback) {
               callback(0);
           }
           var newKeyVals = _convertToObject(Object.keys(oldKeyVals));
           var storageChanges = _calculateChanges(oldKeyVals, newKeyVals);
           self._changedEvent.fire(storageChanges, self._getAreaName());
       };
    } else {
        win = callback && function() {
            callback(0);
        };
    }
    exec(win, fail, 'ChromeStorage', 'clear', [self._namespace]);
};

var Event = require('org.chromium.chrome-common.events');
exports.onChanged = new Event('onChanged');

var local = new StorageArea('local', exports.onChanged);
local.QUOTA_BYTES = 5242880;
exports.local = local;

var sync = new StorageArea('sync', exports.onChanged);
sync.MAX_ITEMS = 512;
sync.MAX_WRITE_OPERATIONS_PER_HOUR = 1000;
sync.QUOTA_BYTES_PER_ITEM = 4096;
sync.MAX_SUSTAINED_WRITE_OPERATIONS_PER_MINUTE = 10;
sync.QUOTA_BYTES = 102400;
exports.sync = sync;

exports.internal = new StorageArea('internal', null);

var sharedOnChanged = new Event('onChanged');
exports.shared = new StorageArea('shared', sharedOnChanged);
exports.shared.onChanged = sharedOnChanged;

exports.shared._nextChangeUpdateIsFromRemote = false;

exports.shared._remoteOnChanged = function(changes) {
  var toCall = exports.shared.remove;
  Object.keys(changes).forEach(function(key) {
    if (changes[key].hasOwnProperty('newValue')) {
      toCall = exports.shared.set;
    }
    changes[key] = changes[key].newValue;
  });
  exports.shared._nextChangeUpdateIsFromRemote = true;
  toCall.call(exports.shared, changes);
};

exports.shared._connectedSockets = [];

exports.shared._addConnection = function(socketId) {
  exports.shared._connectedSockets.push(socketId);
  socket.read(socketId, function(readResult) {
    var recv = new Uint8Array(readResult.data);
    var changes = JSON.parse(String.fromCharCode.apply(null, recv));
    exports.shared._remoteOnChanged(changes);
  });
};
exports.shared._removeConnection = function(socketId) {
  exports.shared._connectedSockets = exports.shared._connectedSockets.filter(function(id) { return id !== socketId; })
}

exports.shared.onChanged.addListener(function(changes) {
  if (exports.shared._nextChangeUpdateIsFromRemote) {
    this._nextChangeUpdateIsFromRemote = false;
    return;
  }
  console.log('sending changes to remotes..');

  var changesEncoded = (function(str) {
      var ret = new Uint8Array(str.length);
      for (var i = 0; i < str.length; i++) {
          ret[i] = str.charCodeAt(i);
      }
      return ret.buffer;
  }(JSON.stringify(changes)));

  exports.shared._connectedSockets.forEach(function(socketId) {
    socket.write(socketId, changesEncoded, function(writeResult) {});
  });
});

