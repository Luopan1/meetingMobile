package com.hezy.guide.phone.business.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zy.guide.phone.R;
import com.hezy.guide.phone.entities.Meeting;

import java.util.ArrayList;


/**
 * Created by whatisjava on 17-10-18.
 */
public class MeetingAdapter extends OpenPresenter {

    private Context mContext;
    private ArrayList<Meeting> meetings;

    private GeneralAdapter mAdapter;
    private OnItemClickListener listener;

    public MeetingAdapter(Context context, ArrayList<Meeting> meetings, OnItemClickListener listener) {
        this.mContext = context;
        this.meetings = meetings;
        this.listener = listener;
    }

    @Override
    public void setAdapter(GeneralAdapter adapter) {
        this.mAdapter = adapter;
    }

    @Override
    public int getItemCount() {
        return meetings != null ? meetings.size() : 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_meeting, parent, false);
        return new MeetingHodler(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
        final Meeting meeting = meetings.get(position);
        MeetingHodler holder = (MeetingHodler) viewHolder;

        if (meeting.getMeetingProcess() == 1) {
            holder.stateText.setBackgroundResource(R.drawable.bg_meeting_state);
            holder.stateText.setText("进行中");
        } else {
            holder.stateText.setBackgroundResource(R.drawable.bg_meeting_state_a);
            holder.stateText.setText("未开始");
        }

        holder.titleText.setText(meeting.getTitle());

        holder.beginTimeText.setText("-----------" + meeting.getStartTime().substring(0, 16) + "-----------");

        if (position % 2==0){
            holder.bgLayout.setBackgroundResource(R.mipmap.bg_meeting_item_b);
        } else {
            holder.bgLayout.setBackgroundResource(R.mipmap.bg_meeting_item_a);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onItemClick(view, meeting);
                }
            }
        });

    }

    public interface OnItemClickListener{

        void onItemClick(View view, Meeting meeting);

    }

    private class MeetingHodler extends ViewHolder {

        View itemView;
        LinearLayout bgLayout;
        TextView titleText, beginTimeText, stateText;

        MeetingHodler(View itemView) {
            super(itemView);
            this.itemView = itemView;
            bgLayout = itemView.findViewById(R.id.background);
            titleText = itemView.findViewById(R.id.title);
            beginTimeText = itemView.findViewById(R.id.begin_time);
            stateText = itemView.findViewById(R.id.meeting_state);
        }
    }

}
