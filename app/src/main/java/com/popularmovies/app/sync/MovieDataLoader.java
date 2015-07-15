package com.popularmovies.app.sync;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import com.popularmovies.app.MovieDetailsActivityFragment;
import com.popularmovies.app.Utility;
import com.popularmovies.app.data.PopularMoviesContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

/**
 * Created by Sheraz on 7/10/2015.
 */
public class MovieDataLoader {
    public final String LOG_TAG = MovieDataLoader.class.getSimpleName();
    private static MovieDataLoader _instance = null;
    private Integer mMovieIds[];
    private Context mContext;
    private Time mDayTime = new Time();
    private int mJulianStartDay = 0;
    private String mSortByParamValue = "popularity.desc";

    private MovieDataLoader() {}

    public static MovieDataLoader getInstance() {
        if (null == _instance) {
            _instance = new MovieDataLoader();
        }

        return _instance;
    }

    public void loadData(Context context) {
        mContext = context;
        mDayTime.setToNow();

        // we start at the day returned by local time. Otherwise this is a mess.
        mJulianStartDay = Time.getJulianDay(System.currentTimeMillis(), mDayTime.gmtoff);
        deleteOldData();

        if (Utility.getPreferredSortOrder(mContext) != null) {
            mSortByParamValue = Utility.getPreferredSortOrder(mContext);
        }

        if(mSortByParamValue.equals("favorite")) {
            mMovieIds = loadFavoriteMovieIds();
            loadFavoriteMovieData();
        } else {
            mMovieIds = loadMovieData();
        }

        if (mMovieIds == null || mMovieIds.length == 0) return;

        FetchMovieTrailersDataTask trailersDataTask = new FetchMovieTrailersDataTask();
        trailersDataTask.execute();

//        FetchMovieExtraDataTask extraDataTask = new FetchMovieExtraDataTask();
//        extraDataTask.execute();

    }

    private Integer[] loadFavoriteMovieIds () {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        Set<String> favoritMovieIdsSet =  prefs.getStringSet(MovieDetailsActivityFragment.FAVORITE_MOVIE_IDS_SET_KEY, null);

        if (favoritMovieIdsSet != null) {
            Integer[] array = new Integer[favoritMovieIdsSet.size()];

            Iterator<String> movieIdsIter = favoritMovieIdsSet.iterator();

            int i = 0;
            while (movieIdsIter.hasNext()) {
                array[i] = Integer.valueOf(movieIdsIter.next());
                i = i + 1;
            }
            return array;
        }

        return null;
    }

    private Integer[] loadMovieData() {

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        try {
            final String MOVIES_BASE_URL = "http://api.themoviedb.org/3/discover/movie?";
            final String SORT_BY_PARAM = "sort_by";
            final String API_KEY_PARAM = "api_key";

            Uri builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                    .appendQueryParameter(SORT_BY_PARAM, mSortByParamValue)
                    .appendQueryParameter(API_KEY_PARAM, Utility.API_KEY)
                    .build();

            URL url = new URL(builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }

            return getMoviesDataFromJson(buffer.toString());

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error " + e.getMessage(), e);
            e.printStackTrace();
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return null;
    }

    private Integer[] getMoviesDataFromJson(String moviesJsonStr)
            throws JSONException {
        Integer[] movieIds = new Integer[0];

        final String OWM_RESULTS_ARRAY = "results";
        final String OWM_ADULT = "adult";
        final String OWN_BACKDROP_PATH = "backdrop_path";
        final String OWM_GENRE_IDS = "genre_ids";
        final String OWM_ID = "id";
        final String OWM_ORIGINAL_LANGUAGE = "original_language";
        final String OWM_ORIGINAL_TITLE = "original_title";
        final String OWM_OVERVIEW = "overview";
        final String OWM_RELEASE_DATE = "release_date";
        final String OWM_POSTER_PATH = "poster_path";
        final String OWM_POPULARITY = "popularity";
        final String OWM_TITLE = "title";
        final String OWM_VIDEO = "video";
        final String OWM_VOTE_AVERAGE = "vote_average";
        final String OWM_VOTE_COUNT = "vote_count";

        // Since this data is also sent in-order and the first day is always the
        // current day, we're going to take advantage of that to get a nice
        // normalized UTC date for all of our weather.


        try{
            JSONObject moviesJson = new JSONObject(moviesJsonStr);
            JSONArray moviesArray = moviesJson.getJSONArray(OWM_RESULTS_ARRAY);

            Vector<ContentValues> cVVector = new Vector<ContentValues>(moviesArray.length());
            movieIds = new Integer[moviesArray.length()];

            for(int i = 0; i < moviesArray.length(); i++) {
                JSONObject movieJSONObject = moviesArray.getJSONObject(i);

                Integer movieId = movieJSONObject.getInt(OWM_ID);
                boolean isAdult = movieJSONObject.getBoolean(OWM_ADULT);
                String backdropPath = movieJSONObject.getString(OWN_BACKDROP_PATH);
                String originalLanguage = movieJSONObject.getString(OWM_ORIGINAL_LANGUAGE);
                String originalTitle = movieJSONObject.getString(OWM_ORIGINAL_TITLE);
                String overview = movieJSONObject.getString(OWM_OVERVIEW);
                String releaseDate = movieJSONObject.getString(OWM_RELEASE_DATE);
                String posterPath = movieJSONObject.getString(OWM_POSTER_PATH);
                Double popularity = movieJSONObject.getDouble(OWM_POPULARITY);
                String title = movieJSONObject.getString(OWM_TITLE);
                boolean isVideo = movieJSONObject.getBoolean(OWM_VIDEO);
                Double voteAverage = movieJSONObject.getDouble(OWM_VOTE_AVERAGE);
                Integer voteCount = movieJSONObject.getInt(OWM_VOTE_COUNT);

                ContentValues movieValues = new ContentValues();

                movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_MOVIE_ID, movieId);
                movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_IS_ADULT, isAdult);
                movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_BACK_DROP_PATH, backdropPath);
                movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_ORIGINAL_LANGUAGE, originalLanguage);
                movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_ORIGINAL_TITLE, originalTitle);
                movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_OVERVIEW, overview);
                movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_RELEASE_DATE, releaseDate);
                movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_POSTER_PATH, posterPath);
                movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_POPULARITY, popularity);
                movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_TITLE, title);
                movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_IS_VIDEO, isVideo);
                movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_VOTE_AVERAGE, voteAverage);
                movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_VOTE_COUNT, voteCount);
                movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_DATE, mDayTime.setJulianDay(mJulianStartDay));

                cVVector.add(movieValues);
                movieIds[i] = movieId;

//                JSONArray genreIds = movieJSONObject.getJSONArray(OWM_GENRE_IDS);
//                Integer[] genreIdsArray = new Integer[genreIds.length()];
//
//                for(int j = 0; j < genreIds.length(); j++) {
//                    genreIdsArray[j] = genreIds.getInt(j);
//                }
//

            }// end of for

            int inserted = 0;
            // add to database
            if ( cVVector.size() > 0 ) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                inserted = mContext.getContentResolver().bulkInsert(PopularMoviesContract.MovieEntry.CONTENT_URI, cvArray);

//                notifyWeather();
            }

            Log.d(LOG_TAG, "Movie Initial Data Loading Task Complete. " + inserted + " rows inserted");


        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        return movieIds;
    }

    private void loadFavoriteMovieData() {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            int inserted = 0;

            try {
//                http://api.themoviedb.org/3/movie/31413?api_key=28edec251247e1d7328ab3ec7f483cd0
                final String MOVIES_BASE_URL = "http://api.themoviedb.org/3/movie";
                final String API_KEY_PARAM = "api_key";

                if (mMovieIds == null || mMovieIds.length == 0) return;

                for (int i = 0; i < mMovieIds.length; i++) {

                    Uri builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                            .appendPath(String.valueOf(mMovieIds[i]))
                            .appendQueryParameter(API_KEY_PARAM, Utility.API_KEY)
                            .build();

                    URL url = new URL(builtUri.toString());

                    // Create the request to OpenWeatherMap, and open the connection
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // Read the input stream into a String
                    InputStream inputStream = null;
                    try {
                        inputStream = urlConnection.getInputStream();
                    }catch (Exception e) {}

                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        // Nothing to do.
                        continue;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                        // But it does make debugging a *lot* easier if you print out the completed
                        // buffer for debugging.
                        buffer.append(line + "\n");
                    }

                    if (buffer.length() == 0) {
                        // Stream was empty.  No point in parsing.
                        continue;
                    }

                    inserted = inserted + getMovieDataFromJson(buffer.toString());

                }//end of for

                Log.d(LOG_TAG, "Movie Initial Data Loading Task Complete. " + inserted + " rows inserted");
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error " + e.getMessage(), e);
                e.printStackTrace();
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return;

    }

    private int getMovieDataFromJson(String moviesJsonStr)
            throws JSONException {
        Integer[] movieIds = new Integer[0];

        final String OWM_RESULTS_ARRAY = "results";
        final String OWM_ADULT = "adult";
        final String OWN_BACKDROP_PATH = "backdrop_path";
        final String OWM_GENRE_IDS = "genre_ids";
        final String OWM_ID = "id";
        final String OWM_ORIGINAL_LANGUAGE = "original_language";
        final String OWM_ORIGINAL_TITLE = "original_title";
        final String OWM_OVERVIEW = "overview";
        final String OWM_RELEASE_DATE = "release_date";
        final String OWM_POSTER_PATH = "poster_path";
        final String OWM_POPULARITY = "popularity";
        final String OWM_TITLE = "title";
        final String OWM_VIDEO = "video";
        final String OWM_VOTE_AVERAGE = "vote_average";
        final String OWM_VOTE_COUNT = "vote_count";

        // Since this data is also sent in-order and the first day is always the
        // current day, we're going to take advantage of that to get a nice
        // normalized UTC date for all of our weather.


        try{
            JSONObject movieJSONObject = new JSONObject(moviesJsonStr);

            Vector<ContentValues> cVVector = new Vector<ContentValues>(1);

            Integer movieId = movieJSONObject.getInt(OWM_ID);
            boolean isAdult = movieJSONObject.getBoolean(OWM_ADULT);
            String backdropPath = movieJSONObject.getString(OWN_BACKDROP_PATH);
            String originalLanguage = movieJSONObject.getString(OWM_ORIGINAL_LANGUAGE);
            String originalTitle = movieJSONObject.getString(OWM_ORIGINAL_TITLE);
            String overview = movieJSONObject.getString(OWM_OVERVIEW);
            String releaseDate = movieJSONObject.getString(OWM_RELEASE_DATE);
            String posterPath = movieJSONObject.getString(OWM_POSTER_PATH);
            Double popularity = movieJSONObject.getDouble(OWM_POPULARITY);
            String title = movieJSONObject.getString(OWM_TITLE);
            boolean isVideo = movieJSONObject.getBoolean(OWM_VIDEO);
            Double voteAverage = movieJSONObject.getDouble(OWM_VOTE_AVERAGE);
            Integer voteCount = movieJSONObject.getInt(OWM_VOTE_COUNT);

            ContentValues movieValues = new ContentValues();

            movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_MOVIE_ID, movieId);
            movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_IS_ADULT, isAdult);
            movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_BACK_DROP_PATH, backdropPath);
            movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_ORIGINAL_LANGUAGE, originalLanguage);
            movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_ORIGINAL_TITLE, originalTitle);
            movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_OVERVIEW, overview);
            movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_RELEASE_DATE, releaseDate);
            movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_POSTER_PATH, posterPath);
            movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_POPULARITY, popularity);
            movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_TITLE, title);
            movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_IS_VIDEO, isVideo);
            movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_VOTE_AVERAGE, voteAverage);
            movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_VOTE_COUNT, voteCount);
            movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_DATE, mDayTime.setJulianDay(mJulianStartDay));

            cVVector.add(movieValues);

            int inserted = 0;
            // add to database
            if ( cVVector.size() > 0 ) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                inserted = mContext.getContentResolver().bulkInsert(PopularMoviesContract.MovieEntry.CONTENT_URI, cvArray);
            }
            return inserted;

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        return -1;
    }

    public class FetchMovieExtraDataTask extends AsyncTask<Void, Void, Void> {

        private final String LOG_TAG = FetchMovieExtraDataTask.class.getSimpleName();


        @Override
        protected Void doInBackground(Void [] params) {


            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            try {
//                http://api.themoviedb.org/3/movie/31413?api_key=28edec251247e1d7328ab3ec7f483cd0
                final String MOVIES_BASE_URL = "http://api.themoviedb.org/3/movie";
                final String API_KEY_PARAM = "api_key";

                for (int i = 0; i < mMovieIds.length; i++) {

                    Uri builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                            .appendPath(String.valueOf(mMovieIds[i]))
                            .appendQueryParameter(API_KEY_PARAM, Utility.API_KEY)
                            .build();

                    URL url = new URL(builtUri.toString());

                    // Create the request to OpenWeatherMap, and open the connection
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // Read the input stream into a String
                    InputStream inputStream = null;
                    try {
                        inputStream = urlConnection.getInputStream();
                    }catch (Exception e) {}

                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        // Nothing to do.
                        continue;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                        // But it does make debugging a *lot* easier if you print out the completed
                        // buffer for debugging.
                        buffer.append(line + "\n");
                    }

                    if (buffer.length() == 0) {
                        // Stream was empty.  No point in parsing.
                        continue;
                    }

                    getMoviesDetailsDataFromJson(buffer.toString());

                }//end of for
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error " + e.getMessage(), e);
                e.printStackTrace();
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return null;
        }

        private void getMoviesDetailsDataFromJson(String moviesJsonStr)
                throws JSONException {

            final String OWM_MOVIE_ID = "id";
            final String OWM_RUNTIME = "runtime";
            final String OWM_STATUS = "status";
            try {
                JSONObject moviesJson = new JSONObject(moviesJsonStr);
                Integer movieId = moviesJson.getInt(OWM_MOVIE_ID);
                Integer runtime = moviesJson.getInt(OWM_RUNTIME);
                String status = moviesJson.getString(OWM_STATUS);

                ContentValues movieValues = new ContentValues();

                movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_MOVIE_ID, movieId);
                movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_RUNTIME, runtime);
                movieValues.put(PopularMoviesContract.MovieEntry.COLUMN_STATUS, status);

                mContext.getContentResolver().update(PopularMoviesContract.MovieEntry.CONTENT_URI, movieValues,
                        PopularMoviesContract.MovieEntry.COLUMN_MOVIE_ID + " = ?", new String[]{Integer.toString(movieId)});


            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
        }

        @Override
        protected void onPostExecute(Void params) {
            FetchMovieTrailersDataTask trailersDataTask = new FetchMovieTrailersDataTask();
            trailersDataTask.execute();
        }
    }
    public class FetchMovieTrailersDataTask extends AsyncTask<Void, Void, Void> {

        private final String LOG_TAG = FetchMovieTrailersDataTask.class.getSimpleName();


        @Override
        protected Void doInBackground(Void[] params) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            int inserted = 0;

            try {
//            http://api.themoviedb.org/3/movie/31413/videos?api_key=28edec251247e1d7328ab3ec7f483cd0
                final String MOVIES_BASE_URL = "http://api.themoviedb.org/3/movie";
                final String API_KEY_PARAM = "api_key";
                final String VIDEOS = "videos";

                for (int i = 0; i < mMovieIds.length; i++) {

                    Uri builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                            .appendPath(String.valueOf(mMovieIds[i]))
                            .appendPath(VIDEOS)
                            .appendQueryParameter(API_KEY_PARAM, Utility.API_KEY)
                            .build();

                    URL url = new URL(builtUri.toString());

                    // Create the request to OpenWeatherMap, and open the connection
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // Read the input stream into a String
                    InputStream inputStream = null;
                    try {
                        inputStream = urlConnection.getInputStream();
                    } catch (Exception e) {}
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        // Nothing to do.
                        continue;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                        // But it does make debugging a *lot* easier if you print out the completed
                        // buffer for debugging.
                        buffer.append(line + "\n");
                    }

                    if (buffer.length() == 0) {
                        // Stream was empty.  No point in parsing.
                        continue;
                    }

                    inserted = inserted + getTrailersDataFromJson(buffer.toString());

                }//end of for
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error " + e.getMessage(), e);
                e.printStackTrace();
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            Log.d(LOG_TAG, "Movie Trailer Data Loading Task Complete. " + inserted + " rows inserted");
            return null;
        }

        private int getTrailersDataFromJson(String moviesJsonStr)
                throws JSONException {

            final String OWM_MOVIE_ID = "id";
            final String OWM_RESULTS_ARRAY = "results";
            final String OWM_TRAILER_ID = "id";
            final String OWM_ISO_639_1 = "iso_639_1";
            final String OWM_KEY = "key";
            final String OWM_NAME = "name";
            final String OWM_SITE = "site";
            final String OWM_SIZE = "size";
            final String OWM_TYPE = "type";
            int inserted = 0;

            try {
                JSONObject moviesJson = new JSONObject(moviesJsonStr);
                Integer movieId = moviesJson.getInt(OWM_MOVIE_ID);

                JSONArray moviesArray = moviesJson.getJSONArray(OWM_RESULTS_ARRAY);
                Vector<ContentValues> cVVector = new Vector<ContentValues>(moviesArray.length());

                for (int i = 0; i < moviesArray.length(); i++) {
                    JSONObject movieJSONObject = moviesArray.getJSONObject(i);

                    String trailerId = movieJSONObject.getString(OWM_TRAILER_ID);
                    String ISO_639_1 = movieJSONObject.getString(OWM_ISO_639_1);
                    String key = movieJSONObject.getString(OWM_KEY);
                    String name = movieJSONObject.getString(OWM_NAME);
                    String site = movieJSONObject.getString(OWM_SITE);
                    String size = movieJSONObject.getString(OWM_SIZE);
                    String type = movieJSONObject.getString(OWM_TYPE);


                    ContentValues trailerValues = new ContentValues();

                    trailerValues.put(PopularMoviesContract.MovieTrailerEntry.COLUMN_MOVIE_ID, movieId);
                    trailerValues.put(PopularMoviesContract.MovieTrailerEntry.COLUMN_TRAILER_ID, trailerId);
                    trailerValues.put(PopularMoviesContract.MovieTrailerEntry.COLUMN_ISO_369_1, ISO_639_1);
                    trailerValues.put(PopularMoviesContract.MovieTrailerEntry.COLUMN_KEY, key);
                    trailerValues.put(PopularMoviesContract.MovieTrailerEntry.COLUMN_NAME, name);
                    trailerValues.put(PopularMoviesContract.MovieTrailerEntry.COLUMN_SITE, site);
                    trailerValues.put(PopularMoviesContract.MovieTrailerEntry.COLUMN_SIZE, size);
                    trailerValues.put(PopularMoviesContract.MovieTrailerEntry.COLUMN_TYPE, type);
                    trailerValues.put(PopularMoviesContract.MovieEntry.COLUMN_DATE, mDayTime.setJulianDay(mJulianStartDay));

                    cVVector.add(trailerValues);
                }

                // add to database
                if (cVVector.size() > 0) {
                    ContentValues[] cvArray = new ContentValues[cVVector.size()];
                    cVVector.toArray(cvArray);
                    inserted = mContext.getContentResolver().bulkInsert(PopularMoviesContract.MovieTrailerEntry.CONTENT_URI, cvArray);
                }

            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return inserted;
        }

        @Override
        protected void onPostExecute(Void params) {
            FetchMovieReviewsDataTask reviewsDataTask = new FetchMovieReviewsDataTask();
            reviewsDataTask.execute();
        }
    }// end of Trailers Task

    public class FetchMovieReviewsDataTask extends AsyncTask<Void, Void, Void> {

        private final String LOG_TAG = FetchMovieReviewsDataTask.class.getSimpleName();

        @Override
        protected Void doInBackground(Void[] params) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            int inserted = 0;

            try {
//            http://api.themoviedb.org/3/movie/135397/reviews?api_key=28edec251247e1d7328ab3ec7f483cd0
                final String MOVIES_BASE_URL = "http://api.themoviedb.org/3/movie";
                final String API_KEY_PARAM = "api_key";
                final String REVIEWS = "reviews";

                for (int i = 0; i < mMovieIds.length; i++) {

                    Uri builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                            .appendPath(String.valueOf(mMovieIds[i]))
                            .appendPath(REVIEWS)
                            .appendQueryParameter(API_KEY_PARAM, Utility.API_KEY)
                            .build();

                    URL url = new URL(builtUri.toString());

                    // Create the request to OpenWeatherMap, and open the connection
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // Read the input stream into a String
                    InputStream inputStream = null;
                    try {
                        inputStream = urlConnection.getInputStream();
                    }catch (Exception e){}

                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        // Nothing to do.
                        continue;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                        // But it does make debugging a *lot* easier if you print out the completed
                        // buffer for debugging.
                        buffer.append(line + "\n");
                    }

                    if (buffer.length() == 0) {
                        // Stream was empty.  No point in parsing.
                        continue;
                    }

                    inserted = inserted + getReviewsDataFromJson(buffer.toString());

                }//end of for
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error " + e.getMessage(), e);
                e.printStackTrace();
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            Log.d(LOG_TAG, "Movie Reviews Data Loading Task Complete. " + inserted + " rows inserted");
            return null;
        }


        private int getReviewsDataFromJson(String moviesJsonStr)
                throws JSONException {

            final String OWM_MOVIE_ID = "id";
            final String OWM_RESULTS_ARRAY = "results";
            final String OWM_REVIEW_ID = "id";
            final String OWM_AUTHOR = "author";
            final String OWM_CONTENT = "content";
            final String OWM_URL = "url";
            int inserted = 0;

            try {
                JSONObject moviesJson = new JSONObject(moviesJsonStr);
                Integer movieId = moviesJson.getInt(OWM_MOVIE_ID);

                JSONArray moviesArray = moviesJson.getJSONArray(OWM_RESULTS_ARRAY);
                Vector<ContentValues> cVVector = new Vector<ContentValues>(moviesArray.length());

                for (int i = 0; i < moviesArray.length(); i++) {
                    JSONObject movieJSONObject = moviesArray.getJSONObject(i);

                    String reviewId = movieJSONObject.getString(OWM_REVIEW_ID);
                    String author = movieJSONObject.getString(OWM_AUTHOR);
                    String content = movieJSONObject.getString(OWM_CONTENT);
                    String url = movieJSONObject.getString(OWM_URL);


                    ContentValues reviewValues = new ContentValues();

                    reviewValues.put(PopularMoviesContract.MovieReviewEntry.COLUMN_MOVIE_ID, movieId);
                    reviewValues.put(PopularMoviesContract.MovieReviewEntry.COLUMN_REVIEW_ID, reviewId);
                    reviewValues.put(PopularMoviesContract.MovieReviewEntry.COLUMN_AUTHOR, author);
                    reviewValues.put(PopularMoviesContract.MovieReviewEntry.COLUMN_CONTENT, content);
                    reviewValues.put(PopularMoviesContract.MovieReviewEntry.COLUMN_URL, url);
                    reviewValues.put(PopularMoviesContract.MovieReviewEntry.COLUMN_DATE, mDayTime.setJulianDay(mJulianStartDay));

                    cVVector.add(reviewValues);
                }

                // add to database
                if (cVVector.size() > 0) {
                    ContentValues[] cvArray = new ContentValues[cVVector.size()];
                    cVVector.toArray(cvArray);
                    inserted = mContext.getContentResolver().bulkInsert(PopularMoviesContract.MovieReviewEntry.CONTENT_URI, cvArray);
                }

            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return inserted;
        }
    }
    private void deleteOldData() {
        // delete old data so we don't build up an endless history
        int deleted = mContext.getContentResolver().delete(PopularMoviesContract.MovieReviewEntry.CONTENT_URI, null, null);
//                PopularMoviesContract.MovieReviewEntry.COLUMN_DATE + " <= ?",
//                new String[]{Long.toString(mDayTime.setJulianDay(mJulianStartDay - 1))});

        Log.d(LOG_TAG, "Movie Review Data Deleting, " + deleted + " rows deleted.");

        // delete old data so we don't build up an endless history
        deleted = mContext.getContentResolver().delete(PopularMoviesContract.MovieTrailerEntry.CONTENT_URI, null, null);
//                PopularMoviesContract.MovieTrailerEntry.COLUMN_DATE + " <= ?",
//                new String[]{Long.toString(mDayTime.setJulianDay(mJulianStartDay - 1))});

        Log.d(LOG_TAG, "Movie Trailer Data Deleting, " + deleted + " rows deleted.");

        // delete old data so we don't build up an endless history
        deleted = mContext.getContentResolver().delete(PopularMoviesContract.MovieEntry.CONTENT_URI, null, null);
//                PopularMoviesContract.MovieEntry.COLUMN_DATE + " <= ?",
//                new String[]{Long.toString(mDayTime.setJulianDay(mJulianStartDay - 1))});

        Log.d(LOG_TAG, "Movie Data Deleting, " + deleted + " rows deleted.");
    }

}
