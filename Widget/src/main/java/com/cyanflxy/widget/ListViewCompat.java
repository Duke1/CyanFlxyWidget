package com.cyanflxy.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * 兼容性的ListView，补足系统ListView的问题。
 * <p/>
 * Created by CyanFlxy on 2015/7/20.
 */
public class ListViewCompat extends ListView {

    public ListViewCompat(Context context) {
        super(context);
    }

    public ListViewCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ListViewCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

}
