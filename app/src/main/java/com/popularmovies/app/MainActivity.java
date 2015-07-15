package com.popularmovies.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.popularmovies.app.sync.PopularMoviesSyncAdapter;


public class MainActivity extends ActionBarActivity implements MainActivityFragment.Callback {

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String MOVIE_DETAIL_FRAGMENT_TAG = "MDFTAG";
    private static final String MOVIE_LIST_FRAGMENT_TAG = "MLFTAG";
    private String mSortOrder;
    private boolean mTwoPane;
    private MainActivityFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSortOrder = Utility.getPreferredSortOrder(this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        if (findViewById(R.id.container_movies) != null) {
            mTwoPane = true;

            mFragment = new MainActivityFragment();
            MovieDetailsActivityFragment fragment = new MovieDetailsActivityFragment();
            fragment.setTwoPane(mTwoPane);
            if (savedInstanceState == null) {

                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_main_placeholder, mFragment, MOVIE_LIST_FRAGMENT_TAG)
                        .commit();

                getSupportFragmentManager().beginTransaction()
                        .add(R.id.container_movies, fragment, MOVIE_DETAIL_FRAGMENT_TAG)
                        .commit();
            } else {
                mFragment = (MainActivityFragment) getSupportFragmentManager()
                        .findFragmentByTag(MOVIE_LIST_FRAGMENT_TAG);
            }
        } else {
            mTwoPane = false;
            getSupportActionBar().setElevation(0f);
        }
        if (mFragment != null) {
            mFragment.setTwoPane(mTwoPane);
        }
        PopularMoviesSyncAdapter.initializeSyncAdapter(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onItemSelected(String data[]){

        if (mTwoPane) {

            MovieDetailsActivityFragment fragment = new MovieDetailsActivityFragment();
            fragment.setTwoPane(mTwoPane);
            Bundle args = new Bundle();
            args.putStringArray(Intent.EXTRA_TEXT, data);
            fragment.setArguments(args);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.replace(R.id.container_movies, fragment, MOVIE_DETAIL_FRAGMENT_TAG);
            transaction.addToBackStack(null);

            // Commit the transaction
            transaction.commit();

        } else {
            Intent intent = new Intent(this, MovieDetailsActivity.class);
            intent.putExtra(Intent.EXTRA_TEXT, data);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String sortOrder = Utility.getPreferredSortOrder( this );

        if(sortOrder != null && !sortOrder.equals(mSortOrder)) {
            if ( null != mFragment ) {
                mFragment.onSortOrderChanged();
            } else {
                mFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
                mFragment.setTwoPane(mTwoPane);
                mFragment.onSortOrderChanged();
            }
            mSortOrder = sortOrder;
        }
    }

}
