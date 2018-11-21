package com.hezy.guide.phone.business.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.hezy.family.photolib.Info;
import com.hezy.family.photolib.PhotoView;
import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ImageBrowerAdapter extends RecyclerView.Adapter<ImageBrowerAdapter.ViewHolder> {

    private Context context;
    private List<String> data;
    private Info info;

    public ImageBrowerAdapter(Context context, List<String> data, Info tempInfo){
        this.context = context;
        this.data = data;
        this.info = tempInfo;

    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_img_brower,parent,false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {

        Picasso.with(BaseApplication.getInstance()).load(data.get(position)).into(holder.img);
        if(position==0){
            holder.img.animaFrom(info);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Log.e("这里是点击每一行item的响应事件",""+position+item);
            }
        });

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private PhotoView img;

        public ViewHolder(View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img);

        }
    }
}


