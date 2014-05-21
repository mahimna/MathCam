package com.example.ocrproject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.leptonica.android.Enhance;
import com.googlecode.tesseract.android.TessBaseAPI;

public class MainActivity extends Activity implements View.OnClickListener{
	
	Button takePic;
	TextView convertedString, mathAnswer;
	ImageView cameraPic;
	Bitmap bmp;
	public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() +
            "/OCRProject/";

	String url_Wolfram = "http://api.wolframalpha.com/v2/query.jsp?input=";
	String apiKey = "&appid=GGG5U2-TPLKULL7EX";
	ProgressDialog pDialog;
	String recognizedText;
	XMLReader xmlReader;
	
	
	final static int cameraData = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_main);

		initialize();		
		takePic.setOnClickListener(this);
				
	}
	
	public void initialize(){
		takePic = (Button)findViewById(R.id.button1);
		mathAnswer = (TextView)findViewById(R.id.text1);
		convertedString = (TextView)findViewById(R.id.readValue);
		xmlReader = new XMLReader();
		cameraPic = (ImageView)findViewById(R.id.ivPicture);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	
	protected void onActivityResult (int requestCode, int resultCode, Intent data){
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK){
			Bundle extras = data.getExtras();
			bmp = (Bitmap)extras.get("data");
			cameraPic.setImageBitmap(bmp);
			
			File dir = new File(DATA_PATH + "tessdata");
		    dir.mkdirs();

		    if (!(new File(DATA_PATH + "tessdata/" + "eng" + ".traineddata")).exists()) {
		        try {

		            AssetManager assetManager = getAssets();
		            InputStream in = assetManager.open("eng.traineddata");
		            OutputStream out = new FileOutputStream(DATA_PATH
		                    + "tessdata/eng.traineddata");

		            byte[] buf = new byte[1024];
		            int len;
		            while ((len = in.read(buf)) > 0) {
		                out.write(buf, 0, len);
		            }
		            in.close();
		            out.close();
		        } catch (IOException e) {}
		    }

		    final TessBaseAPI baseApi = new TessBaseAPI();
		    baseApi.init(DATA_PATH, "eng");
			
		       
			//baseApi.init(myDir.toString(), "init");
			
			baseApi.setImage(bmp);
			recognizedText = baseApi.getUTF8Text();
			recognizedText = recognizedText.replaceAll("x", "");
			recognizedText = recognizedText.replaceAll(" ", "");
			
			baseApi.end();					
			
			GetWolframInfo task = new GetWolframInfo();
			task.execute();
			
			convertedString.setText(recognizedText);
	
					
		
		
		}
	}
	
	public class GetWolframInfo extends AsyncTask <String,String,String>{

		String answer;
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			pDialog = new ProgressDialog(MainActivity.this);
			pDialog.setMessage("Calculating result");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();			
		}

		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			
			String final_url = url_Wolfram + recognizedText + apiKey;
			
			
			try{
			Document dom = xmlReader.makeHttpRequest(final_url, "GET", "");
			if(dom != null){				
				Element root = dom.getDocumentElement();
				Attr sucAttr = root.getAttributeNode("success");
				NodeList pods = root.getElementsByTagName("pod");
				answer = "0";
				boolean exit = false;
				
				for (int i=0; i<pods.getLength(); i++){
					NamedNodeMap attributes = pods.item(i).getAttributes();
					Attr title = (Attr) attributes.getNamedItem("title");
					Log.d("Inside", "1");
					Log.d("Title",title.getValue());
					if (title.getValue().equals("Decimal approximation")||title.getValue().equals("Result")){
						NodeList insideResult = pods.item(i).getChildNodes();
						for (int j = 0; j<insideResult.getLength();j++){
							Log.d("Inside", "2");
							if(insideResult.item(j).getNodeName().equals("subpod")){
								NodeList insideSubpod = insideResult.item(j).getChildNodes();
								for (int k = 0; k<insideSubpod.getLength();k++){
									Log.d("Inside", "3");
									if(insideSubpod.item(k).getNodeName().equals("plaintext")){
										answer = insideSubpod.item(k).getTextContent();
										exit = true;
									}
									
									if(exit) break;
								}
								
							}
							
							if (exit) break;
						}
						
					}
					if(exit) break;
				}
				Log.d("Wolfram Response", answer);
			}
			else 
				Log.d("Wolfram Response", "Null XML");
			}
			catch (IllegalArgumentException e){
				e.printStackTrace();
				Log.d("Error Shiz", "Bitch ass error");
			}
			
			
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			pDialog.dismiss();
			mathAnswer.setText(answer);
			
		}
		
		
		
	}
	

 	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(v.getId()==R.id.button1){
			Intent i = new Intent (MediaStore.ACTION_IMAGE_CAPTURE);
			startActivityForResult(i, cameraData);
			
		}
		
	}


}
