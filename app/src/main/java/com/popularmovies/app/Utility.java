package com.popularmovies.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Sheraz on 7/10/2015.
 */
public class Utility {
    public static final String API_KEY = "28edec251247e1d7328ab3ec7f483cd0";
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String IMAGE_BASE_URL = "http://image.tmdb.org/t/p/";
    private static final String IMAGE_SIZE = "w342";
    private static final String PATH_SEPARATOR = "/";


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

}
