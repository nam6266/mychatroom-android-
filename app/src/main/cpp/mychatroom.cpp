// JNI bridge between com.example.mychatroom.engine.GameEngine (Kotlin) and GameLoop.
//
// Each GameEngine owns one GameLoop through an opaque jlong handle, so every
// new game starts fresh and nothing is kept in global variables.

#include <jni.h>
#include <vector>
#include "GameLoop.h"

namespace {
    GameLoop *loopFrom(jlong handle) {
        return reinterpret_cast<GameLoop *>(handle);
    }
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_mychatroom_engine_GameEngine_nativeCreate(JNIEnv *, jobject,
                                                           jint ticksPerSecond, jint maxEnemies) {
    auto *loop = new GameLoop(ticksPerSecond, static_cast<std::size_t>(maxEnemies > 0 ? maxEnemies : 1));
    return reinterpret_cast<jlong>(loop);
}

JNIEXPORT void JNICALL
Java_com_example_mychatroom_engine_GameEngine_nativeDestroy(JNIEnv *, jobject, jlong handle) {
    delete loopFrom(handle);   // ~GameLoop stops and joins the logic thread
}

JNIEXPORT void JNICALL
Java_com_example_mychatroom_engine_GameEngine_nativeStart(JNIEnv *, jobject, jlong handle) {
    loopFrom(handle)->start();
}

JNIEXPORT void JNICALL
Java_com_example_mychatroom_engine_GameEngine_nativeSetPaused(JNIEnv *, jobject, jlong handle,
                                                              jboolean paused) {
    loopFrom(handle)->setPaused(paused == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_example_mychatroom_engine_GameEngine_nativeSetScreen(JNIEnv *, jobject, jlong handle,
                                                              jfloat width, jfloat height) {
    loopFrom(handle)->setScreen(width, height);
}

JNIEXPORT void JNICALL
Java_com_example_mychatroom_engine_GameEngine_nativeMovePlayerBy(JNIEnv *, jobject, jlong handle,
                                                                 jfloat dx) {
    loopFrom(handle)->movePlayerBy(dx);
}

JNIEXPORT void JNICALL
Java_com_example_mychatroom_engine_GameEngine_nativeRestart(JNIEnv *, jobject, jlong handle) {
    loopFrom(handle)->restart();
}

// Fills a FloatArray the Kotlin side allocates once, so rendering a frame
// creates no Java objects (the old getEnemy() made 21 arrays per frame).
JNIEXPORT jint JNICALL
Java_com_example_mychatroom_engine_GameEngine_nativeGetSnapshot(JNIEnv *env, jobject, jlong handle,
                                                                jfloatArray out) {
    const jsize capacity = env->GetArrayLength(out);
    thread_local std::vector<float> scratch;
    if (static_cast<jsize>(scratch.size()) < capacity) scratch.resize(capacity);

    const int written = loopFrom(handle)->writeSnapshot(scratch.data(), capacity);
    env->SetFloatArrayRegion(out, 0, written, scratch.data());
    return written;
}

} // extern "C"
