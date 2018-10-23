package com.danleech.cordova.plugin.imagePicker.features.recyclers;

import android.content.Context;
import android.graphics.PointF;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class RecyclerLayoutManager extends GridLayoutManager {

    private AppBarManager mAppBarManager;
    private int visibleHeightForRecyclerView;

    public RecyclerLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        View firstVisibleChild = recyclerView.getChildAt(0);
        final int childHeight = firstVisibleChild.getHeight();
        int distanceInPixels = ((findFirstVisibleItemPosition() - position) * childHeight);
        if (distanceInPixels == 0) {
            distanceInPixels = (int) Math.abs(firstVisibleChild.getY());
        }

        visibleHeightForRecyclerView = mAppBarManager.getVisibleHeightForRecyclerViewInPx();

        //Subtract one as adapter position 0 based
        final int visibleChildCount = visibleHeightForRecyclerView/childHeight;

//        if (position <= visibleChildCount) {
//            //Scroll to the very top and expand the app bar
//            position = 0;
//            mAppBarManager.expandAppBar();
//        } else {
//            mAppBarManager.collapseAppBar();
//        }

        // faster on bigger screen
        final int duration = (int) (10 / visibleChildCount + 0.5f);

        SmoothScroller smoothScroller = new SmoothScroller(recyclerView.getContext(), Math.abs(distanceInPixels), duration);
        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }

    public void setAppBarManager(AppBarManager appBarManager) {
        mAppBarManager = appBarManager;
    }

    private class SmoothScroller extends LinearSmoothScroller {
        private final float distanceInPixels;
        private final float duration;

        public SmoothScroller(Context context, int distanceInPixels, int duration) {
            super(context);
            this.distanceInPixels = distanceInPixels;
            float millisecondsPerPx = calculateSpeedPerPixel(context.getResources().getDisplayMetrics());
            this.duration = (int) (Math.abs(distanceInPixels) * millisecondsPerPx * duration);
        }

        @Override
        public PointF computeScrollVectorForPosition(int targetPosition) {
            return RecyclerLayoutManager.this
                    .computeScrollVectorForPosition(targetPosition);
        }

        @Override
        protected int calculateTimeForScrolling(int dx) {
            float proportion = (float) dx / distanceInPixels;
            return (int) (duration * proportion);
        }

        @Override
        protected int getVerticalSnapPreference() {
            return SNAP_TO_START;
        }
    }
}