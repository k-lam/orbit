package orbit;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class V360Show extends ImageView {
	
	BitmapLoop2 bl;

	public V360Show(Context context) {
		super(context);
		init(context);
	}

	public V360Show(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public V360Show(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	Bitmap bitmap;
	private void init(Context context) {
		final GestureDetector gd = new GestureDetector(context,
				new GestureDetector.SimpleOnGestureListener() {

					@Override
					public boolean onScroll(MotionEvent e1, MotionEvent e2,
							float distanceX, float distanceY) {
						Log.d("debugY","onScroll:"+e1.getRawX()+","+e1.getX()+","+e1.getRawY()+","+e1.getY());
						if(null != bl){
							if (distanceX > 0.0001) {
								bitmap = bl.toRight(getContext());
							}else {
								bitmap = bl.toLeft(getContext());
								
							}
							if(null != bitmap){
								V360Show.this.setImageBitmap(bitmap);
							}
						}
						return true;
					}

					@Override
					public boolean onFling(MotionEvent e1, MotionEvent e2,
							float velocityX, float velocityY) {
						//Log.d("debug","onFling:"+velocityX);
						return super.onFling(e1, e2, velocityX, velocityY);
					}

					@Override
					public boolean onDown(MotionEvent e) {
						//Log.d("debug","onDown");
						return super.onDown(e);
					}
					
				});
		this.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				gd.onTouchEvent(event);
				return true;
			}
		});
	}
	
	public void initBitmapLoop(int count,String path,String pfx){
		bl = new BitmapLoop2(getContext(), count,path, pfx);
		//bl.setSrc(path, pfx);
		this.setImageBitmap(bl.getCurBitmap());
	}

}
