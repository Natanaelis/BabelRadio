package com.babelsoft.babelradio;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.support.v7.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class RadiosListActivity extends AppCompatActivity implements IHttpPostAsyncResponse, IImageAsyncResponse {
    private ListView listView;
    private SearchView searchView;
    private ProgressBar progressBar;
    private ArrayList<String> listInput = new ArrayList<>();
    private ArrayList<String> tags = new ArrayList<>();
    private ArrayList<String> streams = new ArrayList<>();
    private ArrayList<Bitmap> images = new ArrayList<>();
    private ArrayList<String> imagesUrl = new ArrayList<>();
    private JSONArray radiosArray = null;
    private String response;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_view);
        progressBar = (ProgressBar)findViewById(R.id.loading);
        listView = (ListView) findViewById(R.id.list);
        searchView = (SearchView) findViewById(R.id.search);

        setupActionBar();

        listInput.clear();
        response = CountriesListActivity.databaseResponse;
        try {
            radiosArray = new JSONArray(response);
            for (int i = 0; i < radiosArray.length(); i++) {
                listInput.add(radiosArray.getJSONObject(i).getString("radio_name"));
                tags.add(radiosArray.getJSONObject(i).getString("radio_tag"));
                streams.add(radiosArray.getJSONObject(i).getString("radio_stream"));
                imagesUrl.add(radiosArray.getJSONObject(i).getString("radio_image"));
            }
            downloadImages();
        } catch (JSONException e) {
            listInput.add(response);
            tags.add(" ");
            images.add(BitmapFactory.decodeResource(this.getResources(), R.drawable.error));
            showList();
        }
    }

    private void showList() {
        final ListsAdapter adapter = new ListsAdapter(this, listInput, tags, streams, images);

        progressBar.setVisibility(View.GONE);
        listView.setAdapter(adapter);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return false;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Radio newRadio = new Radio(PlayerService.radioList.size() + 1,
                            PlayerService.radioList.size() + 1,
                            listInput.get(position),
                            tags.get(position),
                            DbBitmapUtility.getBytes(images.get(position)),
                            streams.get(position));
                PlayerService.currentRadio = newRadio;
                NavUtils.navigateUpTo(RadiosListActivity.this, new Intent(getBaseContext(), BabelRadioApp.class));

                stopPlayRadio();
                startPlayRadio();
            }
        });
    }

    @Override
    public void postResult(String asyncResult) {

    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Radios");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    private void downloadImages() {
        progressBar.setMax(imagesUrl.size());
        for (int i = 0; i < imagesUrl.size(); i++) {
            DownloadImageAsync di = new DownloadImageAsync();
            di.delegate = this;
            String imageUrl = "https:" + imagesUrl.get(i);
            di.execute(imageUrl);
        }
    }

    @Override
    public void imageResult(Bitmap asyncImage) {
        if (asyncImage != null) {
            images.add(asyncImage);
            progressBar.setProgress(images.size());
        }
        else {
            images.add(BitmapFactory.decodeResource(this.getResources(), R.drawable.nologo));
        }
        if (images.size() == imagesUrl.size()) {
            showList();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
   }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
/*        if (listInput != null) {
            listInput.clear();
            listInput = null;
        }
        if (tags != null) {
            tags.clear();
            tags = null;
        }
        if (streams != null) {
            streams.clear();
            streams = null;
        }
        if (images != null) {
            images.clear();
            images = null;
        }
        if (imagesUrl != null) {
            imagesUrl.clear();
            imagesUrl = null;
        }
        if (radiosArray != null) {
            radiosArray = null;
        }
        if (response != null) {
            response = null;
        }
*/
        finish();
        Runtime.getRuntime().gc();
    }

    private void startPlayRadio() {
        Intent playRadioIntent = new Intent();
        playRadioIntent.setAction(ControlAction.PLAY.name());
        sendBroadcast(playRadioIntent);
    }

    private void stopPlayRadio() {
        Intent stopRadioIntent = new Intent();
        stopRadioIntent.setAction(ControlAction.STOP.name());
        sendBroadcast(stopRadioIntent);
    }

}
