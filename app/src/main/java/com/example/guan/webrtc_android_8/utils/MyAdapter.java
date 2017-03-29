package com.example.guan.webrtc_android_8.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.guan.webrtc_android_8.R;

import java.util.List;

/**
 * Created by guan on 3/29/17.
 */

public class MyAdapter extends BaseAdapter {
    Context mContext;
    List<Object> data;
    private LayoutInflater lf;


    public MyAdapter(Context mContext, List<Object> data) {
        this.mContext = mContext;
        this.data = data;
        this.lf = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        //return 0;
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        //return null;
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        //return 0;
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        //这里很重要，设置Tag可以实现重用
        ViewHolder_BrowseFileItem holder = null;
        if (convertView == null) {
            convertView = lf.inflate(R.layout.view_fileitem, null);
            holder = new ViewHolder_BrowseFileItem();
            holder.name = (TextView) convertView.findViewById(R.id.file_name);
            holder.icon = (ImageView) convertView.findViewById(R.id.file_icon);
            holder.item_cb = (CheckBox) convertView.findViewById(R.id.item_cb);

            //给View对象的一个标签，标签可以是任何内容
            //标签就是ViewHolder实例化后对象的一个属性
            convertView.setTag(holder);

            holder.delete_btn.setTag(position);//把position当作Tag,设置到Button上

        } else {
            holder = (ViewHolder_BrowseFileItem) convertView.getTag();
        }


        //=====对holder里面的元素进行自定义设置=====
        holder.name.setText("balabala");
        holder.item_cb.setVisibility(View.VISIBLE);
        holder.item_cb.setChecked(true);
        holder.delete_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int detail_btn_position = (int) v.getTag();
                //删除这条数据
                data.remove(detail_btn_position);
                //更新显示
                MyAdapter.this.notifyDataSetChanged();
            }
        });

        //……
        //返回的就是要显示的item
        return convertView;
    }

    /**
     * 头部添加一条新数据，并更新显示
     * @param object
     */
    public void addItem(Object object) {
        data.add(0,object);
        this.notifyDataSetChanged();
    }

    /**
     * 更换整个数据集，并更新显示
     * @param data
     */
    public void changeDataSet(List<Object> data) {
        this.data = data;
        this.notifyDataSetChanged();
    }


    private class ViewHolder_BrowseFileItem {
        public TextView name;
        public ImageView icon;
        public CheckBox item_cb;
        public Button delete_btn;
    }

}
