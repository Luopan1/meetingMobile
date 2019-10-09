package com.zy.guide.phone.event;

import com.zy.guide.phone.entities.ChatMesData;

public class ForumSendEvent {
    public ChatMesData.PageDataEntity getEntity() {
        return entity;
    }

    public void setEntity(ChatMesData.PageDataEntity entity) {
        this.entity = entity;
    }

    private ChatMesData.PageDataEntity entity;

}
