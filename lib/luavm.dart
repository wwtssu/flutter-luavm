// Copyright 2020 tgarm. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be found in the LICENSE file.

import 'dart:async';
import 'dart:convert';

import 'package:flutter/services.dart';
import 'errors.dart';

export 'errors.dart';

// This class implements the `package:luavm`
class Luavm {
  // we use channel `com.github.tgarm.luavm`
  static const MethodChannel _channel =
      const MethodChannel('com.github.tgarm.luavm');

  static const BasicMessageChannel<dynamic> _callback =
      const BasicMessageChannel("com.github.tgarm.luavm.callback", StandardMessageCodec());

  factory Luavm.Init() => _getInstance();
  static Luavm get instance => _getInstance();
  static Luavm _instance;
  Luavm._internal(){
    _callback.setMessageHandler((value) => callback(value));
  }
  static Luavm _getInstance() {
    if (_instance == null) {
      _instance = new Luavm._internal();
    }
    return _instance;
  }

  // use a list to store vm names
  static List<String> _vms = [];
  // open a new Lua vm with name, return true when succeed
  static List<String> _socketLua = [
    "packages/luavm/lua/socket.lua",
    "packages/luavm/lua/url.lua",
    "packages/luavm/lua/ltn12.lua",
    "packages/luavm/lua/mime.lua",
    "packages/luavm/lua/headers.lua",
    "packages/luavm/lua/http.lua",
    "packages/luavm/lua/dkjson.lua"
    //"packages/luavm/lua/mbox.lua",
    //"packages/luavm/lua/smtp.lua",
    //"packages/luavm/lua/ftp.lua",
  ];

  static Future<bool> open(String name) async {
    bool success = false;
    try {
      if (_vms.contains(name)) return null;
      final int idx = await _channel.invokeMethod('open');
      if (idx >= 0) {
        while (_vms.length <= idx) {
          _vms.add(null);
        }
        _vms[idx] = name;
      }
      for(int i = 0;i<_socketLua.length;i++){
        String code = await rootBundle.loadString(_socketLua[i]);
        await eval(name, code);
      }
      success = true;
    } on PlatformException catch (e) {
      throw LuaError.from(e);
    }
    return success;
  }

  // check Lua vm is opened now, return true/false
  static bool opened(String name) {
    if (_vms.contains(name)) {
      return true;
    }
    return false;
  }

  // close a Lua vm, return true when succeed
  static Future<bool> close(String name) async {
    bool success = false;
    try {
      if (_vms.contains(name)) {
        final int idx = _vms.indexOf(name);
        success = await _channel.invokeMethod('close', idx);
        if (success) {
          _vms[idx] = null;
        }
      }
    } on PlatformException catch (e) {
      throw LuaError.from(e);
    }
    return success;
  }

  // eval, run Lua code in named vm
  // returns a list of result, when there is no result, just return an empty list
  static Future<List> eval(String name, String code) async {
    try {
      if (name != null && _vms.contains(name)) {
        final res = await _channel.invokeMethod<List>(
            'eval', <String, dynamic>{"id": _vms.indexOf(name), "code": code});
        if (res[0] != 'OK') {
          throw LuaError(json.encode(res));
        }
        return res.sublist(1);
      } else {
        throw LuaError("VM[$name] not exists");
      }
    } on PlatformException catch (e) {
      throw LuaError.from(e);
    }
  }

  static Map<String,List<Completer>> _completerList = {};

  static Future<List> evalAsync(String name, String code) async {
    try {
      if (name != null && _vms.contains(name)) {
        Completer<List> myCompleter = Completer<List>();
        if(_completerList[name] == null) _completerList[name] = List<Completer>();
        _completerList[name].add(myCompleter);
        Completer<List> firstCompleter = _completerList[name][0];
        while(firstCompleter != myCompleter){
          await firstCompleter.future;
          firstCompleter = _completerList[name][0];
        }
        await _channel.invokeMethod(
            'evalAsync', <String, dynamic>{"id": _vms.indexOf(name), "code": code});
        return myCompleter.future;
      } else {
        throw LuaError("VM[$name] not exists");
      }
    } on PlatformException catch (e) {
      throw LuaError.from(e);
    }
  }

  callback(value) async{
    String name = _vms[value["id"]];
    List res = value["res"];
    Completer firstCompleter = _completerList[name][0];
    _completerList[name] = _completerList[name].sublist(1);
    if (res[0] != 'OK') {
      firstCompleter.completeError(LuaError(json.encode(res)));
    }
    else firstCompleter.complete(res.sublist(1));
  }
}
