package com.architectica.socialcomponents.main.News;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.MenuItem;
import android.widget.Toast;

import com.architectica.socialcomponents.R;
import com.architectica.socialcomponents.adapters.NewsAdapter;
import com.architectica.socialcomponents.api.ApiClient;
import com.architectica.socialcomponents.api.ApiInterface;
import com.architectica.socialcomponents.model.Article;
import com.architectica.socialcomponents.model.News;
import com.architectica.socialcomponents.utils.NewsUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GoogleNewsActivity extends AppCompatActivity {

    private static final String API_KEY = "a7e67aca85f340ed93494bdb0151cfa5";
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private List<Article> articles = new ArrayList<>();
    private NewsAdapter newsAdapter;
    private String TAG = GoogleNewsActivity.class.getSimpleName();

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_news);

        getSupportActionBar().setTitle("Google News India");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recycler_view);
        layoutManager = new LinearLayoutManager(GoogleNewsActivity.this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setNestedScrollingEnabled(false);

        LoadJson();

    }

    public void LoadJson(){

        articles.clear();

        ApiInterface apiInterface = ApiClient.getApiClient().create(ApiInterface.class);

        String country = NewsUtils.getCountry();

        Call<News> call;
        call = apiInterface.getNews(country,API_KEY);

        call.enqueue(new Callback<News>() {
            @Override
            public void onResponse(Call<News> call, Response<News> response) {

                if (response.isSuccessful() && response.body().getArticle() != null){

                    articles = response.body().getArticle();

                    newsAdapter = new NewsAdapter(articles,GoogleNewsActivity.this);
                    recyclerView.setAdapter(newsAdapter);
                    newsAdapter.notifyDataSetChanged();

                }
                else {

                    Toast.makeText(GoogleNewsActivity.this, "No Result", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<News> call, Throwable t) {

            }
        });

    }
}
