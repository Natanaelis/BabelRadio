package com.babelsoft.babelradio;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;

public class RadiosListActivity extends AppCompatActivity implements IImageAsyncResponse {
    private ListView listView;
    private SearchView searchView;
    private TextView searchText;
    private TextView searchNumber;
    private ProgressBar progressBar;
    private Button searchNextButton;
    private Button searchPreviousButton;
    private ArrayList<String> listInput = new ArrayList<>();
    private ArrayList<Integer> ids = new ArrayList<>();
    private ArrayList<String> tags = new ArrayList<>();
    private ArrayList<String> streams = new ArrayList<>();
    private ArrayList<Bitmap> images = new ArrayList<>();
    private ArrayList<String> imagesUrl = new ArrayList<>();
    private JSONArray radiosArray = null;
    private String response;
    int numberOfRadios;
    private int numberOfPages = 1;
    private int currentPage;
    private int arrayStart, arrayEnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_view);
        progressBar = (ProgressBar)findViewById(R.id.loading);
        listView = (ListView) findViewById(R.id.list);
        searchView = (SearchView) findViewById(R.id.search);
        searchText = (TextView)findViewById(R.id.searchTitle);
        searchNumber = (TextView)findViewById(R.id.searchNumber);
        searchNextButton = (Button)findViewById(R.id.searchNextButton);
        searchPreviousButton = (Button)findViewById(R.id.searchPrevButton);

        setupActionBar();
        currentPage = getIntent().getIntExtra("Page", 1);
        response = CountriesListActivity.databaseResponse;

        try {
            radiosArray = new JSONArray(response);
            numberOfRadios = radiosArray.length();
            numberOfPages = Math.abs(numberOfRadios / 100) + 1;
            readRadiosData();
            downloadImages();
        } catch (JSONException e) {
            listInput.add(response);
            tags.add(" ");
            images.add(BitmapFactory.decodeResource(this.getResources(), R.drawable.error));
            listView.setClickable(false);
            showList();
        }
    }

    private void readRadiosData() {
        countArrayStartEnd();

        try {
            for (int i = arrayStart; i <= arrayEnd; i++) {
                listInput.add(radiosArray.getJSONObject(i).getString("radio_name"));
                ids.add(Integer.parseInt(radiosArray.getJSONObject(i).getString("radio_id")));
                tags.add(radiosArray.getJSONObject(i).getString("radio_tag"));
                streams.add(radiosArray.getJSONObject(i).getString("radio_stream"));
                imagesUrl.add(radiosArray.getJSONObject(i).getString("radio_image"));
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showList() {
        final ListsAdapter adapter = new ListsAdapter(this, listInput, tags, streams, ids, images);

        listView.setAdapter(adapter);
        progressBar.setVisibility(View.INVISIBLE);
        if (numberOfPages > 1)
            searchNextButton.setVisibility(View.VISIBLE);
        if (currentPage == numberOfPages)
            searchNextButton.setVisibility(View.INVISIBLE);
        if (currentPage > 1)
            searchPreviousButton.setVisibility(View.VISIBLE);
        searchView.setVisibility(View.VISIBLE);
        searchText.setVisibility(View.VISIBLE);
        searchNumber.setText(String.valueOf((arrayStart + 1) + "-" + (arrayEnd + 1) + " \\ " + numberOfRadios));
        searchNumber.setVisibility(View.VISIBLE);
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
                if (listView.isClickable()) {
                    stopPlayRadio();
                    Radio newRadio = new Radio(PlayerService.radioList.size() + 1,
                            ids.get(position),
                            listInput.get(position),
                            tags.get(position),
                            DbBitmapUtility.getBytes(images.get(position)),
                            streams.get(position));
                    PlayerService.currentRadio = newRadio;
                    NavUtils.navigateUpTo(RadiosListActivity.this, new Intent(getBaseContext(), BabelRadioApp.class));
                    startPlayRadio();
                }
            }
        });

        searchNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(RadiosListActivity.this, RadiosListActivity.class);
                myIntent.putExtra("Page", currentPage + 1);
                startActivity(myIntent);
            }
        });

        searchPreviousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void countArrayStartEnd() {
        arrayStart = (currentPage - 1) * 100;
        arrayEnd = (numberOfRadios - arrayStart > 100) ? arrayEnd = (currentPage * 100) - 1 : numberOfRadios - 1;
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Radios");
            actionBar.setSubtitle(CountriesListActivity.subtitle);
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
        }
        else {
            images.add(BitmapFactory.decodeResource(this.getResources(), R.drawable.nologo));
        }
        progressBar.setProgress(images.size());
        if (images.size() == imagesUrl.size()) {
            listView.setClickable(true);
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
