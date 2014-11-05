#include <string.h>
#include <stdlib.h>
#include <jni.h>
#include <orbit.h>
//
//jstring Java_test_Jni_getKLTest(JNIEnv* env, jclass clazz)
//{
//#if defined(__arm__)
//#if defined(__ARM_ARCH_7A__)
//#if defined(__ARM_NEON__)
//#define ABI "armeabi-v7a/NEON"
//#else
//#define ABI "armeabi-v7a"
//#endif
//#else
//#define ABI "armeabi"
//#endif
//#elif defined(__i386__)
//#define ABI "x86"
//#elif defined(__mips__)
//#define ABI "mips"
//#else
//#define ABI "unknown"
//#endif
//	int i = test();
//	//free(i);
//	//i = 10;
//
//	if (i == 1)
//	{
//		return (*env)->NewStringUTF(env,
//				" Hello from JNI1 3 !  Compiled with ABI " ABI ". ");
//	}
//	return (*env)->NewStringUTF(env,
//			" Hello from JNI2 !  Compiled with ABI " ABI ". ");
//}
//
//jstring Java_test_Jni_callJessica(JNIEnv* env, jclass clazz, jobject jobj)
//{
//	jclass jessciaz = (*env)->FindClass(env, "test/Jni$Jessica");
//	jfieldID fid = (*env)->GetFieldID(env, jessciaz, "id", "I");
//	jmethodID mID = (*env)->GetMethodID(env, jessciaz, "setId", "(I)V");
//	if (1 == (*env)->GetIntField(env, jobj, fid))
//	{
//		//logDebug("jesscia`s id is 1");
//	}
//	(*env)->CallVoidMethod(env, jobj, mID, 2);
//	if (2 == (*env)->GetIntField(env, jobj, fid))
//	{
//		//logDebug("jesscia`s id has been changed to 2");
//	}
//	return (*env)->NewStringUTF(env, "123");
//}
//
//jobject Java_test_Jni_giveMeJessica(JNIEnv* env, jobject thiz)
//{
//	jclass cls = (*env)->FindClass(env, "test/Jni$Jessica");
//	jmethodID methodId = (*env)->GetMethodID(env, cls, "<init>", "(I)V");
//	return (*env)->NewObject(env, cls, methodId, 52);
//}

jobject c_thiz;
JavaVM* c_jvm;
jclass c_class_Extractor;



void init()
{
	hasSend = 0;
	need_save_count = 0;
	saved_count = 0;
	readFrame_finish = -1;
}

void release()
{
	hasSend = 0;
	need_save_count = 0;
	saved_count = 0;
	readFrame_finish = -1;
}

//涓嶅厑璁稿彇娑堟搷浣滐紝涓斿簲璇ョ瓑寰呮枃浠跺瓨鍌ㄥ畬鎴愬啀杩涜涓嬩竴娆ava_show360_Extractor_extract鐨勮皟鐢�
jobject Java_orbit_Extractor_extract(JNIEnv* env, jobject thiz,
		jstring videoPath, jstring framePath, jstring framePfx,
		jint maxFrameCount)
{
	log_kl("0.000");
	init();
	int width, height = NULL;
	c_thiz = (*env)->NewGlobalRef(env, thiz);

	(*env)->GetJavaVM(env, &c_jvm);
	char *pcPath = (*env)->GetStringUTFChars(env, videoPath, 0);
	char *pcFramePath = (*env)->GetStringUTFChars(env, framePath, 0);
	char *pcPfx = (*env)->GetStringUTFChars(env, framePfx, 0);
	log_kl("0.001");
	int count = decode2pixel(pcPath, pcFramePath, pcPfx, maxFrameCount, &width,
			&height);
	log_kl("0.002 ");
	//release
	(*env)->ReleaseStringUTFChars(env, videoPath, pcPath);
	(*env)->ReleaseStringUTFChars(env, framePath, pcFramePath);
	(*env)->ReleaseStringUTFChars(env, framePfx, pcPfx);

	jclass cls = (*env)->FindClass(env, "orbit/Extractor$Result");
	jclass l_class_Extractor = (*env)->FindClass(env, "orbit/Extractor");
	c_class_Extractor = (*env)->NewGlobalRef(env, l_class_Extractor);
	if (cls == NULL)
	{
		log_kl("has not found orbit/Extractor$Result ");
	}
	jmethodID methodId = (*env)->GetMethodID(env, cls, "<init>", "(III)V");
	return (*env)->NewObject(env, cls, methodId, width, height, count);
}

void sendBack()
{
	log_kl("sendBack call in jni");
	JNIEnv* env;
	(*c_jvm)->AttachCurrentThread(c_jvm, &env, NULL);
//	log_kl("10");
	//jclass cls = (*env)->FindClass(env, "show360/Extractor");
	if (c_class_Extractor == NULL)
	{
		log_kl("has not found show360/Extractor");
	}
//	log_kl("20");
	jmethodID methodId = (*env)->GetMethodID(env, c_class_Extractor,
			"fileSaveFinished", "()V");
//	log_kl("30");
//	if (NULL == c_thiz)
//	{
//		log_kl("31");
//	}
//	else
//	{
//		log_kl("32");
//	}
//	if (NULL == methodId)
//	{
//		log_kl("33");
//	}
//	else
//	{
//		log_kl("34");
//	}
	(*env)->CallVoidMethod(env, c_thiz, methodId);
//	log_kl("50");
	(*env)->DeleteGlobalRef(env, c_thiz);
//	log_kl("51");
	(*env)->DeleteGlobalRef(env, c_class_Extractor);
	//(*env)->DeleteGlobalRef(env,c_jvm);
//	log_kl("52");
	(*c_jvm)->DetachCurrentThread(c_jvm);
//	log_kl("100");
	release();
}

void Java_test_Main_testFFmpeg()
{
//	test();
//	decode();
	log_kl("kl");
	decode2pixel();
}

