package orbit;

import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import bolts.Continuation;
import bolts.Task;
import bolts.Task.KLTask;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class BitmapLoop2 {
	static int controlID = 0;

	// AtomicInteger diskPointer;

	int diskPointer_l;
	int diskPointer_r;

	int count;

	/**
	 * -1:��0��δ�ƶ���1���ҡ�ֻ��ui�߳��޸�
	 */
	int toward = 0;

	/**
	 * �������
	 */
	TasksManager tasksManager = new TasksManager();

	final static int CPU_COUNT = Runtime.getRuntime().availableProcessors();

	/**
	 * ������������������
	 */
	final static int WINDOW_CAPACITY = CPU_COUNT > 2 ? 9 : 7;

	final static int Full_Main = (WINDOW_CAPACITY / 2 + CPU_COUNT) >= WINDOW_CAPACITY ? (WINDOW_CAPACITY / 2 + 2)
			: (WINDOW_CAPACITY / 2 + CPU_COUNT);

	final static int Full_Anti = WINDOW_CAPACITY - Full_Main;
	// final static int Mid = WINDOW_CAPACITY / 2;
	LinkedList<BitmapWithName> bitmapWindow = new LinkedList<BitmapWithName>();

	public class BitmapWithName {
		Bitmap bitmap;
		String name;

		public BitmapWithName(Bitmap bitmap, String name) {
			this.bitmap = bitmap;
			this.name = name;
		}
	}

	/**
	 * ������ʾbitmapWindow����һ��ͼƬ
	 */
	AtomicInteger showing = new AtomicInteger(WINDOW_CAPACITY / 2);

	public BitmapLoop2(Context context, int count) {
//		this.count = count;
//		// diskPointer = new AtomicInteger(count / 2);
//		diskPointer_l = count / 2 - WINDOW_CAPACITY / 2;
//		diskPointer_r = count / 2 + WINDOW_CAPACITY / 2;
//		print("l:" + diskPointer_l + " r:" + diskPointer_r);
//		int start = count / 2 - WINDOW_CAPACITY / 2;
//		for (int i = 0; i != WINDOW_CAPACITY; i++) {
//			String name = getName(start++);
//			bitmapWindow.add(new BitmapWithName(
//					getBitmapFromDisk(context, name), name));
//		}
		// print("showing:"+showing.get());
		// print("full:" + Full_Main);
	}
	
	public BitmapLoop2(Context context, int count,String path,String pfx){
		setSrc(path, pfx);
		this.count = count;
		// diskPointer = new AtomicInteger(count / 2);
		diskPointer_l = count / 2 - WINDOW_CAPACITY / 2;
		diskPointer_r = count / 2 + WINDOW_CAPACITY / 2;
		print("l:" + diskPointer_l + " r:" + diskPointer_r);
		int start = count / 2 - WINDOW_CAPACITY / 2;
		for (int i = 0; i != WINDOW_CAPACITY; i++) {
			String name = getName(start++);
			bitmapWindow.add(new BitmapWithName(
					getBitmapFromDisk(context, name), name));
		}
	}

	public Bitmap getCurBitmap() {
		return bitmapWindow.get(showing.get()).bitmap;
	}

	int getDiskPointer(int toward) {
		return toward == -1 ? diskPointer_l : diskPointer_r;
	}


	LoadInfo[] caculateShouldLoad2() {
		// 4����Ҫԭ��
		// 1.������loading + rest������full
		// 2.���Ҫload��������LoadSum = full - loading - rest
		// 3.LoadSum = l1 + l2 + l3... ���showing��߽�ܽ���Ҳ����Window�����rest���٣�loading
		// = 0���������Ӧ
		// �����ȶ�һ��ͼƬ������ʾ��Ȼ���ٶ�2 ��3��������ȥֱ��������LoadSum
		// 4.showing��δ�ƶ���showingλ�ã��������reverse��toward��δreverse��toward
		// ���������
		// 1.���ڷ���reverse��Υ����Ҫԭ��1�ģ�������������Ҫԭ��1�������loadingҪcancel��

		// ����ܽᣬֻ��һ������mainLoading - antiLoading + mainRest <=
		// Full_main����cancel��add�Ľ���
		int antiLoading = 0;
		// int antiRest = 0;
		int mainRest = 0;
		int mainLoading = 0;
		int showing = this.showing.get();
		// int toward = hasReversed ? -this.toward:this.toward;
		if (-1 == toward) {
			antiLoading = tasksManager.getRloadingCount();
			mainLoading = tasksManager.getLloadingCount();
			// antiRest = WINDOW_CAPACITY - showing - 1;
			mainRest = showing;
		} else {
			antiLoading = tasksManager.getLloadingCount();
			mainLoading = tasksManager.getRloadingCount();
			// antiRest = showing;
			mainRest = WINDOW_CAPACITY - showing - 1;
		}

		int need = Full_Main - mainRest - mainLoading + antiLoading;// ���㻹����ٲ��ܵ�Full
		if (need > 0) {
			// ��cancel��add
			if (antiLoading > 0) {
				LoadTask task;
				//����  tasksManager.getLloadingCount()��tasksManager.getRloadingCount()��Щ��
				//�жϣ���for
				for (int i = tasksManager.tasksQueue.size() - 1; i != -1
						&& need > 0; i--) {
					task = tasksManager.tasksQueue.get(i);
					if (task.info.toward == -toward) {
						task.isCanceled = true;
						tasksManager.removeTask(task);
//						tasksManager.tasksQueue.remove(task);
//						tasksManager.removeLoadingCount(task.info);
						need -= task.info.getCount();
						if (-1 == toward) {
							diskPointer_l = getRightIndex(diskPointer_l,
									task.info.getCount());
						} else {
							diskPointer_r = getLeftIndex(diskPointer_r,
									task.info.getCount());
						}
						print("cancel: " + task.info.getLoadIndex()
								+ "diskPointer_l:" + diskPointer_l
								+ ",diskPointer_r:" + diskPointer_r);
					}
				}
			}
			// ���Ǵ���0��add��
			if (need > 0) {
				LoadInfo[] infos = new LoadInfo[] { new LoadInfo(need, toward) };
				infos[0].setLoadIndexs(getDiskPointer(toward), need);
				if (-1 == toward) {
					diskPointer_l = getLeftIndex(diskPointer_l, need);
				} else {
					diskPointer_r = getRightIndex(diskPointer_r, need);
				}
				print("add:" + infos[0].getLoadIndex() + "diskPointer_l:"
						+ diskPointer_l + ",diskPointer_r:" + diskPointer_r);
				return infos;
			}
		} else {// need < 0,������
				// ��cancel��add
			if (mainLoading > 0) {
				LoadTask task;
				for (int i = tasksManager.tasksQueue.size() - 1; i != -1
						&& need > 0; i--) {
					task = tasksManager.tasksQueue.get(i);
					if (task.info.toward == toward) {
						task.isCanceled = true;
//						tasksManager.tasksQueue.remove(task);
//						tasksManager.removeLoadingCount(task.info);
						tasksManager.removeTask(task);
						need += task.info.getCount();
						if (-1 == toward) {
							diskPointer_l = getRightIndex(diskPointer_l,
									task.info.getCount());
						} else {
							diskPointer_r = getLeftIndex(diskPointer_r,
									task.info.getCount());
						}
						print("cancel: " + task.info.getLoadIndex()
								+ "diskPointer_l:" + diskPointer_l
								+ ",diskPointer_r:" + diskPointer_r);
					}
				}
			}
			// ����С��0��add��
			if (need < 0) {
				LoadInfo[] infos = new LoadInfo[] { new LoadInfo(-need, -toward) };
				infos[0].setLoadIndexs(getDiskPointer(-toward), -need);
				if (-1 == toward) {
					diskPointer_l = getLeftIndex(diskPointer_l, -need);
				} else {
					diskPointer_r = getRightIndex(diskPointer_r, -need);
				}
				print("add:" + infos[0].getLoadIndex() + "diskPointer_l:"
						+ diskPointer_l + ",diskPointer_r:" + diskPointer_r);
				return infos;
			}
		}
		return null;
	}

	String pfx;
	public void setSrc(String path,String pfx){
		this.pfx = path + pfx;
	}

	String getName(int index) {
//		if (index < 10) {
//			return "0" + index + ".png";
//		} else {
//			return index + ".png";
//		}
		return pfx + index + ".png";
	}

	public Bitmap getBitmap2(int toward, final Context context) {
		// print("showing:"+showing.get());
		if (this.toward != toward && this.toward != 0) {
			print("reverse taking palce");
		}
		this.toward = toward;
		LoadInfo[] lis = caculateShouldLoad2();
		if (lis != null) {
			for (final LoadInfo li : lis) {
				final int cid = controlID++;
				if (li.toward == 0 || li.getCount() == 0) {
					continue;
				}
				// print(cid + ":toward:" + li.toward + ", count:" + li.count);
				Task.KLTask kl = new KLTask();
				Callable<BitmapWithName[]> callable = new Callable<BitmapWithName[]>() {
					//final int showing = BitmapLoop2.this.showing.get();

					@Override
					public BitmapWithName[] call() throws Exception {
//						Thread.setDefaultUncaughtExceptionHandler(((KLApplication) context
//								.getApplicationContext()).crashHandler);
						BitmapWithName[] bs = new BitmapWithName[li.getCount()];
						// ��ͼƬ
						String name = "";
						if (-1 == li.toward) {
							for (int i = 0; i != bs.length; i++) {
								name = getName(li.loadIndexs[i]);
								// print(showing + "," + i);
								bs[i] = new BitmapWithName(getBitmapFromDisk(
										context, name), name);
							}
						} else if (1 == li.toward) {
							for (int i = 0; i != bs.length; i++) {
								name = getName(li.loadIndexs[i]);
								// print(showing + "," + i);
								bs[i] = new BitmapWithName(getBitmapFromDisk(
										context, name), name);
							}
						}
						// if (bs[0] == null) {
						// print(cid + " : " + name + " is null");
						// }
						print(cid + " : " + name + " is loaded in background");
						return bs;
					}
				};
				Task<BitmapWithName[]> task = kl.create(callable);
				tasksManager.add(new LoadTask(task, li, cid));
				kl.exec(Task.BACKGROUND_EXECUTOR);
			}
		}
		int index = showing.get() + toward;
		if (index >= 0 && index < WINDOW_CAPACITY) {
			showing.addAndGet(toward);
			// if (toward == -1) {
			// diskPointerDecreate();
			// } else if (toward == 1) {
			// diskPointerIncreate();
			// }
			try{
				bitmapWindow.get(index);
			}catch(Exception exception){
				
				Log.e("debug","exception: index:" + index + ",WINDOW_CAPACITY:"+WINDOW_CAPACITY+",size:"+bitmapWindow.size()+",thread id:"+Thread.currentThread().getId());
			}
			print(controlID + " showing:" + showing.get() + ",toward:" + toward
					+ ",l:" + tasksManager.lloadingCount + ",r:"
					+ tasksManager.rloadingCount + " name:"
					+ bitmapWindow.get(index).name);
			return bitmapWindow.get(index).bitmap;
		} else {
			// showing.addAndGet(-toward);
			print(controlID + " showing:" + showing.get() + ",toward:" + toward
					+ ",l:" + tasksManager.lloadingCount + ",r:"
					+ tasksManager.rloadingCount + "   nothing show");
			return null;
		}
	}

	public Bitmap toLeft(final Context context) {
		return getBitmap2(-1, context);
	}

	public Bitmap toRight(final Context context) {
		return getBitmap2(1, context);
	}

	public int getIndex(int base, int offset, int toward) {
		return toward == -1 ? getLeftIndex(base, offset) : getRightIndex(base,
				offset);
	}

	/**
	 * @param base
	 *            disk�еģ�һ����diskPointer
	 * @param offset
	 * @return
	 */
	public int getLeftIndex(int base, int offset) {
		// print("getLeftIndex" + base + "," + offset);
		int result = base - offset;
		if (result >= 0) {
			return result;
		} else {
			return count + result;
		}
	}

	public int getRightIndex(int base, int offset) {
		// print("getRightIndex" + base + "," + offset);
		int result = base + offset;
		if (result <= count - 1) {
			return result;
		} else {
			return result - count;
		}
	}

	public Bitmap getBitmapFromDisk(Context context, String name) {
		Bitmap bitmap = null;
		//try {
//			bitmap = BitmapFactory.decodeStream(context.getAssets().open(name,
//					AssetManager.ACCESS_BUFFER));
			bitmap = BitmapFactory.decodeFile(name);
			// context.getAssets().open(name,AssetManager.ACCESS_BUFFER);
		//} catch (IOException e) {
		//	e.printStackTrace();
		//}
		// print("img name is " + name);
		return bitmap;
	}

	/**
	 * bitmap�ӵ�ͷ��������ɾ����βԪ�أ�showing + 1
	 * 
	 * @param bitmap
	 */
	public void addToHead(Bitmap bitmap, String name) {
		printW("addToHead call in thread:" + Thread.currentThread().getId() +",bitmapWindow.size():" + bitmapWindow.size());
		if (bitmapWindow.size() == WINDOW_CAPACITY) {
			BitmapWithName last = bitmapWindow.removeLast();
			printW(last.name + " in tail is removed ,bitmapWindow.size():" + bitmapWindow.size());
			last.bitmap.recycle();
			diskPointer_r = getLeftIndex(diskPointer_r, 1);
		}
		bitmapWindow.addFirst(new BitmapWithName(bitmap, name));
		printW(name + " is added in head ,bitmapWindow.size():" + bitmapWindow.size());
		if (showing.get() < WINDOW_CAPACITY - 1) {
			showing.incrementAndGet();
		}
		// print("addToHead showing is "+ showing.get());
	}

	/**
	 * ��bitmap�ӵ�����β��������ɾ����ͷ��
	 * 
	 * @param bitmap
	 */
	public void addToTail(Bitmap bitmap, String name) {
		print("addToTail call in thread:" + Thread.currentThread().getId() + ",bitmapWindow.size():" + bitmapWindow.size());
		if (bitmapWindow.size() == WINDOW_CAPACITY) {
			BitmapWithName first = bitmapWindow.removeFirst();
			print(first.name + " in head is removed ,bitmapWindow.size():" + bitmapWindow.size());
			first.bitmap.recycle();
			diskPointer_l = getRightIndex(diskPointer_l, 1);
		}
		bitmapWindow.addLast(new BitmapWithName(bitmap, name));
		printW(name + " is added in tail ,bitmapWindow.size():" + bitmapWindow.size());
		showing.decrementAndGet();
	}

	/**
	 * ������¼Ӧ��������disk����ͼƬ���������ض����ţ����ĸ�������� �������������loadInfo��¼����Ϊ��һ��trigger,���Դ����������
	 * 
	 * @author K.Lam
	 */
	public class LoadInfo {
		// int count;
		/**
		 * -1:��1����
		 */
		int toward;

		int[] loadIndexs;

		int getCount() {
			return loadIndexs == null ? 0 : loadIndexs.length;
		}

		public void setLoadIndexs(int diskPointer, int count) {
			if (loadIndexs == null) {
				loadIndexs = new int[count];
			}
			//int loading = tasksManager.getLoadingCount(toward);
			for (int i = 0; i != count; i++) {
				loadIndexs[i] = getIndex(toward == -1 ? diskPointer_l
						: diskPointer_r, i + 1, toward);
			}
		}

		String getLoadIndex() {
			String s = "";
			for (int i : loadIndexs) {
				s += i + ",";
			}
			return s;
		}

		/**
		 * �������ĸ�taskȥִ�С�
		 */
		// Task<?> task;
		public LoadInfo(int count, int toward) {
			// this.count = count;
			if (count > 0) {
				loadIndexs = new int[count];
			}
			this.toward = toward;
		}
	}

	public static class LoadTask {
		/**
		 * ��Ӳ�̼���ͼƬ
		 */
		Task<BitmapWithName[]> task;
		/**
		 * ��ͼƬ�Ž�window���С�
		 */
		Task<Void> task_resultback;
		LoadInfo info;
		int cid;
		// String name;

		/**
		 * ���Ż�
		 */
		boolean isCanceled = false;

		public LoadTask(Task<BitmapWithName[]> task, LoadInfo info) {
			this.task = task;
			this.info = info;
		}

		public LoadTask(Task<BitmapWithName[]> task, LoadInfo info, int cid) {
			this.task = task;
			this.info = info;
			this.cid = cid;
		}

		// public LoadTask(Task<BitmapWithName[]> task, LoadInfo info,int
		// cid,String name) {
		// this.task = task;
		// this.info = info;
		// this.cid = cid;
		// this.name = name;
		// }
	}

	/**
	 * ����ִ�е��������,�����Ķ���ֻ����main�̲߳���
	 * 
	 * @author K.Lam
	 */
	public class TasksManager {

		public TasksManager() {

		}

		/**
		 * ������ڼ��ص�ͼƬ����������ֻ�����̻߳�����������Բ�����
		 */
		private int lloadingCount = 0;
		/**
		 * �ұ����ڼ��ص�ͼƬ����������ֻ�����̻߳�����������Բ�����
		 */
		private int rloadingCount = 0;

		public int getLloadingCount() {
			return lloadingCount;
		}

		public int getRloadingCount() {
			return rloadingCount;
		}

		public int getLoadingCount(int toward) {
			return toward == -1 ? lloadingCount : rloadingCount;
		}

		/**
		 * ���ڼ���ͼƬ��task
		 */
		final LinkedList<LoadTask> tasksQueue = new LinkedList<LoadTask>();

		public void add(final LoadTask lt) {
			tasksQueue.add(lt);
			addLoadingCount(lt.info);
			Task<Void> task_tmp = lt.task.continueWith(
					new Continuation<BitmapWithName[], Void>() {

						@Override
						public Void then(Task<BitmapWithName[]> task)
								throws Exception {
							if (task.getError() != null || lt.isCanceled) {
								task.getError().printStackTrace();
								return null;
							}
							final BitmapWithName[] bs = task.getResult();
							if (null == bs || 0 == bs.length || null == bs[0]) {
								if (null == bs[0]) {
									print("----------------->" + lt.info.toward
											+ "," + lt.info.getCount()
											+ lt.task.isCompleted() + ","
											+ lt.task.isFaulted() + ","
											+ lt.task.isCancelled());
								}
								return null;
							}

							// add ��window����
							// if (tasksQueue.size() != 0) {
							if (tasksQueue.size() != 0
									&& !lt.equals(tasksQueue.get(0))) {
								String cid = "";
								LoadTask ltLast = null;
								for (LoadTask task2 : tasksQueue) {
									if (lt.equals(task2)) {
										break;
									}
									ltLast = task2;
									cid += task2.cid + ",";
								}
								print("I am " + lt.cid + " waiting for " + cid
										+ " completed");
								lt.task_resultback = ltLast.task_resultback
										.continueWith(new Continuation<Void, Void>() {

											@Override
											public Void then(Task<Void> task)
													throws Exception {
												String N = "";
												if (-1 == lt.info.toward) {
													for (BitmapWithName bitmap : bs) {
														addToHead(
																bitmap.bitmap,
																bitmap.name);
														N += bitmap.name + ",";
													}
												} else {
													for (BitmapWithName bitmap : bs) {
														addToTail(
																bitmap.bitmap,
																bitmap.name);
														N += bitmap.name + ",";
													}
												}
//												tasksQueue.remove(lt);
//												removeLoadingCount(lt.info);
												removeTask(lt);
												print("cid : " + lt.cid
														+ ", name " + N
														+ " is added to window");
												return null;
											}
										},Task.UI_THREAD_EXECUTOR);
							} else {
								String N = "";
								if (-1 == lt.info.toward) {
									// for (Bitmap bitmap : bs) {
									// addToHead(bitmap,lt.name);
									// }
									for (BitmapWithName bitmap : bs) {
										addToHead(bitmap.bitmap, bitmap.name);
										N += bitmap.name + ",";
									}
								} else {
									// for (Bitmap bitmap : bs) {
									// addToTail(bitmap,lt.name);
									// }
									for (BitmapWithName bitmap : bs) {
										addToTail(bitmap.bitmap, bitmap.name);
										N += bitmap.name + ",";
									}
								}
//								tasksQueue.remove(lt);
//								removeLoadingCount(lt.info);
								removeTask(lt);
								print("cid : " + lt.cid + ", name " + N
										+ " is added to window");
							}
							return null;
						}
					}, Task.UI_THREAD_EXECUTOR);
			if(null == lt.task_resultback ){
				lt.task_resultback = task_tmp;
			}
		}

		void addLoadingCount(LoadInfo li) {
			if (li != null) {
				if (-1 == li.toward) {
					lloadingCount += li.getCount();
				} else {
					rloadingCount += li.getCount();
				}
			}
		}

		void removeLoadingCount(LoadInfo li) {
			if (li != null) {
				if (-1 == li.toward) {
					lloadingCount -= li.getCount();
				} else {
					rloadingCount -= li.getCount();
				}
			}
		}
		
		void removeTask(LoadTask lt){
			tasksQueue.remove(lt);
			removeLoadingCount(lt.info);
		}

		public void removeAll() {
			LoadTask lt = null;
			while (tasksQueue.size() != 0) {
				lt = tasksQueue.poll();
				if (lt != null) {
					// lt.task.cancelled();
					lt.isCanceled = true;
					removeLoadingCount(lt.info);
				}
			}
		}

		public void removeFirst() {
			LoadTask lt = tasksQueue.poll();
			if (null != lt) {
				// lt.task.cancelled();
				lt.isCanceled = true;
				removeLoadingCount(lt.info);
			}
		}
	}

	void print(String s) {
		Log.d("debug", s);
	}
	
	void printW(String s){
		Log.d("debugW", s);
	}

}
