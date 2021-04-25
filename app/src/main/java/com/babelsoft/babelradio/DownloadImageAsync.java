package com.babelsoft.babelradio;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadImageAsync extends AsyncTask<String, Void, Bitmap> {

    public IImageAsyncResponse delegate = null;

    @Override
    protected Bitmap doInBackground(String... params) {
        try {
            URL url = new URL(params[0]);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (MalformedURLException e) {
//            e.printStackTrace();
            Log.e("Progress", "MalformedURLException");
        } catch (IOException e) {
//            e.printStackTrace();
            Log.e("Progress", "IOException");
        }
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap response) {
        delegate.imageResult(response);
    }
}
