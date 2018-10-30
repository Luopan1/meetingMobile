package io.agora.openlive.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.entities.Audience;

import java.util.ArrayList;


/**
 * Created by whatisjava on 17-11-22.
 */

public class AudienceAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater inflater;
    private ArrayList<Audience> audiences;

    public AudienceAdapter(Context context, ArrayList<Audience> audiences) {
        this.context = context;
        this.audiences = audiences;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return audiences != null ? audiences.size() : 0;
    }

    @Override
    public Object getItem(int i) {
        return audiences != null ? audiences.get(i) : null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_audience, null);
            viewHolder = new ViewHolder();
            viewHolder.audienceLayout = convertView.findViewById(R.id.stop_audience);
            viewHolder.nameText = convertView.findViewById(R.id.audience_name);
            viewHolder.talkButton = convertView.findViewById(R.id.talk);
            viewHolder.checkButton = convertView.findViewById(R.id.check);
            viewHolder.handsupImage = convertView.findViewById(R.id.handsup);
            viewHolder.callingText = convertView.findViewById(R.id.calling);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Audience audience = audiences.get(position);
        viewHolder.nameText.setText((position + 1) + " " + audience.getUname());

        if (audience.isHandsUp()) {
            viewHolder.handsupImage.setVisibility(View.VISIBLE);
        } else {
            viewHolder.handsupImage.setVisibility(View.GONE);
        }

        if (audience.isCalling()) {
            viewHolder.callingText.setVisibility(View.VISIBLE);
            viewHolder.talkButton.setVisibility(View.GONE);
        } else {
            viewHolder.callingText.setVisibility(View.GONE);
            viewHolder.talkButton.setVisibility(View.VISIBLE);
        }
//        viewHolder.audienceLayout.setOnFocusChangeListener((view, b) -> {
//            if (b) {
//                viewHolder.talkButton.setVisibility(View.VISIBLE);
//                viewHolder.checkButton.setVisibility(View.VISIBLE);
//            } else {
//                viewHolder.talkButton.setVisibility(View.GONE);
//                viewHolder.checkButton.setVisibility(View.GONE);
//            }
//        });

        return convertView;
    }

    class ViewHolder {
        LinearLayout audienceLayout;
        ImageView handsupImage;
        TextView nameText, callingText;
        Button talkButton, checkButton;
    }

    public void setData(ArrayList<Audience> audiences){
        this.audiences = audiences;
        notifyDataSetChanged();
    }

}
