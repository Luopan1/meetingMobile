package com.hezy.guide.phone.event;

import com.hezy.guide.phone.entities.ChatMesData;

public class ForumRevokeEvent {
    public ChatMesData.PageDataEntity getEntity() {
        return entity;
    }

    public void setEntity(ChatMesData.PageDataEntity entity) {
        this.entity = entity;
    }

    private ChatMesData.PageDataEntity entity;
}
