#include <string.h>
#include "com_github_tgarm_luavm_LuaJNI.h"
#include "lua.h"
#include "lauxlib.h"
#include "lualib.h"

#define MAX_VMS	100
static lua_State *vms[MAX_VMS] = {NULL};


JNIEXPORT jint JNICALL Java_com_github_tgarm_luavm_LuaJNI_open
(JNIEnv *env, jclass cls){
	for(int i=0;i<MAX_VMS;i++){
		if(!vms[i]){
			lua_State *L = luaL_newstate();
			if(L){
				luaL_openlibs(L);
				vms[i] = L;
				return i;
			}
		}
	}
	return -1;
}

JNIEXPORT jboolean JNICALL Java_com_github_tgarm_luavm_LuaJNI_close
(JNIEnv *env, jclass cls, jint id){
	if(id>=0&&id<MAX_VMS){
		lua_State *L = vms[id];
		if(L){
			lua_close(L);
			vms[id] = NULL;
			return JNI_TRUE;
		}
	}
	return JNI_FALSE;
}

JNIEXPORT jobjectArray JNICALL Java_com_github_tgarm_luavm_LuaJNI_eval
(JNIEnv *env, jclass cls, jint id, jstring jcode){
	const char *restr = "Fail";
	jobjectArray rets = NULL;
	if(id>=0&&id<MAX_VMS){
		lua_State *L = vms[id];
		if(L){
			const char *code = (*env)->GetStringUTFChars(env,jcode, NULL);
  			int base = lua_gettop(L);
			int res = luaL_dostring(L, code);
			int top = lua_gettop(L);
			if(res>0){
				restr = lua_tostring(L,-1);
				lua_pop(L,1);
				top = lua_gettop(L);
			}else{
				restr = "OK";
			}
			if(top>0){
				rets = (*env)->NewObjectArray(env, top+1,(*env)->FindClass(env,"java/lang/String"),(*env)->NewStringUTF(env,""));
				for(int i=0;i<top;i++){
					const char *str = lua_tostring(L, i-top);
					if(str){
						(*env)->SetObjectArrayElement(env,rets,i+1,(*env)->NewStringUTF(env,str));
					}
				}
			}
			if(top>base){
  				lua_pop(L, top-base);
			}
		}else{
			restr = "VM Not exist";
		}
	}else{
		restr = "VM ID out of range";
	}
	if(rets==NULL){
		rets = (*env)->NewObjectArray(env, 1,(*env)->FindClass(env,"java/lang/String"),(*env)->NewStringUTF(env,restr));
	}else{
		(*env)->SetObjectArrayElement(env,rets,0,(*env)->NewStringUTF(env,restr));

	}
	return rets;
}
