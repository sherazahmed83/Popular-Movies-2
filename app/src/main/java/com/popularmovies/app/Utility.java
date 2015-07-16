package com.popularmovies.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.popularmovies.app.sync.MovieDataLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Sheraz on 7/10/2015.
 */
public class Utility {
    public static final String API_KEY = "28edec251247e1d7328ab3ec7f483cd0";
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String IMAGE_BASE_URL = "http://image.tmdb.org/t/p/";
    private static final String IMAGE_SIZE = "w342";
    private static final String PATH_SEPARATOR = "/";
    // Developer Key URL is below
    //https://console.developers.google.com/project/popular-movies-2/apiui/credential?authuser=0#
    public static final String DEVELOPER_KEY = "AIzaSyDpzDiD894g3Um10YZPz0HuqPlfMmpSwoY";
    public static final String YOUTUBE_PACKAGE_NAME = "com.google.android.youtube";
    public static final String YOUTUBE_CLASS_NAME = "com.google.android.youtube.WatchActivity";


    public static String getPreferredSortOrder(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_sort_order_key),
                context.getString(R.string.pref_sort_order_default));
    }


    public static String getImageURL(String imagePath) {
        StringBuilder imageURL = new StringBuilder();

        imageURL.append(IMAGE_BASE_URL);
        imageURL.append(IMAGE_SIZE);
        imageURL.append(PATH_SEPARATOR);
        imageURL.append(imagePath);

        return imageURL.toString();
    }

    public static Integer getYearFromDate(String dateStr) {
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        try {
            Date inputDate = dbDateFormat.parse(dateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(inputDate);

            return cal.get(Calendar.YEAR);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void setDynamicHeight(ListView mListView) {
        ListAdapter mListAdapter = mListView.getAdapter();
        if (mListAdapter == null) {
            // when adapter is null
            return;
        }
        int height = 0;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(mListView.getWidth(), View.MeasureSpec.AT_MOST);
        for (int i = 0; i < mListAdapter.getCount(); i++) {
            View listItem = mListAdapter.getView(i, null, mListView);
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            height += listItem.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = mListView.getLayoutParams();
        params.height = height + (mListView.getDividerHeight() * (mListAdapter.getCount()));
        mListView.setLayoutParams(params);
        mListView.requestLayout();
    }

    public static boolean isStringEmpty(String string) {
        return (string == null || string.equals("null") || string.equals(""));
    }

    public static boolean isAppInstalled(String uri, Context context) {
        PackageManager pm = context.getPackageManager();
        boolean installed = false;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            installed = false;
        }
        return installed;
    }

    public static String getVideoHTML(String videoId) {

        String html =
                "<iframe class=\"youtube-player\" "
                        + "style=\"border: 0; width: 100%; height: 95%;"
                        + "padding:0px; margin:0px\" "
                        + "id=\"ytplayer\" type=\"text/html\" "
                        + "src=\"http://www.youtube.com/embed/" + videoId
                        + "?fs=0\" frameborder=\"0\" " + "allowfullscreen autobuffer "
                        + "controls onclick=\"this.play()\">\n" + "</iframe>\n";

/**
 * <iframe id="ytplayer" type="text/html" width="640" height="360"
 * src="https://www.youtube.com/embed/WM5HccvYYQg" frameborder="0"
 * allowfullscreen>
 **/

        return html;
    }

    public static boolean canResolveIntent(Intent intent, Context context) {
        List<ResolveInfo> resolveInfo = context.getPackageManager().queryIntentActivities(intent, 0);
        return resolveInfo != null && !resolveInfo.isEmpty();
    }

    public static String argsArrayToString(String[] args) {
        StringBuilder argsBuilder = new StringBuilder();

        final int argsCount = args.length;
        for (int i = 0; i < argsCount; i++) {
            argsBuilder.append(args[i]);

            if (i < argsCount - 1) {
                argsBuilder.append(",");
            }
        }

        return argsBuilder.toString();
    }

    public static boolean isMovieIdFavorite(String [] favoriteMovieIds, String movieId) {
        boolean result = false;

        if (favoriteMovieIds == null || favoriteMovieIds.length == 0) return result;

        for (int i = 0; i < favoriteMovieIds.length; i++) {
            if (movieId.trim().equals(favoriteMovieIds[i].trim())){
                result = true;
                break;
            }
        }

        return result;
    }

    @SuppressWarnings("ResourceType")
    public static @MovieDataLoader.MovieStatus int getMovieStatus(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(context.getString(R.string.pref_movie_status_key), MovieDataLoader.MOVIE_STATUS_UNKNOWN);
    }
}
