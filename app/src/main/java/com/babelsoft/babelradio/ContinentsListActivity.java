package com.babelsoft.babelradio;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ContinentsListActivity extends AppCompatActivity implements IHttpPostAsyncResponse {
    private String inputUrl = "https://babelradio.000webhostapp.com/countries.php";
    private ListView list;
    private SearchView searchView;
    private TextView searchText;
    private TextView searchNumber;
    private ProgressBar progressBar;
    public static String databaseResponse;
    public static String subtitle;
    private ArrayList<String> listInput = new ArrayList<>();
    private ArrayList<Bitmap> images = new ArrayList<Bitmap>();
    private JSONArray continentsArray = null;
    private String response;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_view);
        progressBar = (ProgressBar)findViewById(R.id.loading);
        list = (ListView)findViewById(R.id.list);
        searchView = (SearchView)findViewById(R.id.search);
        searchText = (TextView)findViewById(R.id.searchTitle);
        searchNumber = (TextView)findViewById(R.id.searchNumber);

        setupActionBar();

        listInput.clear();
        response = BabelRadioApp.databaseResponse;
        try {
            continentsArray = new JSONArray(response);
            for (int i = 0; i < continentsArray.length(); i++) {
                listInput.add(continentsArray.getJSONObject(i).getString("continent_name"));
            }
            createImagesList();
            showList();
        } catch (JSONException e) {
            listInput.add(response);
            images.add(BitmapFactory.decodeResource(this.getResources(), R.drawable.error));
            showList();
        }
    }

    private void getInput(String input) {
        Map<String, String> postData = new HashMap<>();
        postData.put("continent", input);
        HttpPostAsync httpPost = new HttpPostAsync(postData);
        httpPost.delegate = this;
        httpPost.execute(inputUrl);
    }

    private void showList() {
        final ListsAdapter adapter = new ListsAdapter(this, listInput, images);

        progressBar.setVisibility(View.GONE);
        list.setAdapter(adapter);
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

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                list.setEnabled(false);
                subtitle = listInput.get(position);
                getInput(listInput.get(position));
            }
        });
    }

    private void createImagesList() {
        for (int i = 0; i < 6; i++) {
            images.add(BitmapFactory.decodeResource(this.getResources(), R.drawable.worldwide));
        }
    }

    @Override
    public void postResult(String asyncResult) {
        Intent myIntent = new Intent(ContinentsListActivity.this, CountriesListActivity.class);
        databaseResponse = asyncResult;
        startActivity(myIntent);
        list.setEnabled(true);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Continents");
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
/*        if (listInput != null) {
            listInput.clear();
            listInput = null;
        }
        if (images != null) {
            images.clear();
            images = null;
        }
        if(continentsArray != null) {
            continentsArray = null;
        }
        if(response != null) {
            response = null;
        }
        if (databaseResponse != null) {
            databaseResponse = null;
        }
*/
        finish();
        Runtime.getRuntime().gc();
    }
}
