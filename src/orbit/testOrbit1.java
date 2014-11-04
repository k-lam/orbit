package orbit;


import java.io.File;

import android.app.Activity;
import android.os.Bundle;

import com.example.orbit.R;

public class testOrbit1 extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//以下是orbit的测试
		//setContentView(R.layout.test);
		//V360Show orbit = (V360Show) findViewById(R.id.orbit_);
		File path = new File(getFilesDir(), "kltmp");	
		//orbit.initBitmapLoop(29, path.getAbsolutePath()+"/", "wtmp");
	}

}
