package com.babelsoft.babelradio;

import android.content.Intent;
import android.os.Bundle;
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
import java.util.HashMap;
import java.util.Map;

public class ContinentsListActivity extends AppCompatActivity implements IHttpPostAsyncResponse {
    String inputUrl = "https://babelradio.000webhostapp.com/countries.php";
    ListView list;
    SearchView searchView;
    ArrayList<String> listInput = new ArrayList<>();
    static String databaseResponse;

    //    Integer[] imgid = {R.drawable.worldwide, R.drawable.worldwide, R.drawable.worldwide, R.drawable.worldwide,
//            R.drawable.worldwide};
    Integer imgid = R.drawable.worldwide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        JSONArray continentsArray = null;
        listInput.clear();
        String response = BabelRadioApp.databaseResponse;
        try {
            continentsArray = new JSONArray(response);
            for (int i = 0; i < continentsArray.length(); i++) {
                listInput.add(continentsArray.getJSONObject(i).getString("continent_name"));
            }
            initiateView();
        } catch (JSONException e) {
            listInput.add(response);
            imgid = R.drawable.error;
            initiateView();
        }
    }

    private void getInput(String input) {
        Map<String, String> postData = new HashMap<>();
        postData.put("continent", input);
        HttpPostAsync httpPost = new HttpPostAsync(postData);
        httpPost.delegate = this;
        httpPost.execute(inputUrl);
    }

    private void initiateView() {
        setContentView(R.layout.list_view);
        setupActionBar();

        final ListsAdapter adapter = new ListsAdapter(this, listInput, imgid);
        list = (ListView)findViewById(R.id.list);
        searchView = (SearchView)findViewById(R.id.search);

        list.setAdapter(adapter);

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
                getInput(listInput.get(position));
            }
        });
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
}
