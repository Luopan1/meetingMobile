package com.hezy.guide.phone.business.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.entities.ChatMesData;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.helper.ImageHelper;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class chatAdapter extends RecyclerView.Adapter<chatAdapter.ViewHolder> {

    private Context context;
    private List<ChatMesData.PageDataEntity> data;
    private onClickCallBack callBack;

    public chatAdapter(Context context, List<ChatMesData.PageDataEntity> data, onClickCallBack tempCallBack){
        this.context = context;
        this.data = data;
        this.callBack = tempCallBack;

    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == 0){
            View view = LayoutInflater.from(context).inflate(R.layout.item_left,parent,false);
            return new ViewHolder(view);
        }else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_right,parent,false);
            return new ViewHolder(view);
        }

    }


    @Override
    public int getItemViewType(int position) {
        if(!data.get(position).getUserName().equals(Preferences.getUserName())){
            return 0;
        }else {
            return 1;
        }
//        return super.getItemViewType(position);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Log.e("这里是点击每一行item的响应事件",""+position+item);
                callBack.onClickCallBackFuc();
            }
        });

        ((TextView)holder.name).setText(data.get(position).getUserName());
        ((TextView)holder.tvContent).setText(data.get(position).getContent());
        ImageHelper.loadImageDpId(data.get(position).getUserLogo(), R.dimen.my_px_66, R.dimen.my_px_66, holder.imgHead);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private TextView name;
        private TextView tvContent;
        private CircleImageView imgHead;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_name);
            tvContent = itemView.findViewById(R.id.tv_content);
            imgHead = (CircleImageView)itemView.findViewById(R.id.mIvHead);

        }
    }

    public interface onClickCallBack{
        void onClickCallBackFuc();
    }
}
