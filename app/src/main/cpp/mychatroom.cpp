#include <jni.h>
#include <android/log.h>
#include "Player.h"
#include "Enemy.h"
#include <android/log.h>


float x = 100.0f;
float y = 100.0f;
float xVelocity = 5.0f;
float yVelocity = 5.0f;
int z = 0;

Player player = Player(1080.0f);
Enemy enemy = Enemy(20);

bool checkColsion();

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mychatroom_engine_GameEngine_update(JNIEnv *env, jobject thiz, jfloat newPosition) {
    player.setPosition(newPosition);

}

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_example_mychatroom_engine_GameEngine_getPosition(JNIEnv *env, jobject thiz) {
    return player.getPosition();
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_mychatroom_engine_GameEngine_getNumber(JNIEnv *env, jobject thiz) {
    jint increas = z++;
    return increas;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mychatroom_engine_GameEngine_setScreen(JNIEnv *env, jobject thiz, jfloat witdh, jfloat height) {
    player.setScreen(witdh);
    enemy.setScreen(witdh,height);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mychatroom_engine_GameEngine_setVelocity(JNIEnv *env, jobject thiz, jfloat newVelocity) {
    player.setVelocity(newVelocity);
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_mychatroom_engine_GameEngine_updateEnemy(JNIEnv *env, jobject thiz, jfloat playerPosition) {
    return enemy.updateEnemy( playerPosition);
}

bool checkColsion() {
    return false;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_example_mychatroom_engine_GameEngine_getEnemy(JNIEnv *env, jobject thiz) {
    size_t size = enemy.getEnemyNumber();
    jclass floatArrayClass = env->FindClass("[F"); // float[]

    jobjectArray result = env->NewObjectArray(size, floatArrayClass, nullptr);

    for (int i = 0; i < size; i++) {
        jfloatArray point = env->NewFloatArray(2);
        float values[2] = { enemy.getEnemyPositionx(i), enemy.getEnemyPositiony(i) };
        env->SetFloatArrayRegion(point, 0, 2, values);
        env->SetObjectArrayElement(result, i, point);
        env->DeleteLocalRef(point);
    }

    return result;
}
