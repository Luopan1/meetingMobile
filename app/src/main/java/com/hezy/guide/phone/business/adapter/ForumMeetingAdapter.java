package com.hezy.guide.phone.business.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.entities.ForumMeeting;

import java.util.ArrayList;

/**
 * 讨论区列表adapter
 *
 * @author Dongce
 * create time: 2018/11/14
 */
public class ForumMeetingAdapter extends OpenPresenter {

    private Context mContext;
    private ArrayList<ForumMeeting> forumMeetings;
    private GeneralAdapter mAdapter;
    private OnItemClickListener listener;

    public ForumMeetingAdapter(Context context, ArrayList<ForumMeeting> forumMeetings, OnItemClickListener listener) {
        this.mContext = context;
        this.forumMeetings = forumMeetings;
        this.listener = listener;
    }

    @Override
    public void setAdapter(GeneralAdapter adapter) {
        this.mAdapter = adapter;
    }

    @Override
    public int getItemCount() {
        return forumMeetings != null ? forumMeetings.size() : 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_forum_meeting, parent, false);
        return new ForumMeetingHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {

        final ForumMeeting forumMeeting = forumMeetings.get(position);
        ForumMeetingHolder holder = (ForumMeetingHolder) viewHolder;

        //图片背景及文字显示
        if (forumMeeting.getMeetingType() == ForumMeeting.TYPE_MEETING_PUBLIC) {
            holder.img_forum_meeting_item_head.setBackground(ContextCompat.getDrawable(mContext, R.drawable.bg_forum_meeting_public));
            holder.tv_forum_meeting_item_head.setText(mContext.getResources().getText(R.string.forum_meeting_public));
        } else if (forumMeeting.getMeetingType() == ForumMeeting.TYPE_MEETING_PRIVATE) {
            holder.img_forum_meeting_item_head.setBackground(ContextCompat.getDrawable(mContext, R.drawable.bg_forum_meeting_private));
            holder.tv_forum_meeting_item_head.setText(mContext.getResources().getText(R.string.forum_meeting_private));
        }

        //会议title
        holder.tv_forum_meeting_item_title.setText(forumMeeting.getTitle());
        //未读消息
        holder.tv_forum_meeting_item_unread.setText(forumMeeting.getNewMsgCnt() + "条新消息");
        //最后消息时间
        holder.tv_forum_meeting_item_msg_lasttime.setText(forumMeeting.getNewMsgReplyTime());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onItemClick(v, forumMeeting);
                }
            }
        });
    }

    public interface OnItemClickListener {
        void onItemClick(View view, ForumMeeting forumMeeting);
    }

    private class ForumMeetingHolder extends ViewHolder {
        View itemView;
        ImageView img_forum_meeting_item_head;
        TextView tv_forum_meeting_item_head, tv_forum_meeting_item_title, tv_forum_meeting_item_unread, tv_forum_meeting_item_at, tv_forum_meeting_item_msg_lasttime;

        ForumMeetingHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            img_forum_meeting_item_head = itemView.findViewById(R.id.img_forum_meeting_item_head);
            tv_forum_meeting_item_head = itemView.findViewById(R.id.tv_forum_meeting_item_head);
            tv_forum_meeting_item_title = itemView.findViewById(R.id.tv_forum_meeting_item_title);
            tv_forum_meeting_item_unread = itemView.findViewById(R.id.tv_forum_meeting_item_unread);
            tv_forum_meeting_item_at = itemView.findViewById(R.id.tv_forum_meeting_item_at);
            tv_forum_meeting_item_msg_lasttime = itemView.findViewById(R.id.tv_forum_meeting_item_msg_lasttime);
        }
    }
}
