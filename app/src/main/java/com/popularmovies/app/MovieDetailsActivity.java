package com.popularmovies.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


public class MovieDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);
        if (savedInstanceState == null) {

            Bundle arguments = new Bundle();
            String data[] = getIntent().getStringArrayExtra(Intent.EXTRA_TEXT);
            arguments.putStringArray(Intent.EXTRA_TEXT, data);

            MovieDetailsActivityFragment fragment = new MovieDetailsActivityFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container_movies, fragment)
                    .commit();
        }
    }

}
