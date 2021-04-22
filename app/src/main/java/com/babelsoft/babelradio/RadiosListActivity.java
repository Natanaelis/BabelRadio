package com.babelsoft.babelradio;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.support.v7.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;

public class RadiosListActivity extends AppCompatActivity implements IHttpPostAsyncResponse {
    ListView listView;
    SearchView searchView;
    ArrayList<String> listInput = new ArrayList<>();
    ArrayList<String> tags = new ArrayList<>();
    ArrayList<String> streams = new ArrayList<>();
    JSONArray radiosArray;

    //    Integer[] imgid = {R.drawable.worldwide, R.drawable.worldwide, R.drawable.worldwide, R.drawable.worldwide,
//            R.drawable.worldwide};
    Integer imgid = R.drawable.worldwide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        radiosArray = null;
        listInput.clear();

        String response = CountriesListActivity.databaseResponse;
        try {
            radiosArray = new JSONArray(response);
            for (int i = 0; i < radiosArray.length(); i++) {
                listInput.add(radiosArray.getJSONObject(i).getString("radio_name"));
                tags.add(radiosArray.getJSONObject(i).getString("radio_tag"));
                streams.add(radiosArray.getJSONObject(i).getString("radio_stream"));
            }
            initiateView();
        } catch (JSONException e) {
            listInput.add(response);
            tags.add(" ");
            imgid = R.drawable.error;
            initiateView();
        }
    }

    private void initiateView() {
        setContentView(R.layout.list_view);
        setupActionBar();

        final ListsAdapter adapter = new ListsAdapter(this, listInput, tags, streams, imgid);
        listView = (ListView) findViewById(R.id.list);
        searchView = (SearchView) findViewById(R.id.search);

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
                            R.drawable.logo_rmf,
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
        if(listInput != null) {
            listInput.clear();
        }
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
