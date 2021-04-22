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

public class CountriesListActivity extends AppCompatActivity implements IHttpPostAsyncResponse {
    String inputUrl = "https://babelradio.000webhostapp.com/radios.php";
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
        JSONArray countriesArray = null;
        listInput.clear();

        String response = ContinentsListActivity.databaseResponse;
        try {
            countriesArray = new JSONArray(response);
            for (int i = 0; i < countriesArray.length(); i++) {
                listInput.add(countriesArray.getJSONObject(i).getString("country_name"));
            }
            initiateView();
        } catch (JSONException e) {
            listInput.add(response);
            imgid = R.drawable.error;
            initiateView();
        }
    }

    private void initiateView() {
        setContentView(R.layout.list_view);
        setupActionBar();

        final ListsAdapter adapter = new ListsAdapter(this, listInput, imgid);
        list = (ListView) findViewById(R.id.list);
        searchView = (SearchView) findViewById(R.id.search);

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

    private void getInput(String input) {
        Map<String, String> postData = new HashMap<>();
        postData.put("country", input);
        HttpPostAsync httpPost = new HttpPostAsync(postData);
        httpPost.delegate = this;
        httpPost.execute(inputUrl);
    }

    @Override
    public void postResult(String asyncResult) {
        Intent myIntent = new Intent(CountriesListActivity.this, RadiosListActivity.class);
        databaseResponse = asyncResult;
        startActivity(myIntent);
        list.setEnabled(true);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Countries");
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
