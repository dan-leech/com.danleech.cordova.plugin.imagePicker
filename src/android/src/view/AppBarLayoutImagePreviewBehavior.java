package com.danleech.cordova.plugin.imagePicker.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.lang.reflect.Method;

import static android.support.v4.view.ViewCompat.TYPE_NON_TOUCH;

public class AppBarLayoutImagePreviewBehavior extends AppBarLayout.Behavior {
    private static final String TAG = "ImagePreviewBehavior";

    private int mOffsetDelta;
    private int mOffsetSpring;
    private ValueAnimator mSpringRecoverAnimator;
    private ValueAnimator mFlingAnimator;
    private int mPreHeadHeight;
    private SpringOffsetCallback mSpringOffsetCallback;

    public AppBarLayoutImagePreviewBehavior() { }

    public AppBarLayoutImagePreviewBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);

        // allow dragging
        this.setDragCallback(new DragCallback() {
            @Override
            public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
                return true;
            }
        });
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout parent, AppBarLayout child, View directTargetChild, View target, int nestedScrollAxes, int type) {
        final boolean started = super.onStartNestedScroll(parent, child, directTargetChild, target, nestedScrollAxes, type);
        if (started && mSpringRecoverAnimator != null && mSpringRecoverAnimator.isRunning()) {
            mSpringRecoverAnimator.cancel();
        }
        resetFlingAnimator();

        if (child instanceof AppBarPreviewLayout) {
            ((AppBarPreviewLayout) child).updateState();
        }

        return started;
    }

    private void resetFlingAnimator() {
        if (mFlingAnimator != null) {
            if (mFlingAnimator.isRunning()) {
                mFlingAnimator.cancel();
            }
            mFlingAnimator = null;
        }
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        if (dyUnconsumed < 0) {
            setHeaderTopBottomOffset(coordinatorLayout, child,
                    getTopBottomOffsetForScrollingSibling() - dyUnconsumed, -1 * getDownNestedScrollRange(child), 0, type);
        }
    }

    int getDownNestedScrollRange(AppBarLayout view) {
        try {
            Method m;
            if (view instanceof AppBarPreviewLayout)
                m = view.getClass().getSuperclass().getDeclaredMethod("getDownNestedScrollRange");
            else
                m = view.getClass().getDeclaredMethod("getDownNestedScrollRange");
            m.setAccessible(true);
            return (int) m.invoke(view);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, AppBarLayout abl, View target, int type) {
        super.onStopNestedScroll(coordinatorLayout, abl, target, type);

        if (type == TYPE_NON_TOUCH) {
            resetFlingAnimator();
        }

        if (abl instanceof AppBarPreviewLayout) {
            ((AppBarPreviewLayout) abl).updateState();
        }

        checkShouldSpringRecover(coordinatorLayout, abl);
    }

    private void checkShouldSpringRecover(CoordinatorLayout coordinatorLayout, AppBarLayout abl) {
        if (mOffsetSpring > 0) animateRecoverBySpring(coordinatorLayout, abl);
    }

    private void animateFlingSpring(final CoordinatorLayout coordinatorLayout, final AppBarLayout abl, int originNew) {
        if (mFlingAnimator == null) {
            mFlingAnimator = new ValueAnimator();
            mFlingAnimator.setDuration(200);
            mFlingAnimator.setInterpolator(new DecelerateInterpolator());
            mFlingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    updateSpringHeaderHeight(coordinatorLayout, abl, (int) animation.getAnimatedValue());
                }
            });
            mFlingAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    checkShouldSpringRecover(coordinatorLayout, abl);
                }
            });
        } else {
            if (mFlingAnimator.isRunning()) {
                mFlingAnimator.cancel();
            }
        }
        mFlingAnimator.setIntValues(mOffsetSpring, Math.min(mPreHeadHeight * 3 / 2, originNew));
        mFlingAnimator.start();
    }

    private void animateRecoverBySpring(final CoordinatorLayout coordinatorLayout, final AppBarLayout abl) {
        if (mSpringRecoverAnimator == null) {
            mSpringRecoverAnimator = new ValueAnimator();
            mSpringRecoverAnimator.setDuration(200);
            mSpringRecoverAnimator.setInterpolator(new DecelerateInterpolator());
            mSpringRecoverAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    updateSpringHeaderHeight(coordinatorLayout, abl, (int) animation.getAnimatedValue());
                }
            });
        } else {
            if (mSpringRecoverAnimator.isRunning()) {
                mSpringRecoverAnimator.cancel();
            }
        }
        mSpringRecoverAnimator.setIntValues(mOffsetSpring, 0);
        mSpringRecoverAnimator.start();
    }

    @Override
    public boolean onMeasureChild(CoordinatorLayout parent, AppBarLayout child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        boolean b = super.onMeasureChild(parent, child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
        if (mPreHeadHeight == 0 && child.getHeight() != 0) {
            mPreHeadHeight = getHeaderExpandedHeight(child);
        }
        return b;
    }

    int getHeaderExpandedHeight(AppBarLayout appBarLayout) {
        int range = 0;
        for (int i = 0, z = appBarLayout.getChildCount(); i < z; i++) {
            final View child = appBarLayout.getChildAt(i);
            final AppBarLayout.LayoutParams lp = (AppBarLayout.LayoutParams) child.getLayoutParams();
            int childHeight = child.getMeasuredHeight();
            childHeight += lp.topMargin + lp.bottomMargin;
            range += childHeight;
        }
        return Math.max(0, range);
    }

    int setHeaderTopBottomOffset(CoordinatorLayout coordinatorLayout,
                                 AppBarLayout appBarLayout, int newOffset, int minOffset, int maxOffset, int type) {
        int originNew = newOffset;
        final int curOffset = getTopBottomOffsetForScrollingSibling();
        int consumed = 0;
        if (mOffsetSpring != 0 && newOffset < 0) {
            int newSpringOffset = mOffsetSpring + originNew;
            if (newSpringOffset < 0) {
                newOffset = newSpringOffset;
                newSpringOffset = 0;
            }
            updateSpringOffsetByscroll(coordinatorLayout, appBarLayout, newSpringOffset);
            consumed = getTopBottomOffsetForScrollingSibling() - originNew;
            if (newSpringOffset >= 0)
                return consumed;
        }

        if (mOffsetSpring > 0 && appBarLayout.getHeight() >= mPreHeadHeight && newOffset > 0) {
            consumed = updateSpringByScroll(coordinatorLayout, appBarLayout, type, originNew);
            return consumed;
        }

        if (minOffset != 0 && curOffset >= minOffset && curOffset <= maxOffset) {
            newOffset = clamp(newOffset, minOffset, maxOffset);
            if (curOffset != newOffset) {
                final int interpolatedOffset = hasChildWithInterpolator(appBarLayout)
                        ? interpolateOffset(appBarLayout, newOffset)
                        : newOffset;

                final boolean offsetChanged = setTopAndBottomOffset(interpolatedOffset);
                consumed = curOffset - newOffset;
                mOffsetDelta = newOffset - interpolatedOffset;
                if (!offsetChanged && hasChildWithInterpolator(appBarLayout)) {
                    coordinatorLayout.dispatchDependentViewsChanged(appBarLayout);
                }

                try {
                    Method m;
                    if (appBarLayout instanceof AppBarPreviewLayout)
                        m = appBarLayout.getClass().getSuperclass().getDeclaredMethod("dispatchOffsetUpdates", int.class);
                    else
                        m = appBarLayout.getClass().getDeclaredMethod("dispatchOffsetUpdates", int.class);
                    m.setAccessible(true);
                    m.invoke(appBarLayout, getTopAndBottomOffset());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                updateAppBarLayoutDrawableState(coordinatorLayout, appBarLayout, newOffset,
                        newOffset < curOffset ? -1 : 1, false);
            } else if (curOffset != minOffset) {
                consumed = updateSpringByScroll(coordinatorLayout, appBarLayout, type, originNew);
            }
        } else {
            mOffsetDelta = 0;
        }
        return consumed;
    }

    boolean hasChildWithInterpolator(AppBarLayout view) {
        try {
            Method m;
            if (view instanceof AppBarPreviewLayout)
                m = view.getClass().getSuperclass().getDeclaredMethod("hasChildWithInterpolator");
            else
                m = view.getClass().getDeclaredMethod("hasChildWithInterpolator");
            m.setAccessible(true);
            return (boolean) m.invoke(view);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private int updateSpringByScroll(CoordinatorLayout coordinatorLayout, AppBarLayout appBarLayout, int type, int originNew) {
        int consumed;
        if (appBarLayout.getHeight() >= mPreHeadHeight && type == 1) {
            if (mFlingAnimator == null)
                animateFlingSpring(coordinatorLayout, appBarLayout, originNew);
            return originNew;
        }
        updateSpringOffsetByscroll(coordinatorLayout, appBarLayout, mOffsetSpring + originNew / 3);
        consumed = getTopBottomOffsetForScrollingSibling() - originNew;

        return consumed;
    }

    int getTopBottomOffsetForScrollingSibling() {
        return getTopAndBottomOffset() + mOffsetDelta;
    }

    int interpolateOffset(AppBarLayout layout, final int offset) {
        try {
            Method m = getClass().getSuperclass().getDeclaredMethod("interpolateOffset", AppBarLayout.class, int.class);
            m.setAccessible(true);
            return (int) m.invoke(this, layout, offset);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private void updateSpringOffsetByscroll(CoordinatorLayout coordinatorLayout, AppBarLayout appBarLayout, int offset) {
        if (mSpringRecoverAnimator != null && mSpringRecoverAnimator.isRunning())
            mSpringRecoverAnimator.cancel();
        updateSpringHeaderHeight(coordinatorLayout, appBarLayout, offset);
    }

    private void updateSpringHeaderHeight(CoordinatorLayout coordinatorLayout, AppBarLayout appBarLayout, int offset) {
        if (appBarLayout.getHeight() < mPreHeadHeight || offset < 0) return;
        mOffsetSpring = offset;
        if (mSpringOffsetCallback != null) mSpringOffsetCallback.springCallback(mOffsetSpring);
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
        layoutParams.height = mPreHeadHeight + offset;
        appBarLayout.setLayoutParams(layoutParams);
        coordinatorLayout.dispatchDependentViewsChanged(appBarLayout);
    }

    public int getOffsetSpring() {
        return mOffsetSpring;
    }

    public SpringOffsetCallback getSpringOffsetCallback() {
        return mSpringOffsetCallback;
    }

    public void setSpringOffsetCallback(SpringOffsetCallback springOffsetCallback) {
        mSpringOffsetCallback = springOffsetCallback;
    }

    private void updateAppBarLayoutDrawableState(final CoordinatorLayout parent,
                                                 final AppBarLayout layout, final int offset, final int direction,
                                                 final boolean forceJump) {
        try {
            Method m = getClass().getSuperclass().getDeclaredMethod("updateAppBarLayoutDrawableState", CoordinatorLayout.class, AppBarLayout.class, int.class, int.class, boolean.class);
            m.setAccessible(true);
            m.invoke(this, parent, layout, offset, direction, forceJump);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        }
        return value;
    }

    public interface SpringOffsetCallback {
        void springCallback(int offset);
    }
}