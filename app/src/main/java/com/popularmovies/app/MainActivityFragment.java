package com.popularmovies.app;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import com.popularmovies.app.adapter.GridViewMoviesAdapter;
import com.popularmovies.app.data.PopularMoviesContract;
import com.popularmovies.app.data.PopularMoviesContract.MovieEntry;
import com.popularmovies.app.sync.PopularMoviesSyncAdapter;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private GridViewMoviesAdapter mGridViewMoviesAdapter;
    private static final int FORECAST_LOADER = 0;
    private boolean mTwoPane;

    public static final String[] MOVIE_COLUMNS = {
            MovieEntry.TABLE_NAME + "." + MovieEntry._ID,
            MovieEntry.COLUMN_MOVIE_ID,
            MovieEntry.COLUMN_IS_ADULT,
            MovieEntry.COLUMN_BACK_DROP_PATH,
            MovieEntry.COLUMN_ORIGINAL_LANGUAGE,
            MovieEntry.COLUMN_ORIGINAL_TITLE,
            MovieEntry.COLUMN_OVERVIEW,
            MovieEntry.COLUMN_RELEASE_DATE,
            MovieEntry.COLUMN_POSTER_PATH,
            MovieEntry.COLUMN_POPULARITY,
            MovieEntry.COLUMN_TITLE,
            MovieEntry.COLUMN_IS_VIDEO,
            MovieEntry.COLUMN_VOTE_AVERAGE,
            MovieEntry.COLUMN_VOTE_COUNT,
            MovieEntry.COLUMN_RUNTIME,
            MovieEntry.COLUMN_STATUS,
            MovieEntry.COLUMN_DATE
    };

    public static final int COL_MOVIE_PK_ID = 0;
    public static final int COL_MOVIE_ID = 1;
    public static final int COL_IS_ADULT   = 2;
    public static final int COL_BACK_DROP_PATH = 3;
    public static final int COL_ORIGINAL_LANGUAGE = 4;
    public static final int COL_ORIGINAL_TITLE = 5;
    public static final int COL_OVERVIEW = 6;
    public static final int COL_RELEASE_DATE = 7;
    public static final int COL_POSTER_PATH = 8;
    public static final int COL_POPULARITY = 9;
    public static final int COL_TITLE = 10;
    public static final int COL_IS_VIDEO = 11;
    public static final int COL_VOTE_AVERAGE = 12;
    public static final int COL_VOTE_COUNT = 13;
    public static final int COL_RUNTIME = 14;
    public static final int COL_STATUS = 15;
    public static final int COL_DATE = 16;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mGridViewMoviesAdapter = new GridViewMoviesAdapter(getActivity(), null, 0);

        // initialize the GridView
        GridView gridView = (GridView) rootView.findViewById(R.id.gridView);
        TextView emptyTextView = (TextView) rootView.findViewById(R.id.gridView_empty);
        emptyTextView.setText(getString(R.string.label_text_view_no_movies_as_favorite));
        gridView.setEmptyView(emptyTextView);
        gridView.setAdapter(mGridViewMoviesAdapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position,
                                    long id) {
                final Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);

                String backDropPath = cursor.getString(COL_BACK_DROP_PATH);
                String posterPath = cursor.getString(COL_POSTER_PATH);
                if (backDropPath == null || backDropPath.equals("null")) {
                    if (posterPath != null && !posterPath.equals("null")) {
                        backDropPath = posterPath;
                    }
                }

                if (posterPath == null || posterPath.equals("null")) {
                    if (backDropPath != null && !backDropPath.equals("null")) {
                        posterPath = backDropPath;
                    }
                }


                String data[] = {backDropPath, posterPath, cursor.getString(COL_RELEASE_DATE).toString(),
                        Double.toString(cursor.getDouble(COL_RUNTIME)), Double.toString(cursor.getDouble(COL_VOTE_AVERAGE)), cursor.getString(COL_OVERVIEW),
                        cursor.getString(COL_ORIGINAL_TITLE), Integer.toString(cursor.getInt(COL_MOVIE_ID)), Boolean.toString(mTwoPane)};
                ((Callback) getActivity()).onItemSelected(data);
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    void onSortOrderChanged( ) {
        PopularMoviesSyncAdapter.syncImmediately(getActivity());
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String sortOrderSelected = prefs.getString(getActivity().getString(R.string.pref_sort_order_key), null);

        String sortOrder = PopularMoviesContract.MovieEntry.COLUMN_POPULARITY + " DESC";

        if(sortOrderSelected != null && sortOrderSelected.equals(getActivity().getString(R.string.pref_sort_order_vote_average))) {
            sortOrder = MovieEntry.COLUMN_VOTE_AVERAGE + " DESC";
        }

        Uri weatherForLocationUri = PopularMoviesContract.MovieEntry.buildMovieUri();

        return new CursorLoader(getActivity(),
                weatherForLocationUri,
                MOVIE_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mGridViewMoviesAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mGridViewMoviesAdapter.swapCursor(null);
    }

    public interface Callback {
        /**
         * MainActivityFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(String data[]);
    }

    public void setTwoPane(boolean mTwoPane) {
        this.mTwoPane = mTwoPane;
    }

}
