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
	 * 这个方法会触发多线程操作，而当这个方法返回时，并不意味着多线程操作的结束，多线程的结束会发
	 * 
	 * @param videoPath
	 *            video的绝对路径，包含文件名
	 * @param framePath
	 *            解压出来的frame的路径
	 * @param framePfx
	 *            加压出来的文件前缀（文件名会是 framePfx%d,其中%d是序号，从0开始）
	 * @param maxFrameCount
	 *            最大帧数，用来预防误传一个很大的视频，解压出很多帧
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
