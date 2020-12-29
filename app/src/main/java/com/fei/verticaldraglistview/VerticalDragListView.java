package com.fei.verticaldraglistview;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ListViewCompat;
import androidx.customview.widget.ViewDragHelper;

/**
 * @ClassName: VerticalDragListView
 * @Description: 描述
 * @Author: Fei
 * @CreateDate: 2020/12/29 9:26
 * @UpdateUser: Fei
 * @UpdateDate: 2020/12/29 9:26
 * @UpdateRemark: 更新说明
 * @Version: 1.0
 */
public class VerticalDragListView extends FrameLayout {

    private final String TAG = "VerticalDragListView";
    private ViewDragHelper mViewDragHelper;//拖动工具
    private View mListView;//可拖动View
    private int mDragHeight;//可拖动高度

    public VerticalDragListView(@NonNull Context context) {
        this(context, null);
    }

    public VerticalDragListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalDragListView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mViewDragHelper = ViewDragHelper.create(this, mCallBack);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        int childCount = getChildCount();
        if (childCount != 2) {
            throw new RuntimeException("VerticalDragListView 需要包含两个控件");
        }

    }

    /**
     * 测量之后，获取子View高度
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            mDragHeight = getChildAt(0).getMeasuredHeight();
            mListView = getChildAt(1);
        }
    }

    /**
     * 拦截，当listview在最顶时，且往下滚动就拦截
     */
    private float mDownY;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = ev.getY();
                //mViewDragHelper需要完整的事件，因为Down时被子View拦截了，走不到onTouchEvent，获取不到Down事件
                //所以需要这里赋值
                mViewDragHelper.processTouchEvent(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                float moveY = ev.getY();
                if (moveY - mDownY > 0 && !canChildScrollUp()) {
                    //向下拉,且子View已经到达顶部
                    return true;//拦截
                } else if (moveY - mDownY < 0 && mListView.getTop() > 0) {
                    //如果子View不在顶部且向上拉，说明打开状态，就响应mViewDragHelper
                    return true;
                }
                break;
        }

        return super.onInterceptTouchEvent(ev);
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        if (mListView instanceof ListView) {
            return ListViewCompat.canScrollList((ListView) mListView, -1);
        }
        return mListView.canScrollVertically(-1);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mViewDragHelper.processTouchEvent(event);
        return true;//必须return true，使得父容器下次move，up在不拦截的情况下进来自己的dispatchTouch，才能到onTouchEvent
    }

    private ViewDragHelper.Callback mCallBack = new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            Log.e(TAG, "child->" + child.toString() + " mListView->" + mListView.toString());
            //只有mListView可拖动
            return child == mListView;
        }

        @Override
        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            //可垂直拖动

            //控制拖动范围
            if (top <= 0) {
                top = 0;
            } else if (top > mDragHeight) {
                top = mDragHeight;
            }

            return top;
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            //手指释放后回弹到指定位置
            int halfHeight = mDragHeight / 2;
            if (releasedChild.getTop() > halfHeight) {
                mViewDragHelper.settleCapturedViewAt(0, mDragHeight);
            } else {
                mViewDragHelper.settleCapturedViewAt(0, 0);
            }
            invalidate();
        }
    };

    @Override
    public void computeScroll() {
        super.computeScroll();
        //使用continueSettling(true)判断拖拽是否完成
        if (mViewDragHelper.continueSettling(true)) {
            invalidate();
        }
    }
}
