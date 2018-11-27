package io.agora.openlive.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.entities.ChatMesData;

import java.util.List;

public class InMeetingAdapter extends RecyclerView.Adapter<InMeetingAdapter.ViewHolder> {

    private Context context;
    private List<ChatMesData.PageDataEntity> data;
    private onItemClickInt itemClick;

    public InMeetingAdapter(Context context, List<ChatMesData.PageDataEntity> data, onItemClickInt temItemClick){
        this.context = context;
        this.data = data;
        this.itemClick = temItemClick;


    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_inmeeting_chat,parent,false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {

        holder.tvContent.setText(":"+data.get(position).getContent());
        holder.tvName.setText(data.get(position).getUserName());
        holder.tvAddress.setText(data.get(position).getUserName());
        if(data.get(position).getLocalState()==0){
            holder.sendBar.setVisibility(View.GONE);
            holder.tvState.setVisibility(View.GONE);
        }else if(data.get(position).getLocalState() == 1){
            holder.sendBar.setVisibility(View.VISIBLE);
            holder.tvState.setVisibility(View.GONE);
        }else if(data.get(position).getLocalState() == 2){
            holder.sendBar.setVisibility(View.GONE);
            holder.tvState.setVisibility(View.VISIBLE);
            holder.tvState.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    callBack.onReSend(data.get(position).getContent(),data.get(position).getType());
                }
            });
            holder.tvState.setText("失败");
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Log.e("这里是点击每一行item的响应事件",""+position+item);
                itemClick.onItemClick(position);
            }
        });

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private TextView tvName, tvAddress, tvContent, tvState;
        private ProgressBar sendBar;

        public ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvAddress = itemView.findViewById(R.id.tv_addres);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvState = itemView.findViewById(R.id.send_sate);
            sendBar = itemView.findViewById(R.id.send_bar);

        }
    }

    public interface  onItemClickInt{
        void onItemClick(int pos);
    }
}


