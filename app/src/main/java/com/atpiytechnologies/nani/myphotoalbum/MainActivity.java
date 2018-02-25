package com.atpiytechnologies.nani.myphotoalbum;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static android.content.ContentValues.TAG;
import static com.atpiytechnologies.nani.myphotoalbum.Config.nani;

public class MainActivity extends Activity {

    boolean flag = false;
    String[] data;
    int allasynctasks = 0;
    int no_of_images = 0;
    int k = 0;
    GridView gridview;
    Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Config.isNetworkStatusAvailable(getApplicationContext())) {
            new getPics().execute();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("No Internet Connection").setTitle("Information");
            builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });

            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
    }

    public class getPics extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new Dialog(MainActivity.this);
            dialog.requestWindowFeature(getWindow().FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.loader);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();

            String url = "";
            String jsonStr = sh.makeServiceCall(url);

            //Log.e(TAG, "Response from url: " + jsonStr);
            //Log.e(TAG, "URL : " + url);
            Log.e(TAG, "Got Response from url!");

            if (!jsonStr.equals("Nothing")) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);
                    JSONObject jsonObj1 = jsonObj.getJSONObject("photos");
                    JSONArray photo = jsonObj1.getJSONArray("photo");
                    int length = photo.length();
                    data = new String[length];

                    // looping through the response
                    for (int i = 0; i < length; i++) {
                        JSONObject c = photo.getJSONObject(i);
                        data[i] = "https://farm"+c.getInt("farm");
                        data[i] += ".staticflickr.com/" + c.getString("server");
                        data[i] += "/" + c.getString("id")+ "_" + c.getString("secret")+ ".jpg";
                        Log.e(TAG, "data["+i+"] : "+data[i]);
                    }
                    no_of_images = data.length;
                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    flag = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Json parsing error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } else {
                Log.v(TAG, "Couldn't get json from server.");
                flag = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Couldn't get json from server.", Toast.LENGTH_LONG).show();
                    }
                });
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            dialog.dismiss();

            if (flag == false) {
                if(no_of_images > 0) {
                    nani = new Drawable[no_of_images];
                    for(int i = 0; i < no_of_images; i++)
                        new LoadImages(data[i], "image"+i+".jpg").execute();
                }
            }
        }
    }

    private class LoadImages extends AsyncTask<String, Void, Drawable> {

        private String imageUrl , imageName;

        public LoadImages(String url, String file_name) {
            this.imageUrl = url;
            this.imageName = file_name;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Drawable doInBackground(String... urls) {

            try {
                InputStream is = (InputStream) this.fetch(this.imageUrl);
                Drawable d = Drawable.createFromStream(is, this.imageName);
                return d;
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        private Object fetch(String address) throws MalformedURLException,IOException {
            URL url = new URL(address);
            Object content = url.getContent();
            return content;
        }

        @Override
        protected void onPostExecute(Drawable result) {
            super.onPostExecute(result);
            nani[k] = result;
            k++;
            allasynctasks++;
            Log.e(TAG, "allasynctasks : " + allasynctasks);
            if(allasynctasks == no_of_images) {
                gotonextActivity();
            }
        }
    }

    public void gotonextActivity() {
        gridview = (GridView) findViewById(R.id.gv);
        gridview.setAdapter(new ImageAdapter(this));

        dialog.dismiss();

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,int position, long id) {
                //Toast.makeText(MainActivity.this, "" + position,Toast.LENGTH_SHORT).show();

                Intent i = new Intent(getApplicationContext(), detailedView.class);
                // Pass image index
                i.putExtra("id", position);
                startActivity(i);
            }
        });
    }
}
