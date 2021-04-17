package com.babelsoft.babelradio;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.support.v7.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ContinentsActivity extends AppCompatActivity implements IHttpPostAsyncResponse {
    String inputUrl = "https://babelradio.000webhostapp.com/countries.php";
    ListView list;
    SearchView searchView;
//    ArrayList<String> continents = new ArrayList<>();
    static ArrayList<String> listInput = new ArrayList<>();
    static String databaseResponse;

//    Integer[] imgid = {R.drawable.worldwide, R.drawable.worldwide, R.drawable.worldwide, R.drawable.worldwide,
//            R.drawable.worldwide};
    Integer imgid = R.drawable.worldwide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_view);

        listInput.add("Africa");
        listInput.add("Asia");
        listInput.add("North America");
        listInput.add("South America");
        listInput.add("Europe");
        listInput.add("Oceania");

        final ListsAdapter adapter = new ListsAdapter(this, listInput, imgid);
        list=(ListView)findViewById(R.id.list);
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

        getWindow().getDecorView().setBackgroundColor(Color.DKGRAY);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                getInput(listInput.get(position));
            }
        });
    }

    private void getInput(String input) {
        Map<String, String> postData = new HashMap<>();
        postData.put("continent", input);
        HttpPostAsync httpPost = new HttpPostAsync(postData);
        httpPost.delegate = this;
        httpPost.execute(inputUrl);
    }

    @Override
    public void postResult(String asyncResult) {
        Intent myIntent = new Intent(ContinentsActivity.this, CountriesActivity.class);
        databaseResponse = asyncResult;
        ContinentsActivity.this.startActivity(myIntent);
    }
}
