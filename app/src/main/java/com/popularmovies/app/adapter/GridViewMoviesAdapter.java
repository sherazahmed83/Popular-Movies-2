package com.popularmovies.app.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;

import com.popularmovies.app.MainActivityFragment;
import com.popularmovies.app.R;
import com.popularmovies.app.Utility;
import com.squareup.picasso.Picasso;

/**
 * Created by Sheraz on 7/9/2015.
 */
public class GridViewMoviesAdapter extends
        CursorAdapter {


    public GridViewMoviesAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        View view = LayoutInflater.from(context).inflate(R.layout.gridview_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder) view.getTag();

        String posterPath = cursor.getString(MainActivityFragment.COL_POSTER_PATH);

        if (posterPath == null) {
            posterPath = cursor.getString(MainActivityFragment.COL_BACK_DROP_PATH);
        }
        
        Picasso.with(context).load(Utility.getImageURL(posterPath)).error(R.drawable.no_image_available).into(viewHolder.imageView);
    }


    public static class ViewHolder {
        public final ImageView imageView;

        public ViewHolder(View vi) {
            imageView = (ImageView) vi.findViewById(R.id.imageView);
        }
    }
}
