/*
 * decoder.c
 *
 *  Created on: 2014-5-14
 *  Author: KL
 */

#include <stdio.h>
#include <stdlib.h>
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>
#include <time.h>
//#include <pthread.h>
#include <android/log.h>
#include <string.h>
//#define CHECK_ERR(ERR) {if ((ERR)<0) return -1; }

//static int remain = -1;
//static int readframe_finish = 0;
void log_kl(char * text)
{
	//char * text = "hello world printf in native";
	__android_log_write(3, "kldebug", text);
}
void createFramePath(char *path, const char *pfx, char * tmp, char *extention)
{
	//int length = strlen(path) + strlen(pfx) + 10;
	//char tmp[ strlen(path) + strlen(pfx) + 10];
	strcpy(tmp, path);
	strcat(tmp, pfx);
	strcat(tmp, "%d");
	if (extention != NULL)
	{
		strcat(tmp, extention);
	}
//	log_kl(tmp);
	//return tmp;
}

struct SaveParams
{
	AVFrame *pFrame;
	char *path;
	int iFrame;
	int height;
};

struct SaveParamsPNG
{
	unsigned char *data;
	char *path;
	char *pfx;
	int iFrame;
	int height;
	int width;
//	pthread_mutex_t mutex;
};

void SaveFrame(AVFrame *pFrame, char *path, int iFrame, int height)
{
	FILE *pFile;
	char szFilename[50];
	//sprintf(szFilename, "/mnt/sdcard/kltmp/watch%d", iFrame);
	sprintf(szFilename, path, iFrame);
//	log_kl(szFilename);
	// Open file
	long start = clock();
	pFile = fopen(szFilename, "wb");
	long finish = clock();
	__android_log_print(3, "debug", "open file used:%f",
			(double) (finish - start) / CLOCKS_PER_SEC);
	if (pFile == NULL)
		return;

	start = clock();
	fwrite(pFrame->data[0], 1, pFrame->linesize[0] * height, pFile);

//	__android_log_print(3, "debug", "length: %d", pFrame->linesize[0] * height);
	int y = 0;
	finish = clock();
	__android_log_print(3, "debug", "write file used:%f",
			(double) (finish - start) / CLOCKS_PER_SEC);
//	__android_log_print(3, "debug", "y is %d",y);
	// Close file
	fclose(pFile);
}

void *SaveFrame2(struct SaveParams *params)
{
	SaveFrame(params->pFrame, params->path, params->iFrame, params->height);
	return NULL;
}

void *SaveFramePNG2(void *paramss)
{
	//unsigned char *data, int width, int height, int iFrame, const char* outFolder,const char* pfx)
	//log_kl("SaveFramePNG2(struct SaveParamsPNG *params) called");
	struct SaveParamsPNG *params = (struct SaveParamsPNG *) paramss;
	//__android_log_print(3, "kldebug", "in SaveFramePNG2 : data[20]: %u",
//			params->data[20]);
	SaveFramePNG(params->data, params->width, params->height, params->iFrame,
			params->path, params->pfx);
	return NULL;
}

/**
 * @return -1,出错.>0 返回解压出的图片数
 */
int decode2pixel(char *videoPath, char *framePath, char *framePfx,
		int maxFrameCount, int *width, int *height)
{
	long lstrat = clock();
//	__android_log_print(3, "debug", "maxFrameCount is %d", maxFrameCount);
	AVFormatContext *pFormatCtx = NULL;
	int i, videoStream;
	AVCodecContext *pCodecCtx = NULL;
	AVCodec *pCodec = NULL;
	AVFrame *pFrame = NULL;
	AVFrame *pFrameRGB = NULL;
	AVPacket packet;
	int frameFinished = 0;
	int numBytes;
	uint8_t *buffer;
	// Register all formats and codecs
	av_register_all();

	// Open video file
	if (avformat_open_input(&pFormatCtx, videoPath, NULL, NULL) != 0)
	{
		log_kl("1");
		return -1; // Couldn't open file
	}

	// Find the first video stream
	videoStream = -1;
	for (i = 0; i < pFormatCtx->nb_streams; i++)
	{
		if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO)
		{
			videoStream = i;
//			log_kl("30");
			break;
		}
	}
	if (videoStream == -1)
	{
		log_kl("40");
		return -1; // Didn't find a video stream
	}

	// Get a pointer to the codec context for the video stream
	pCodecCtx = pFormatCtx->streams[videoStream]->codec;
//	log_kl("43");
	// Find the decoder for the video stream
	pCodec = avcodec_find_decoder(pCodecCtx->codec_id);
//	log_kl("44");
	if (pCodec == NULL)
	{
		//fprintf(stderr, "Unsupported codec!\n");
		log_kl("50");
		return -1; // Codec not found
	}
	// Open codec
	if (avcodec_open2(pCodecCtx, pCodec, NULL) < 0)
	{
		log_kl("60");
		return -1; // Could not open codec
	}
//	log_kl("62");
	// Allocate video frame
	pFrame = av_frame_alloc();
	if (pFrame == NULL)
	{
		log_kl("63");
	}
	// Allocate an AVFrame structure
	pFrameRGB = av_frame_alloc();
	if (pFrameRGB == NULL)
	{
		log_kl("70");
		return -1;
	}
//	log_kl("71");
	// Determine required buffer size and allocate buffer
	//AV_PIX_FMT_ARGB
	numBytes = avpicture_get_size(AV_PIX_FMT_BGR24, pCodecCtx->width,
			pCodecCtx->height);
	if (numBytes == 0)
	{
		log_kl("71.5");
	}
//	log_kl("72");
	buffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));
	if (buffer == NULL)
	{
		log_kl("72.5");
	}
//	log_kl("73");
	// Assign appropriate parts of buffer to image planes in pFrameRGB
	// Note that pFrameRGB is an AVFrame, but AVFrame is a superset
	// of AVPicture
	if (avpicture_fill((AVPicture *) pFrameRGB, buffer, AV_PIX_FMT_BGR24,
			pCodecCtx->width, pCodecCtx->height) < 0)
	{
		log_kl("73.5");
		return -1;
	}
//	log_kl("74");
	if (AV_PIX_FMT_NONE == pCodecCtx->pix_fmt)
	{
//		log_kl("fffffffffffffffffffff*ck");
		pCodecCtx->pix_fmt = AV_PIX_FMT_YUV444P;
	} //AV_PIX_FMT_BGR24
	struct SwsContext *sws_ctx = sws_getContext(pCodecCtx->width,
			pCodecCtx->height, pCodecCtx->pix_fmt, pCodecCtx->width,
			pCodecCtx->height, AV_PIX_FMT_BGR24, SWS_BICUBIC, NULL, NULL, NULL);
//	log_kl("120");
	*width = pCodecCtx->width;
	*height = pCodecCtx->height;
	// Read frames and save first five frames to disk
	i = 0;
	int ok = 0;
	double total = 0.0;
	int data_len = pCodecCtx->height * pCodecCtx->width * 3;
	__android_log_print(3, "kldebug", "ready used:%f",(double) (clock() - lstrat) / CLOCKS_PER_SEC);
	while (av_read_frame(pFormatCtx, &packet) >= 0)
	{
		//__android_log_print(3, "debug", "read count is %d， frameFinished is %d",ok++,frameFinished);
		// Is this a packet from the video stream?
		if (packet.stream_index == videoStream)
		{
			long l1 = clock();
			// Decode video frame
			avcodec_decode_video2(pCodecCtx, pFrame, &frameFinished, &packet);
			//__android_log_print(3, "debug", "read count is %d, u is %d,frameFinished is %d",ok++,u,frameFinished);
			// Did we get a video frame?
			if (frameFinished)
			{
				// Convert the image from its native format to RGB
				sws_scale(sws_ctx, (uint8_t const * const *) pFrame->data,
						pFrame->linesize, 0, pCodecCtx->height, pFrameRGB->data,
						pFrameRGB->linesize);
//				__android_log_print(3, "debug", "read frame used %f",
//						(double) (clock() - l1) / CLOCKS_PER_SEC);
				total += ((double) (clock() - l1) / CLOCKS_PER_SEC);
				// Save the frame to disk
				if (i <= maxFrameCount)
				{
					SaveFramePNG(pFrameRGB->data[0],pCodecCtx->width,pCodecCtx->height,i,framePath,framePfx);
					i++;
				}
				else
				{
					break;
				}
			}
		}
		// Free the packet that was allocated by av_read_frame
		av_free_packet(&packet);
	}
	setFinish();
	__android_log_print(3, "kldebug", "sws total used:%f",total);
	// Free the RGB image
	av_free(buffer);
	av_free(pFrameRGB);

	// Free the YUV frame
	av_free(pFrame);

	// Close the codec
	avcodec_close(pCodecCtx);

	// Close the video file
	avformat_close_input(&pFormatCtx);

	return i;
}

