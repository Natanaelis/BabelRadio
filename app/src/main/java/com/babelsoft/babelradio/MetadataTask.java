package com.babelsoft.babelradio;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.URL;

public class MetadataTask extends AsyncTask<URL, Void, String>
{

    public IMetadataAsyncResponse delegate = null;


    @Override
    protected String doInBackground(URL... urls)
    {
        IcyStreamMeta streamMeta = new IcyStreamMeta();
        String result = "";
        try
        {
            streamMeta.setStreamUrl(urls[0]);
            streamMeta.refreshMeta();
            if (!streamMeta.isError())
                result = streamMeta.getStreamTitle();
            else
                result = "";
        }
        catch (IOException e)
        {
            e.getMessage();
        }
        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        delegate.metadataResult(result);
    }
}
