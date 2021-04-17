package com.babelsoft.babelradio;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class HttpPostAsync extends AsyncTask<String, Void, String> {
    Map<String, String> postData = new HashMap<>();

    private static final String TAG = "HttpPostAsyncTask";
    public HttpPostAsync(Map<String, String> postData) {
        if (postData != null) {
            this.postData = postData;
        }
    }

    public IHttpPostAsyncResponse delegate = null;

    // This is a function that we are overriding from AsyncTask. It takes Strings as parameters because that is what we defined for the parameters of our async task
    @Override
    protected String doInBackground(String... params) {
        String response = null;

        try {
            // This is getting the url from the string we passed in
            URL url = new URL(params[0]);
            // Create the urlConnection
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();


            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);

            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            urlConnection.setRequestMethod("POST");

            // OPTIONAL - Sets an authorization header
            urlConnection.setRequestProperty("Authorization", "someAuthString");

            // Send the post body
            if (postData != null) {
                OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                String postDataEncoded = URLEncoder.encode(postData.keySet().toArray()[0].toString(), "UTF-8")+"="
                        + URLEncoder.encode(postData.values().toArray()[0].toString(), "UTF-8");
                writer.write(postDataEncoded);
                writer.flush();
            }

            int statusCode = urlConnection.getResponseCode();

            if (statusCode ==  200) {

                InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());

                response = convertInputStreamToString(inputStream);
                inputStream.close();
            } else {
                Log.d(TAG, "Status code not 200");
            }

        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
        }
        return response;
    }

    @Override
    protected void onPostExecute(String response) {
        if(delegate != null)
        {
            if(response == null) {
                response = "Unable to resolve host. Check internet connection";
            }
            delegate.postResult(response);
        }
        else
        {
            Log.e("HttpPost", "You have not assigned IHttpPostAsyncResponse delegate");
        }
    }

    private String convertInputStreamToString(InputStream inputStream) {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

}