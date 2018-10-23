package com.danleech.cordova.plugin.imagePicker.view;

import android.content.Context;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.danleech.cordova.plugin.imagePicker.features.recyclers.AppBarManager;

public class AppBarPreviewLayout extends AppBarLayout implements AppBarManager, AppBarLayout.OnOffsetChangedListener, View.OnTouchListener {
    /**
     *  View complete show state.
     */
    int WHOLE_STATE = 0;

    /**
     * View collapse state.
     */
    int COLLAPSE_STATE = 1;

    private Handler mHandler = new Handler();

    private int mState = WHOLE_STATE;

    private boolean mTouchStart;
    private boolean mUpdateState = false;

    private UpdateStateRunnable mUpdateStateRunnable = new UpdateStateRunnable();

    public AppBarPreviewLayout(Context context) {
        this(context, null);
    }

    public AppBarPreviewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        this.addOnOffsetChangedListener(this);
        this.setOnTouchListener(this);
    }

    public void updateState() {
        mUpdateState = true;
    }

    private void onSwitchState(int offset, int type) {
        if(offset == 0) {
            mState = WHOLE_STATE;
            return;
        }

        final int height = getHeight();
        final int minHeight = height - getTotalScrollRange();
        final int scrollHeight = height + offset;

        if(scrollHeight == minHeight) {
            mState = COLLAPSE_STATE;
            return;
        }

        if( scrollHeight > height / 2) {
            setExpanded(true, true);
            mState = WHOLE_STATE;
        } else {
            setExpanded(false, true);
            mState = COLLAPSE_STATE;
        }
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        if(mUpdateState) {
            mHandler.removeCallbacks(mUpdateStateRunnable);
            mUpdateStateRunnable.SetOffset(verticalOffset);
            mHandler.postDelayed(mUpdateStateRunnable, 250);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouch(View v, MotionEvent ev) {
        final int action = ev.getAction();
        final int y = (int) ev.getRawY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mTouchStart = true;
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_CANCEL:
                updateState();
                mTouchStart = false;
                break;
            case MotionEvent.ACTION_UP:
                if (mTouchStart) {
                    setExpanded(true, true);
                }
                mTouchStart = false;
                break;
        }
        return true;
    }

    @Override
    public void collapseAppBar() {
        mUpdateState = false;
        mHandler.removeCallbacks(mUpdateStateRunnable);

        if( mState == WHOLE_STATE) {
            super.setExpanded(false, true);
            mState = COLLAPSE_STATE;
        }
    }

    @Override
    public void expandAppBar() {
        mUpdateState = false;
        mHandler.removeCallbacks(mUpdateStateRunnable);

        if( mState == COLLAPSE_STATE) {
            super.setExpanded(true, true);
            mState = WHOLE_STATE;
        }
    }

    @Override
    public int getVisibleHeightForRecyclerViewInPx() {
        final int height = getHeight();
        final int minHeight = height - getTotalScrollRange();
        final int parentHeight = ((View) this.getParent()).getHeight();

        if(mState == WHOLE_STATE)
            return parentHeight - height;

        return parentHeight - minHeight;
    }


    private class UpdateStateRunnable implements Runnable {
        int mOffset = 0;
        int mType = 0;
        public void SetOffset(int offset) { mOffset = offset; }
        public void SetType(int type) { mType = type; }
        public void run() {
            onSwitchState(mOffset, mType);
            mUpdateState = false;
        }
    }
}
