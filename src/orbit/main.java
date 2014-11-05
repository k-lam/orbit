package orbit;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import orbit.Extractor.ExtractFinishedListener;
import orbit.Extractor.Result;

import com.example.orbit.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class main extends Activity implements OnClickListener{
	
	final static String AVINAME = "video.avi";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		findViewById(R.id.btn_del).setOnClickListener(this);
		findViewById(R.id.btn_extract).setOnClickListener(this);
		findViewById(R.id.btn_moon).setOnClickListener(this);
		findViewById(R.id.btn_orbit).setOnClickListener(this);
		
		boolean flag = false;
		
		for(String name : fileList()){
			if(name.equals(AVINAME)){
				flag = true;
				break;
			}
		}
		FileOutputStream os = null;
		InputStream is = null;
		if(!flag){
			try {
				os = openFileOutput(AVINAME, 0);
				is = getAssets().open("avi/"+AVINAME);
				byte[] buf = new byte[1024];
				int bytesRead;
				while ((bytesRead = is.read(buf)) > 0) {
					os.write(buf, 0, bytesRead);
				}
				os.flush();
				is.close();
				os.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.btn_extract:
			Extractor extractor = new Extractor();
			extractor.extractPic(getFilesDir().getAbsolutePath() + "/"+ AVINAME,
					getFilesDir().getAbsolutePath(), "wtmp", 50,new ExtractFinishedListener() {
						
						@Override
						public void run(Result result) {
							Toast.makeText(main.this, "Íê³É", Toast.LENGTH_SHORT).show();
						}
					});
			//extractor.
			break;
		case R.id.btn_del:
			break;
		case R.id.btn_orbit:
			break;
		case R.id.btn_moon:
			break;
		default:
			break;
		}
	}

}
