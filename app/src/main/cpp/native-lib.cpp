#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_menu_noni_android_noni_model3D_view_ModelActivity_stringFromJNI(JNIEnv *env,
                                                                     jobject instance) {

    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}