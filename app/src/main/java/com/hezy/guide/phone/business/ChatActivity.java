package com.hezy.guide.phone.business;

import android.os.Bundle;

public class ChatActivity extends BasicActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public String getStatisticsTag() {
        return "聊天室";
    }
}
