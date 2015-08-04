package com.cyanflxy.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.cyanflxy.annotation.API;

import java.util.ArrayList;
import java.util.List;

/**
 * 兼容性的ListView，补足系统ListView的问题。 - 实验性质，有待改进
 * <p/>
 * Created by CyanFlxy on 2015/7/20.
 */
public class ListViewCompat extends ListView {

    private List<View> headerList = new ArrayList<View>();
    private List<View> footerList = new ArrayList<View>();
    private ListAdapter mAdapter;
    private View emptyView;

    private OnItemClickListener onHeaderClickListener;
    private OnItemClickListener onContentClickListener;
    private OnItemClickListener onFooterClickListener;

    private OnItemLongClickListener onHeaderLongClickListener;
    private OnItemLongClickListener onContentLongClickListener;
    private OnItemLongClickListener onFooterLongClickListener;

    @API
    public ListViewCompat(Context context) {
        super(context);
        init();
    }

    @API
    public ListViewCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @API
    public ListViewCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        super.setAdapter(localAdapter);
        super.setOnItemClickListener(onItemClickListener);
        super.setOnItemLongClickListener(onItemLongClickListener);
    }

    @API
    @Override
    public void setAdapter(ListAdapter adapter) {
        mAdapter = adapter;
        localAdapter.notifyDataSetChanged();
    }

    @API
    @Override
    public ListAdapter getAdapter() {
        return mAdapter;
    }

    @API
    @Override
    public void setEmptyView(View emptyView) {
        this.emptyView = emptyView;
        if (emptyView != null) {
            ViewParent parent = emptyView.getParent();
            if (parent != null && parent instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) parent;
                group.removeView(emptyView);
            }
        }
    }

    @API
    @Override
    public View getEmptyView() {
        return emptyView;
    }

    @API
    public void addHeader(View view) {
        headerList.add(view);
        localAdapter.notifyDataSetChanged();
    }

    @API
    public void addHeader(int index, View view) {
        headerList.add(index, view);
        localAdapter.notifyDataSetChanged();
    }

    @Override
    public int getHeaderViewsCount() {
        return headerList.size();
    }

    @API
    public void addFooter(View view) {
        footerList.add(view);
        localAdapter.notifyDataSetChanged();
    }

    @API
    public void addFooter(int index, View view) {
        footerList.add(index, view);
        localAdapter.notifyDataSetChanged();
    }

    @Override
    public int getFooterViewsCount() {
        return footerList.size();
    }

    @API
    @Override
    public boolean removeHeaderView(View v) {
        if (headerList.contains(v)) {
            headerList.remove(v);
            localAdapter.notifyDataSetChanged();
            return true;
        } else {
            return false;
        }
    }

    @API
    public boolean removeHeaderView(int index) {
        if (headerList.size() > index) {
            headerList.remove(index);
            localAdapter.notifyDataSetChanged();
            return true;
        } else {
            return false;
        }
    }

    @API
    @Override
    public boolean removeFooterView(View v) {
        if (footerList.contains(v)) {
            footerList.remove(v);
            localAdapter.notifyDataSetChanged();
            return true;
        } else {
            return false;
        }
    }

    @API
    public boolean removeFooterView(int index) {
        if (footerList.size() > index) {
            footerList.remove(index);
            localAdapter.notifyDataSetChanged();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void setOnItemClickListener(OnItemClickListener listener) {
        onContentClickListener = listener;
    }

    @Override
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        onContentLongClickListener = listener;
    }

    @API
    public void setOnHeaderItemClickListener(OnItemClickListener listener) {
        onHeaderClickListener = listener;
    }

    @API
    public void setOnHeaderItemLongClickListener(OnItemLongClickListener listener) {
        onHeaderLongClickListener = listener;
    }

    @API
    public void setOnFooterItemClickListener(OnItemClickListener listener) {
        onFooterClickListener = listener;
    }

    @API
    public void setOnFooterItemLongClickListener(OnItemLongClickListener listener) {
        onFooterLongClickListener = listener;
    }

    @API
    public void setOnContentItemClickListener(OnItemClickListener listener) {
        onContentClickListener = listener;
    }

    @API
    public void setOnContentItemLongClickListener(OnItemLongClickListener listener) {
        onContentLongClickListener = listener;
    }

    private static final int POSITION_IN_HEADER = 0;
    private static final int POSITION_IN_CONTENT = 1;
    private static final int POSITION_IN_FOOTER = 2;

    private int[] transPosition(int position) {
        int[] p = new int[2];

        int headerSize = headerList.size();
        int contentSize = emptyViewAdapterWrapper.getCount();

        if (position < headerSize) {
            p[0] = POSITION_IN_HEADER;
            p[1] = position;
        } else if (position - headerSize < contentSize) {
            p[0] = POSITION_IN_CONTENT;
            p[1] = position - headerSize;
        } else {
            p[0] = POSITION_IN_FOOTER;
            p[1] = position - headerSize - contentSize;
        }
        return p;
    }

    private BaseAdapter localAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return headerList.size() + emptyViewAdapterWrapper.getCount() + footerList.size();
        }

        @Override
        public Object getItem(int position) {
            int[] p = transPosition(position);
            switch (p[0]) {
                case POSITION_IN_HEADER:
                    return headerList.get(p[1]);
                case POSITION_IN_CONTENT:
                    return emptyViewAdapterWrapper.getItem(p[1]);
                case POSITION_IN_FOOTER:
                    return footerList.get(p[1]);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            int[] p = transPosition(position);
            switch (p[0]) {
                case POSITION_IN_HEADER:
                    return headerList.get(p[1]);
                case POSITION_IN_CONTENT:
                    return emptyViewAdapterWrapper.getView(position, convertView, parent);
                case POSITION_IN_FOOTER:
                    return footerList.get(p[1]);
            }
            return null;
        }
    };

    private BaseAdapter emptyViewAdapterWrapper = new BaseAdapter() {
        @Override
        public int getCount() {
            if (isContentAdapterEmpty()) {
                return emptyView == null ? 0 : 1;
            }
            return mAdapter.getCount();
        }

        @Override
        public Object getItem(int position) {
            if (isContentAdapterEmpty()) {
                return emptyView;
            } else {
                return mAdapter.getItem(position);
            }
        }

        @Override
        public long getItemId(int position) {
            if (isContentAdapterEmpty()) {
                return 0;
            } else {
                return mAdapter.getItemId(position);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (isContentAdapterEmpty()) {
                return emptyView;
            } else {
                return mAdapter.getView(position, convertView, parent);
            }
        }

        private boolean isContentAdapterEmpty() {
            return mAdapter == null || mAdapter.getCount() == 0;
        }
    };

    private OnItemClickListener onItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            int[] p = transPosition(position);
            switch (p[0]) {
                case POSITION_IN_HEADER:
                    if (onHeaderClickListener != null) {
                        onHeaderClickListener.onItemClick(parent, view, p[1], id);
                    }
                    break;
                case POSITION_IN_CONTENT:
                    if (onContentClickListener != null) {
                        onContentClickListener.onItemClick(parent, view, p[1], id);
                    }
                    break;
                case POSITION_IN_FOOTER:
                    if (onFooterClickListener != null) {
                        onFooterClickListener.onItemClick(parent, view, p[1], id);
                    }
                    break;
            }
        }
    };

    private OnItemLongClickListener onItemLongClickListener = new OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            int[] p = transPosition(position);
            switch (p[0]) {
                case POSITION_IN_HEADER:
                    if (onHeaderLongClickListener != null) {
                        onHeaderLongClickListener.onItemLongClick(parent, view, p[1], id);
                    }
                    break;
                case POSITION_IN_CONTENT:
                    if (onContentLongClickListener != null) {
                        onContentLongClickListener.onItemLongClick(parent, view, p[1], id);
                    }
                    break;
                case POSITION_IN_FOOTER:
                    if (onFooterLongClickListener != null) {
                        onFooterLongClickListener.onItemLongClick(parent, view, p[1], id);
                    }
                    break;
            }
            return false;
        }
    };
}
