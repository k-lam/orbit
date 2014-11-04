package orbit;

import java.util.concurrent.Callable;
import bolts.Continuation;
import bolts.Task;
import android.os.Handler;
import android.util.Log;

public class Extractor {
	long l1 = 0;
	static{
		System.loadLibrary("orbit");
	}
	
	/**
	 * ��������ᴥ�����̲߳��������������������ʱ��������ζ�Ŷ��̲߳����Ľ��������̵߳Ľ����ᷢ
	 * 
	 * @param videoPath
	 *            video�ľ���·���������ļ���
	 * @param framePath
	 *            ��ѹ������frame��·��
	 * @param framePfx
	 *            ��ѹ�������ļ�ǰ׺���ļ������� framePfx%d,����%d����ţ���0��ʼ��
	 * @param maxFrameCount
	 *            ���֡��������Ԥ����һ���ܴ����Ƶ����ѹ���ܶ�֡
	 * @return
	 */
	public native Result extract(String videoPath, String framePath,
			String framePfx, int maxFrameCount);

	private Handler mHandler;


	public Extractor() {
		// System.load("orbit");
		mHandler = new Handler() {

			@Override
			public void handleMessage(android.os.Message msg) {
				Log.d("debug","get send back ,"+ Thread.currentThread().getId());
				
			}
		};
	}
	
	

	public void extractPic(final String videoPath, final String framePath,
			final String framePfx, final int maxFrameCount,
			final ExtractFinishedListener finishedListener) {
		l1 = System.currentTimeMillis();
		Task.callInBackground(new Callable<Result>() {

			@Override
			public Result call() throws Exception {
				long l1 = System.currentTimeMillis();
				Result result = extract(videoPath, framePath, framePfx, maxFrameCount);
				Log.d("debug","extract total used:" + (System.currentTimeMillis() - l1));
				return result;
			}
		}).continueWith(new Continuation<Extractor.Result, Void>() {

			@Override
			public Void then(Task<Result> task) throws Exception {
				if (null != finishedListener) {
					finishedListener.run(task.getResult());
				}
				return null;
			}
		}, bolts.Task.UI_THREAD_EXECUTOR);
	}
	
	public void fileSaveFinished(){
	  //Log.d("debug", "fileSaveFinished call in java, " + Thread.currentThread().getId());
		Log.d("debug","save come back,total used:" + (System.currentTimeMillis() - l1));
		mHandler.sendEmptyMessage(11);
	}

	public interface ExtractFinishedListener {
		// Result result;
		// public ExtractFinishedListener(Result result){
		// this.result = result;
		// }
		void run(Result result);
	}

	// public class show360.Extractor$Result {
	// int width;
	// Signature: I
	// int height;
	// Signature: I
	// int frameCount;
	// Signature: I
	// public show360.Extractor$Result(int, int, int);
	// Signature: (III)V
	// }
	/**
	 * 
	 * @author K.Lam
	 */
	public static class Result {
		public int width;
		public int height;
		public int frameCount;

		public Result(int width, int height, int frameCount) {
			this.width = width;
			this.height = height;
			this.frameCount = frameCount;
		}
	}
}
