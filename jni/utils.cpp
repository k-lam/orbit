/*
 * uitls.cpp
 *
 *  Created on: 2014-5-23
 *      Author: KL
 */
extern "C"
{
#include <android/log.h>
#include <pthread.h>
#define P_COUNT 15
int __android_log_write(int prio, const char *tag, const char *text);
int __android_log_print(int prio, const char *tag, const char *fmt, ...);
int pthread_mutex_lock(pthread_mutex_t *mutex);
int pthread_mutex_unlock(pthread_mutex_t *mutex);
//int remain;
//int readframe_finish;
void sendBack();
}
#include <orbit.h>
#include <opencv2/opencv.hpp>
#include <time.h>
using namespace std;
extern "C" void SaveFramePNG(unsigned char *data, int width, int height,
		int iFrame, const char* outFolder, const char* pfx);
extern "C" void setFinish();
struct saveParams
{
	char *fileName;
	cv::Mat *image;
	unsigned char *data;
	//int index;
};
double saveFile = 0.0;
pthread_mutex_t mutex_saved_add = PTHREAD_MUTEX_INITIALIZER;

//bool hasSend = false;
//hasSend用的
pthread_mutex_t mutex_send = PTHREAD_MUTEX_INITIALIZER;
struct saveParams *params[P_COUNT];

void sendFinish()
{
	pthread_mutex_lock(&mutex_send);
	if(hasSend != 1 && readFrame_finish == 1 && need_save_count == saved_count){
		hasSend = 1;
		sendBack();
	}
	pthread_mutex_unlock(&mutex_send);
}
void setFinish()
{
	pthread_mutex_lock(&mutex_send);
	readFrame_finish = 1;
	pthread_mutex_unlock(&mutex_send);
	sendFinish();
}

int requestAParam(int height, int width, unsigned char *data)
{
//	__android_log_print(3, "kldebug", "requestAParam");
	int i;
	//index = NULL;
	for (i = 0; i != P_COUNT; i++)
	{
		if (params[i] == NULL)
		{
			params[i] = (struct saveParams *) malloc(sizeof(struct saveParams));
			int data_len = height * width * 3;
			params[i]->data = (unsigned char *) malloc(
					data_len * sizeof(unsigned char));
			memcpy(params[i]->data, data, data_len);
//			__android_log_print(3, "kldebug", "index:%d is NULL", i);
			params[i]->image = new cv::Mat(height, width, CV_8UC3,
					params[i]->data);
			//*index = i;
			//params[i]->index = i;
			return i;
		}
//		else if (i == 10 && !(*params[i]->image).empty())
//		{
//			__android_log_print(3, "kldebug",
//					"index:%d is not NULL image is %d", i,
//					*(*params[i]->image).refcount);
//		}
	}
	return -1;
}

void *saveInDisk(void *ps)
{
	long l1 = clock();
	int i = (int) ps;
//	__android_log_print(3, "kldebug", "index:%d", i);

	if (cv::imwrite(params[i]->fileName, *(params[i]->image)))
	{
		__android_log_print(3, "kldebug", "write file %s ok ",
				params[i]->fileName);
	}
	else
	{
		__android_log_print(3, "kldebug", "write file %s failed",
				params[i]->fileName);
	}
	pthread_mutex_lock(&mutex_saved_add);
	++saved_count;
	pthread_mutex_unlock(&mutex_saved_add);
	//释放资源
	//Matrix
	(*params[i]->image).release();
	delete params[i]->image;

	free(params[i]->data);
	//fileName
	free(params[i]->fileName);
	params[i]->fileName = NULL;
	//free(params->index);

	free(params[i]);
//	if (params[i] != NULL)
//	{
//		__android_log_print(3, "kldebug", "params[%d] is not NULL after free",
//				i);
//	}
	params[i] = NULL;
//	if (params[i] != NULL)
//	{
//		__android_log_print(3, "kldebug",
//				"params[%d] is not NULL after set NULL", i);
//	}
	saveFile += ((double) (clock() - l1) / CLOCKS_PER_SEC);
//	__android_log_print(3, "kldebug", "index %d,total %f", i, saveFile);
	sendFinish();
	return NULL;
}

void SaveFramePNG(unsigned char *data, int width, int height, int iFrame,
		const char* outFolder, const char* pfx)
{

	//__android_log_print(3, "kldebug", "save call %d", iFrame);
	int i = requestAParam(height, width, data);
	if (i == -1)
	{
		__android_log_print(3, "kldebug",
				"an error has occurted in utils.cpp SaveFramePNG kl2310");
		return;
	}
	params[i]->fileName = (char *) malloc(128 * sizeof(char));
	int y;
	sprintf(params[i]->fileName, "%s/%s%d.png", outFolder, pfx, iFrame);

//	int step = 3 * width;
//	for (y = 0; y < height; y++)
//	{
//		//unsigned char *ptr = (*params[i]->image).ptr + y * step;
//		for (int x = 0; x < width; x++)
//		{
//			//*(ptr++) = data(
////			image.data[y * step + x * 3 + 2] = data[y * step + x * 3];
////			image.data[y * step + x * 3 + 1] = data[y * step + x * 3 + 1];
////			image.data[y * step + x * 3] = data[y * step + x * 3 + 2];
////			(*params[i]->image).data[y * step + x * 3 + 2] = data[y * step
////					+ x * 3];
////			(*params[i]->image).data[y * step + x * 3 + 1] = data[y * step
////					+ x * 3 + 1];
////			(*params[i]->image).data[y * step + x * 3] = data[y * step + x * 3
////					+ 2];
//			//__android_log_print(3,"kldebug","2500   %u yeah",(uchar)(data[y*step+x*3]));
//			//        	 __android_log_print(3,"kldebug","image(%d,%d)",y, x);
//
//			//            image.at<uchar>(y, x, 0) = (uchar)(data[y*step+x*3]);
//			//            image.at<uchar>(y, x, 1) = (uchar)( data[y*step+x*3+1] );
//			//        image. at<uchar>(y, x, 2) = (uchar)(data[y*step+x*3+2]);
//			//            __android_log_print(3,"kldebug","image(%d,%d)=%d,%d,%d",y, x,image.at<uchar>(y, x, 0), image.at<uchar>(y, x, 1), image.at<uchar>(y, x, 2));
//			//            __android_log_print(3,"kldebug","image(%d,%d)",y, x);
//
//		}
//	}
	//struct saveParams ps = {szFilename,image};
	//因为这个数是 >= saved_count的，所以可以不加锁
	need_save_count++;
//	pthread_mutex_lock(&mutex);
//	pthread_mutex_unlock(&mutex);
	pthread_t tid;
	pthread_create(&tid, NULL, saveInDisk, (void *) (i));
}

