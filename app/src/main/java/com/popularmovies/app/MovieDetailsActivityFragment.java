package com.popularmovies.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.popularmovies.app.adapter.CustomReviewsListAdapter;
import com.popularmovies.app.adapter.CustomTrailersListAdapter;
import com.popularmovies.app.data.PopularMoviesContract;
import com.squareup.picasso.Picasso;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * A placeholder fragment containing a simple view.
 */
public class MovieDetailsActivityFragment extends Fragment  implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private final String LOG_TAG = MovieDetailsActivityFragment.class.getSimpleName();

    private ImageView mBackgroundImageView   = null;
    private TextView mMovieNameView     = null;
    private ImageView mMovieImageView   = null;
    private TextView mMovieYearView     = null;
    private RatingBar mMovieRatingsView  = null;
    private TextView mMovieOverviewView = null;
    private ListView mMovieTrailersListView = null;
    private ListView mMovieReviewsListView = null;
    private TextView mTrailersEmptyView = null;
    private TextView mReviewsEmptyView     = null;
    private Button mFavoriteButton = null;
    private ImageView mCalendarImageView = null;
    private View mDetailsSeperator = null;
    private TextView mTrailersHeaderTextView = null;
    private TextView mReviewsHeaderTextView = null;

    private Context mContext = null;
    private SharedPreferences mPrefs = null;
    public static final String FAVORITE_MOVIE_IDS_SET_KEY = "movie_id_set_key";
    private CustomTrailersListAdapter mTrailersListAdapter = null;
    private CustomReviewsListAdapter mReviewsListAdapter = null;
    private int movieId = 0;
    private static final int MOVIE_TRAILER_LOADER = 0;
    private static final int MOVIE_REVIEW_LOADER = 1;
    private boolean mTwoPane;
    private Toast mFavoriteToast;
    private String mMovieName;
    private boolean mIsPreferenceChanged = false;


    public static final String[] MOVIE_TRAILER_COLUMNS = {
            PopularMoviesContract.MovieTrailerEntry.TABLE_NAME + "." + PopularMoviesContract.MovieTrailerEntry._ID,
            PopularMoviesContract.MovieTrailerEntry.TABLE_NAME + "." + PopularMoviesContract.MovieTrailerEntry.COLUMN_MOVIE_ID,
            PopularMoviesContract.MovieTrailerEntry.COLUMN_TRAILER_ID,
            PopularMoviesContract.MovieTrailerEntry.COLUMN_ISO_369_1,
            PopularMoviesContract.MovieTrailerEntry.COLUMN_KEY,
            PopularMoviesContract.MovieTrailerEntry.COLUMN_NAME,
            PopularMoviesContract.MovieTrailerEntry.COLUMN_SITE,
            PopularMoviesContract.MovieTrailerEntry.COLUMN_SIZE,
            PopularMoviesContract.MovieTrailerEntry.COLUMN_TYPE,
            PopularMoviesContract.MovieTrailerEntry.TABLE_NAME + "." + PopularMoviesContract.MovieTrailerEntry.COLUMN_DATE
    };

    public static final int COL_MOVIE_PK_ID = 0;
    public static final int COL_MOVIE_ID = 1;
    public static final int COL_TRAILER_ID = 2;
    public static final int COL_ISO_369_1 = 3;
    public static final int COL_KEY = 4;
    public static final int COL_NAME = 5;
    public static final int COL_SITE = 6;
    public static final int COL_SIZE = 7;
    public static final int COL_TYPE = 8;
    public static final int COL_DATE = 9;

    public static final String[] MOVIE_REVIEWS_COLUMNS = {
            PopularMoviesContract.MovieReviewEntry.TABLE_NAME + "." + PopularMoviesContract.MovieReviewEntry._ID,
            PopularMoviesContract.MovieReviewEntry.TABLE_NAME + "." + PopularMoviesContract.MovieReviewEntry.COLUMN_MOVIE_ID,
            PopularMoviesContract.MovieReviewEntry.COLUMN_REVIEW_ID,
            PopularMoviesContract.MovieReviewEntry.COLUMN_AUTHOR,
            PopularMoviesContract.MovieReviewEntry.COLUMN_CONTENT,
            PopularMoviesContract.MovieReviewEntry.COLUMN_URL,
            PopularMoviesContract.MovieReviewEntry.TABLE_NAME + "." + PopularMoviesContract.MovieReviewEntry.COLUMN_DATE
    };

    public static final int COL_REVIEW_MOVIE_PK_ID = 0;
    public static final int COL_REVIEW_MOVIE_ID = 1;
    public static final int COL_REVIEW_ID = 2;
    public static final int COL_AUTHOR = 3;
    public static final int COL_CONTENT = 4;
    public static final int COL_URL = 5;
    public static final int COL_REVIEW_DATE = 6;

    public MovieDetailsActivityFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_movie_details, container, false);
        Bundle arguments = getArguments();
        String data[] = null;
        if (arguments != null) {
            data = arguments.getStringArray(Intent.EXTRA_TEXT);
        }

        mContext = container.getContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        initializeUI(rootView);

        if (data != null) {

            mTwoPane = Boolean.valueOf(data[8]);

            if (mTwoPane) {
                mMovieNameView = (TextView) rootView.findViewById(R.id.movie_name);
                loadDataAndFinalizeUIComponents(data);
                mMovieNameView.setText(mMovieName);

            } else {
                /*
                int orientation = getResources().getConfiguration().orientation;
        int sw = getResources().getConfiguration().smallestScreenWidthDp;
        if (sw == 600 && orientation == Configuration.ORIENTATION_LANDSCAPE) {

        }
                 */
                Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar_detail);
                ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

                loadDataAndFinalizeUIComponents(data);

                CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) rootView.findViewById(R.id.collapsing_toolbar);
                collapsingToolbarLayout.setTitle(mMovieName);
                collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(R.color.white));

            }
        } else {
            clearViews();
        }

        return rootView;
    }

    private void clearViews() {
        mMovieRatingsView.setVisibility(View.INVISIBLE);
        mCalendarImageView.setVisibility(View.INVISIBLE);
        mFavoriteButton.setVisibility(View.INVISIBLE);
        mDetailsSeperator.setVisibility(View.INVISIBLE);
        mTrailersHeaderTextView.setVisibility(View.INVISIBLE);
        mReviewsHeaderTextView.setVisibility(View.INVISIBLE);
    }

    private void initializeUI (View rootView) {
        mTrailersListAdapter   = new CustomTrailersListAdapter(getActivity(), null, 0);
        mReviewsListAdapter    = new CustomReviewsListAdapter(getActivity(), null, 0);

        mBackgroundImageView   = (ImageView) rootView.findViewById(R.id.background_imageView);
        mMovieImageView        = (ImageView) rootView.findViewById(R.id.movie_image);
        mMovieYearView         = (TextView) rootView.findViewById(R.id.detail_year);
        mMovieRatingsView      = (RatingBar) rootView.findViewById(R.id.detail_ratings);
        mMovieTrailersListView = (ListView) rootView.findViewById(R.id.listview_trailers);
        mMovieReviewsListView  = (ListView) rootView.findViewById(R.id.listview_reviews);
        mFavoriteButton        = (Button) rootView.findViewById(R.id.button_favorite);
        mMovieOverviewView     = (TextView) rootView.findViewById(R.id.detail_overview);

        // empty views for ListViews
        mTrailersEmptyView     = (TextView) rootView.findViewById(R.id.listview_trailers_empty);
        mReviewsEmptyView      = (TextView) rootView.findViewById(R.id.listview_reviews_empty);

        mCalendarImageView     = (ImageView) rootView.findViewById(R.id.imageview_calendar);
        mDetailsSeperator      = rootView.findViewById(R.id.detail_seperator);
        mTrailersHeaderTextView= (TextView) rootView.findViewById(R.id.detail_label_trailers);
        mReviewsHeaderTextView = (TextView) rootView.findViewById(R.id.detail_label_reviews);
    }

    private void hideUIViews() {
        mBackgroundImageView.setVisibility(View.INVISIBLE);
        mMovieImageView.setVisibility(View.INVISIBLE);
        mMovieYearView.setVisibility(View.INVISIBLE);
        mMovieTrailersListView.setVisibility(View.INVISIBLE);
        mMovieTrailersListView.setAdapter(null);
        mMovieReviewsListView.setVisibility(View.INVISIBLE);
        mMovieReviewsListView.setAdapter(null);
        mMovieOverviewView.setVisibility(View.INVISIBLE);
        if (mMovieNameView != null) {
            mMovieNameView.setVisibility(View.INVISIBLE);
        }
        mTrailersEmptyView.setVisibility(View.INVISIBLE);
        mReviewsEmptyView.setVisibility(View.INVISIBLE);
        clearViews();
    }

    private void loadDataAndFinalizeUIComponents (String data[]) {

        fixRatingsViewColorScheme();

        if (data != null) {

            String backdropPath = data[0];
            String posterPath   = data[1];
            String dateValue    = data[2];
            String ratings      = data[4];
            String overview     = data[5];
            mMovieName          = data[6];
            String movieIdStr   = data[7];

            if (!Utility.isStringEmpty(backdropPath)) {
                Picasso.with(mContext).load(Utility.getImageURL(backdropPath)).error(R.drawable.no_image_available).into(mBackgroundImageView);
            }

            Picasso.with(mContext).load(Utility.getImageURL(posterPath)).error(R.drawable.no_image_available).into(mMovieImageView);

            if (Utility.isStringEmpty(dateValue)) {
                mMovieYearView.setText(getString(R.string.label_text_view_no_date));
            } else {
                mMovieYearView.setText(dateValue);
            }

            if (Utility.isStringEmpty(overview)) {
                mMovieOverviewView.setText(getString(R.string.label_text_view_no_overview));
            } else {
                mMovieOverviewView.setText(overview);
            }

            mMovieRatingsView.setRating(Float.valueOf(ratings) / 2f);
            movieId = Integer.valueOf(movieIdStr);

        } // end of data if

        mMovieTrailersListView.setAdapter(mTrailersListAdapter);
        mMovieReviewsListView.setAdapter(mReviewsListAdapter);

        mTrailersListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (mIsPreferenceChanged) {
                    mTrailersEmptyView.setText("");
                }
                else if (mTrailersListAdapter.getCount() <= 0) {
                    mTrailersEmptyView.setText(R.string.label_text_view_empty_trailers);
                }
                mMovieTrailersListView.setEmptyView(mTrailersEmptyView);
            }

        });

        mReviewsListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (mIsPreferenceChanged) {
                    mReviewsEmptyView.setText("");
                }
                else if (mReviewsListAdapter.getCount() <= 0) {
                    mReviewsEmptyView.setText(R.string.label_text_view_empty_reviews);
                }
                mMovieReviewsListView.setEmptyView(mReviewsEmptyView);
            }

        });

        mFavoriteButton.setOnClickListener(this);

        if (isMovieFavorite()) {
            mFavoriteButton.setBackgroundColor(getResources().getColor(R.color.favorite_selected));
            mFavoriteButton.setTextColor(getResources().getColor(R.color.text_favorite_selected));
        }

    }

    private void fixRatingsViewColorScheme() {
        LayerDrawable stars = (LayerDrawable) mMovieRatingsView.getProgressDrawable();
        stars.getDrawable(2).setColorFilter(getResources().getColor(R.color.starFullySelected), PorterDuff.Mode.SRC_ATOP);
        stars.getDrawable(1).setColorFilter(getResources().getColor(R.color.starPartiallySelected), PorterDuff.Mode.SRC_ATOP);
        stars.getDrawable(0).setColorFilter(getResources().getColor(R.color.starNotSelected), PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(MOVIE_TRAILER_LOADER, null, this);
        getLoaderManager().initLoader(MOVIE_REVIEW_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        switch (loaderId) {
            case MOVIE_TRAILER_LOADER:
                Uri trackUri = PopularMoviesContract.MovieTrailerEntry.buildMovieTrailerUri(movieId);

                return new CursorLoader(getActivity(),
                        trackUri,
                        MOVIE_TRAILER_COLUMNS,
                        null,
                        null,
                        null);

            case MOVIE_REVIEW_LOADER:
                Uri reviewUri = PopularMoviesContract.MovieReviewEntry.buildMovieReviewsUri(movieId);

                return new CursorLoader(getActivity(),
                        reviewUri,
                        MOVIE_REVIEWS_COLUMNS,
                        null,
                        null,
                        null);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case MOVIE_TRAILER_LOADER:
                mTrailersListAdapter.swapCursor(cursor);
                Utility.setDynamicHeight(mMovieTrailersListView);
                break;
            case MOVIE_REVIEW_LOADER:
                mReviewsListAdapter.swapCursor(cursor);
                Utility.setDynamicHeight(mMovieReviewsListView);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case MOVIE_TRAILER_LOADER:
                mTrailersListAdapter.swapCursor(null);
                break;
            case MOVIE_REVIEW_LOADER:
                mReviewsListAdapter.swapCursor(null);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        boolean isAlreadyFavorite = isMovieFavorite();

        int labelId = -1;
        int backgroundColorId = -1;
        int textColorId = -1;

        Set<String> favoriteMovieIdsSet = null;

        if (mPrefs.contains(FAVORITE_MOVIE_IDS_SET_KEY)) {
            favoriteMovieIdsSet = mPrefs.getStringSet(FAVORITE_MOVIE_IDS_SET_KEY, null);
        }

        if (favoriteMovieIdsSet == null) {
            favoriteMovieIdsSet = new LinkedHashSet<>();
        }

        if (isAlreadyFavorite) {
            favoriteMovieIdsSet.remove(Integer.toString(movieId));
            labelId = R.string.label_movie_marked_not_favorite;
            backgroundColorId = R.color.favorite_not_selected;
            textColorId = R.color.text_favorite_not_selected;

        } else {
            favoriteMovieIdsSet.add(Integer.toString(movieId));
            final SharedPreferences.Editor prefsEdit = mPrefs.edit();
            prefsEdit.putStringSet(FAVORITE_MOVIE_IDS_SET_KEY, favoriteMovieIdsSet);
            prefsEdit.commit();

            labelId = R.string.label_movie_marked_favorite;
            backgroundColorId = R.color.favorite_selected;
            textColorId = R.color.text_favorite_selected;
        }

        if (mFavoriteToast != null) {
            mFavoriteToast.cancel();
        }

        mFavoriteToast = Toast.makeText(mContext, getString(labelId), Toast.LENGTH_SHORT);

        mFavoriteToast.show();
        mFavoriteButton.setBackgroundColor(getResources().getColor(backgroundColorId));
        mFavoriteButton.setTextColor(getResources().getColor(textColorId));

    }

    private boolean isMovieFavorite() {
        boolean result = false;
        Set<String> favoriteMovieIdsSet = null;

        if (mPrefs.contains(FAVORITE_MOVIE_IDS_SET_KEY)) {
            favoriteMovieIdsSet = mPrefs.getStringSet(FAVORITE_MOVIE_IDS_SET_KEY, null);
        }

        if (favoriteMovieIdsSet != null) {
            Iterator<String> favIterator = favoriteMovieIdsSet.iterator();

            while (favIterator.hasNext()) {
                String favMovieId = favIterator.next();
                if (favMovieId.equals(Integer.toString(movieId))) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    public void setTwoPane(boolean mTwoPane) {
        this.mTwoPane = mTwoPane;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_sort_order_key))) {
            hideUIViews();
            mIsPreferenceChanged = true;
        }
    }

}
