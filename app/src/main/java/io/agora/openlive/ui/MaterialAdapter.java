package io.agora.openlive.ui;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.hezy.guide.phone.entities.Material;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.utils.helper.ImageHelper;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by whatisjava on 17-10-18.
 */

public class MaterialAdapter extends RecyclerView.Adapter<MaterialAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<Material> materials;
    private OnClickListener onClickListener;

    public MaterialAdapter(Context context, ArrayList<Material> materials) {
        this.mContext = context;
        this.materials = materials;
    }

    public void addData(ArrayList<Material> materials) {
        this.materials.addAll(materials);
        notifyItemRangeInserted(this.materials.size(), materials.size());
    }

    public void cleanData() {
        this.materials.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_material, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        Material material = materials.get(position);

        viewHolder.nameText.setText(material.getName());
        String imageUrl = ImageHelper.getThumb(material.getMeetingMaterialsPublishList().get(0).getUrl());
        Picasso.with(mContext).load(imageUrl).into(viewHolder.imageView);
        viewHolder.countText.setText(String.format("%d张", material.getMeetingMaterialsPublishList().size()));
        viewHolder.uploadTimeText.setText(String.format("%s上传", material.getCreateDate()));

        if (onClickListener != null) {
            viewHolder.itemView.setOnClickListener(v -> {
                int layoutPos = viewHolder.getLayoutPosition();
                onClickListener.onPreviewButtonClick(v, material,  layoutPos);
            });
        }

    }

    @Override
    public int getItemCount() {
        return materials != null ? materials.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        View itemView;
        ImageView imageView;
        TextView nameText, countText, uploadTimeText;
        FrameLayout coverLayout;

        public ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            coverLayout = itemView.findViewById(R.id.cover_layout);
            imageView = itemView.findViewById(R.id.image);
            nameText = itemView.findViewById(R.id.name);
            countText = itemView.findViewById(R.id.count);
            uploadTimeText = itemView.findViewById(R.id.upload_time);
        }
    }

    public interface OnClickListener {

        void onPreviewButtonClick(View v, Material material, int position);

    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

}
