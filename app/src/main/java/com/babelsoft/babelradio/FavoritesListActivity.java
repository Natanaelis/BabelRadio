package com.babelsoft.babelradio;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class FavoritesListActivity extends AppCompatActivity {
    private ListView listView;
    private SearchView searchView;
    private TextView searchText;
    private TextView searchNumber;
    private ProgressBar progressBar;
    public static String subtitle;
    private ArrayList<String> listInput = new ArrayList<>();
    private ArrayList<Integer> ids = new ArrayList<>();
    private ArrayList<String> tags = new ArrayList<>();
    private ArrayList<String> streams = new ArrayList<>();
    private ArrayList<Bitmap> images = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_view);
        listView = (ListView)findViewById(R.id.list);
        searchView = (SearchView)findViewById(R.id.search);
        searchText = (TextView)findViewById(R.id.searchTitle);
        searchNumber = (TextView)findViewById(R.id.searchNumber);
        progressBar = (ProgressBar)findViewById(R.id.loading);

        setupActionBar();

        InternalDatabaseHandler idh = new InternalDatabaseHandler(getApplicationContext());

        List<Radio> inputRadios = idh.getAllRadios();

        for (int i = 0; i < inputRadios.size(); i++) {
            listInput.add(inputRadios.get(i).getRadioName());
            ids.add(inputRadios.get(i).getRadioId());
            tags.add(inputRadios.get(i).getRadioTag());
            streams.add(inputRadios.get(i).getRadioStream());
            images.add(DbBitmapUtility.getBitmap(inputRadios.get(i).getRadioImage()));

        }
        showList();
    }

    private void showList() {
        final ListsAdapter adapter = new ListsAdapter(this, listInput, tags, streams, ids, images);

        progressBar.setVisibility(View.INVISIBLE);
        listView.setAdapter(adapter);
        searchText.setVisibility(View.VISIBLE);
        searchView.setVisibility(View.VISIBLE);
        searchNumber.setText(String.valueOf(listInput.size()));
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
                stopPlayRadio();
                Radio newRadio = new Radio(PlayerService.radioList.size() + 1,
                        ids.get(position),
                        listInput.get(position),
                        tags.get(position),
                        DbBitmapUtility.getBytes(images.get(position)),
                        streams.get(position));
                PlayerService.currentRadio = newRadio;
                NavUtils.navigateUpTo(FavoritesListActivity.this, new Intent(getBaseContext(), BabelRadioApp.class));
                startPlayRadio();
            }
        });
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Favorites");
            actionBar.setSubtitle("");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
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
        sendBroadcast(new Intent(ControlAction.PLAY.name()));
    }

    private void stopPlayRadio() {
        sendBroadcast(new Intent(ControlAction.STOP.name()));
    }

}
