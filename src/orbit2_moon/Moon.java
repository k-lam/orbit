package orbit2_moon;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Vector;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.opengl.ETC1Util;
import android.opengl.GLES10;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class Moon extends Activity {

	private GLSurfaceView mGLSurfaceView;
	Renderer render;

	int disPerFrame = 20;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//这个公式应该根据用户体验而修改
		disPerFrame = getResources().getDisplayMetrics().widthPixels / 360 * 10;
		
		mGLSurfaceView = new GLSurfaceView(this);
		render = new Renderer("pkm/g", 1, 31, 1);
		mGLSurfaceView.setRenderer(render);
		mGLSurfaceView.post(new Runnable() {

			@Override
			public void run() {
				mGLSurfaceView
						.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
			}
		});
		setContentView(mGLSurfaceView);
		final GestureDetector gd = new GestureDetector(this,
				new GestureDetector.SimpleOnGestureListener() {
					float lastDX;

					@Override
					public boolean onScroll(MotionEvent e1, MotionEvent e2,
							float distanceX, float distanceY) {
						lastDX += distanceX;
						int i = (int) (lastDX / disPerFrame);
						lastDX %= disPerFrame;
						if (i > 0) {
							while (i-- != 0) {
								render.toLeft();
							}
						} else if (i < 0) {
							while (i++ != 0) {
								render.toRight();
							}
						}
						return true;
					}

					@Override
					public boolean onFling(MotionEvent e1, MotionEvent e2,
							float velocityX, float velocityY) {
						// s = v * v / 2a
						int i = (int) (velocityX * velocityX / 500000 / disPerFrame);
						Log.i("debug", "velocityX:" + velocityX + " i:" + i);
						while (i-- != 0) {
							if (velocityX > 0) {
								render.toLeft();
							} else {
								render.toRight();
							}
						}
						return true;
					}

				});
		mGLSurfaceView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				gd.onTouchEvent(event);
				return true;
			}
		});
	}

	// Runnable runnable = new Runnable() {
	//
	// @Override
	// public void run() {
	// mGLSurfaceView.requestRender();
	// mGLSurfaceView.postDelayed(this, 100);
	// }
	// };

	@Override
	public void onResume() {
		super.onPause();
		mGLSurfaceView.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		mGLSurfaceView.onPause();
	}

	class Renderer implements GLSurfaceView.Renderer {

		FloatBuffer vertices;
		FloatBuffer texture;
		ShortBuffer indices;
		int[] textures;

		String prefix;
		int from;
		int count;
		int form;
		int textureIndex;

		/**
		 * @param prefix
		 *            路径 包括前缀 如png/g1.png,前缀应该是 png/g
		 * @param from
		 * @param count
		 * @param form
		 *            0是png，1是pkm
		 */
		public Renderer(String prefix, int from, int count, int form) {
			this();
			this.prefix = prefix;
			this.form = form;
			this.from = from;
			this.count = count;
			textureIndex = count - 1;
		}

		private Renderer() {
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 2 * 4);
			byteBuffer.order(ByteOrder.nativeOrder());
			vertices = byteBuffer.asFloatBuffer();
			vertices.put(new float[] { -1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f });
			ByteBuffer indicesBuffer = ByteBuffer.allocateDirect(6 * 2);
			indicesBuffer.order(ByteOrder.nativeOrder());
			indices = indicesBuffer.asShortBuffer();
			indices.put(new short[] { 0, 1, 2, 1, 2, 3 });

			ByteBuffer textureBuffer = ByteBuffer.allocateDirect(4 * 2 * 4);
			textureBuffer.order(ByteOrder.nativeOrder());
			texture = textureBuffer.asFloatBuffer();
			texture.put(new float[] { 0, 1f, 1f, 1f, 0f, 0f, 1f, 0f });

			indices.position(0);
			vertices.position(0);
			texture.position(0);

		}

		@Override
		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			textures = new int[count];
			gl.glGenTextures(count, textures, 0);
			if (form == 0) {
				BitmapFactory.Options options = new Options();
				options.inSampleSize = 9;
				options.inScaled = true;
				Bitmap bitmap = null;
				int j = 0;
				for (int i = from; i != from + count; i++) {
					try {
						bitmap = BitmapFactory.decodeStream(getAssets().open(
								prefix + i + ".png"));
						gl.glTexParameterf(GL10.GL_TEXTURE_2D,
								GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
						gl.glTexParameterf(GL10.GL_TEXTURE_2D,
								GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
						gl.glTexParameterf(GL10.GL_TEXTURE_2D,
								GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
						gl.glTexParameterf(GL10.GL_TEXTURE_2D,
								GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
						gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[j++]);
						GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
						bitmap.recycle();
					} catch (Exception e) {
						Log.e("debug", e.getMessage());
						e.printStackTrace();
					}
				}
			} else if (form == 1) {
				InputStream inputStream = null;
				int j = 0;
				for (int i = from; i != from + count; i++) {
					try {
						gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[j++]);
						gl.glTexParameterf(GL10.GL_TEXTURE_2D,
								GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
						gl.glTexParameterf(GL10.GL_TEXTURE_2D,
								GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
						gl.glTexParameterf(GL10.GL_TEXTURE_2D,
								GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
						gl.glTexParameterf(GL10.GL_TEXTURE_2D,
								GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
						inputStream = getAssets().open(prefix + i + ".pkm");
						ETC1Util.loadTexture(GLES10.GL_TEXTURE_2D, 0, 0,
								GLES10.GL_RGB, GLES10.GL_UNSIGNED_BYTE,
								inputStream);
						inputStream.close();
					} catch (IOException e) {
						Log.e("debug", e.getMessage());
						e.printStackTrace();
					}
				}
			}
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vertices);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texture);
		}

		@Override
		public void onSurfaceChanged(GL10 gl, int width, int height) {
			gl.glViewport(0, (mGLSurfaceView.getHeight() - 432) / 2, 720, 432);
			gl.glMatrixMode(GL10.GL_MODELVIEW);
			gl.glLoadIdentity();
			gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
			gl.glMatrixMode(GL10.GL_PROJECTION);
			gl.glLoadIdentity();
			gl.glOrthof(-1f, 1f, -1f, 1f, 10, -10);
			gl.glEnable(GL10.GL_TEXTURE_2D);
		}

		Vector<Integer> msgs_index = new Vector<Integer>();

		void getNextTid() {
			if (++textureIndex >= textures.length) {
				textureIndex = 0;
			}
		}

		public int toLeft() {
			Log.i("debug", "toLeft");
			if (++textureIndex >= textures.length) {
				textureIndex = 0;
			}
			msgs_index.add(textureIndex);
			mGLSurfaceView.requestRender();
			return textureIndex;
		}

		public int toRight() {
			Log.i("debug", "toRight");
			if (--textureIndex < 0) {
				textureIndex = textures.length - 1;
			}
			msgs_index.add(textureIndex);
			mGLSurfaceView.requestRender();
			return textureIndex;
		}

		@Override
		public void onDrawFrame(GL10 gl) {
			int index = textureIndex;
			if (msgs_index.size() != 0) {
				if (msgs_index.size() >= 5) {
					try {
						Log.e("debug", "skip frame");
						// 选择跳历史最旧的4帧是可以接受的，而且效果挺好
						index = msgs_index.remove(0);
						index = msgs_index.remove(0);
						index = msgs_index.remove(0);
						index = msgs_index.remove(0);
						index = msgs_index.remove(0);
					} catch (Exception ex) {

					}
				} else {
					index = msgs_index.remove(0);
				}
				if (msgs_index.size() > 0) {
					Log.d("debug", "remain:" + msgs_index.size());
				}
			} else {
				Log.e("debug", "msg_index is empty");
			}
			mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
			gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[index]);
			gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 6,
					GL10.GL_UNSIGNED_SHORT, indices);
			Log.i("debug", "" + index);
			if (msgs_index.size() != 0) {
				mGLSurfaceView.requestRender();
			}
		}
	}
}
