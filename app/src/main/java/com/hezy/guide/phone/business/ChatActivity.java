package com.hezy.guide.phone.business;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import com.hezy.guide.phone.R;

public class ChatActivity extends BasicActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_chat);
        initFragment();
    }

    private void initFragment(){
        ChatFragment fragment = ChatFragment.newInstance();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.rl_content, fragment).show(fragment);
        fragmentTransaction.commitAllowingStateLoss();
    }
    @Override
    public String getStatisticsTag() {
        return "聊天室";
    }
}
