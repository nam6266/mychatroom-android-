// JNI bridge between com.example.mychatroom.engine.SurvivorEngine (Kotlin) and
// SurvivorLoop. Same pattern as alien_shooter/alien_shooter_jni.cpp: the Kotlin object keeps an
// opaque jlong handle to its own SurvivorLoop.

#include <jni.h>
#include <vector>
#include "SurvivorLoop.h"

namespace {
    SurvivorLoop *loopFrom(jlong handle) { return reinterpret_cast<SurvivorLoop *>(handle); }
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_mychatroom_engine_SurvivorEngine_nativeCreate(JNIEnv *, jobject, jint ticksPerSecond,
                                                               jint maxEnemies, jint maxBullets,
                                                               jint maxBlasts) {
    auto atLeast1 = [](jint n) { return static_cast<std::size_t>(n > 0 ? n : 1); };
    return reinterpret_cast<jlong>(new SurvivorLoop(ticksPerSecond, atLeast1(maxEnemies),
                                                    atLeast1(maxBullets), atLeast1(maxBlasts)));
}

JNIEXPORT void JNICALL
Java_com_example_mychatroom_engine_SurvivorEngine_nativeDestroy(JNIEnv *, jobject, jlong handle) {
    delete loopFrom(handle);   // ~SurvivorLoop stops and joins the logic thread
}

JNIEXPORT void JNICALL
Java_com_example_mychatroom_engine_SurvivorEngine_nativeStart(JNIEnv *, jobject, jlong handle) {
    loopFrom(handle)->start();
}

JNIEXPORT void JNICALL
Java_com_example_mychatroom_engine_SurvivorEngine_nativeSetPaused(JNIEnv *, jobject, jlong handle,
                                                                  jboolean paused) {
    loopFrom(handle)->setPaused(paused == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_example_mychatroom_engine_SurvivorEngine_nativeSetScreen(JNIEnv *, jobject, jlong handle,
                                                                  jfloat width, jfloat height) {
    loopFrom(handle)->setScreen(width, height);
}

JNIEXPORT void JNICALL
Java_com_example_mychatroom_engine_SurvivorEngine_nativeSetJoystick(JNIEnv *, jobject, jlong handle,
                                                                    jfloat x, jfloat y) {
    loopFrom(handle)->setJoystick(x, y);
}

JNIEXPORT void JNICALL
Java_com_example_mychatroom_engine_SurvivorEngine_nativeRestart(JNIEnv *, jobject, jlong handle) {
    loopFrom(handle)->restart();
}

JNIEXPORT jint JNICALL
Java_com_example_mychatroom_engine_SurvivorEngine_nativeGetSnapshot(JNIEnv *env, jobject, jlong handle,
                                                                    jfloatArray out) {
    const jsize capacity = env->GetArrayLength(out);
    thread_local std::vector<float> scratch;
    if (static_cast<jsize>(scratch.size()) < capacity) {
        scratch.resize(static_cast<std::size_t>(capacity));
    }
    const int written = loopFrom(handle)->writeSnapshot(scratch.data(), capacity);
    env->SetFloatArrayRegion(out, 0, written, scratch.data());
    return written;
}

} // extern "C"
