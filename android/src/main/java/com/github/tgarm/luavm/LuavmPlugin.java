package com.github.tgarm.luavm;

import androidx.annotation.NonNull;
import android.util.Log;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.BasicMessageChannel;
import io.flutter.plugin.common.StandardMessageCodec;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import android.os.Handler;
import android.os.Message;

/** LuavmPlugin */
public class LuavmPlugin implements FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native
  /// Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine
  /// and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private static BasicMessageChannel<Object> callback;

  private static Handler handler;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "com.github.tgarm.luavm");
    channel.setMethodCallHandler(this);
    callback = new BasicMessageChannel<>(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "com.github.tgarm.luavm.callback", new StandardMessageCodec());
    handler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
        super.handleMessage(msg);
        if(msg.what == 1){
          callback.send(msg.obj);
        }
      }
    };
  }

  // This static function is optional and equivalent to onAttachedToEngine. It
  // supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new
  // Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith
  // to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith
  // will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both
  // be defined
  // in the same class.
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "com.github.tgarm.luavm");
    channel.setMethodCallHandler(new LuavmPlugin());
    callback = new BasicMessageChannel<>(registrar.messenger(), "com.github.tgarm.luavm.callback", new StandardMessageCodec());
    handler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
        super.handleMessage(msg);
        if(msg.what == 1){
          Log.e("test",msg.obj.toString());
          callback.send(msg.obj);
        }
      }
    };
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    int id;
    String code;
    switch (call.method) {
      case "open":
        id = LuaJNI.open();
        result.success(id);
        break;
      case "close":
        id = ((Integer)call.arguments).intValue();
        Boolean bres = LuaJNI.close(id);
        result.success(bres);
        break;
      case "eval":
        id = call.argument("id");
        code = call.argument("code");
        String restr[] = LuaJNI.eval(id, code);
        ArrayList<String> res = new ArrayList<String>();
        for (int i = 0; i < restr.length; i++) {
          res.add(restr[i]);
        }
        result.success(res);
        break;
      case "evalAsync":
        id = call.argument("id");
        code = call.argument("code");
        final int Id = id;
        final String Code = code;
        new Thread(() -> {
            String restring[] = LuaJNI.eval(Id, Code);
            ArrayList<String> ret = new ArrayList<String>();
            for (int i = 0; i < restring.length; i++) {
              ret.add(restring[i]);
            }
            Map event = new LinkedHashMap();
            event.put("id",Id);
            event.put("res",ret);
            Message msg=new Message();
            msg.what = 1;
            msg.obj = event;
            handler.sendMessage(msg);
          }).start();
        result.success(true);
        break;
      default:
        result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }
}
