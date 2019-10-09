package com.zy.guide.phone.event;

import com.zy.guide.phone.entities.RecordData;

/**
 * Created by wufan on 2017/8/17.
 */

public class ReplyReviewEvent {
    RecordData.PageDataEntity bean;

    public ReplyReviewEvent(RecordData.PageDataEntity bean) {
        this.bean = bean;
    }

    public RecordData.PageDataEntity getBean() {
        return bean;
    }

    public void setBean(RecordData.PageDataEntity bean) {
        this.bean = bean;
    }
}
