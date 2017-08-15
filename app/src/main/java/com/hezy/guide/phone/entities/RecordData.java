package com.hezy.guide.phone.entities;

import android.text.TextUtils;

import java.util.List;

/**
 * Created by wufan on 2017/8/2.
 */

public class RecordData {

    /**
     * pageNo : 1
     * totalPage : 1
     * pageSize : 20
     * pageData : [{"callAnswerTime":"17:58","minuteInterval":20,"address":"中关村店","callEndTime":"2017-07-27 18:18:58.0","callStartTime":"2017-07-27 17:58:08.0","name":"啦啦啦123","mobile":"138****3620","id":"0d1ac3895f4b41eb8dfdb2da55d02891","status":1}]
     * totalCount : 13
     */

    private int pageNo;
    private int totalPage;
    private int pageSize;
    private int totalCount;
    private List<PageDataEntity> pageData;

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<PageDataEntity> getPageData() {
        return pageData;
    }

    public void setPageData(List<PageDataEntity> pageData) {
        this.pageData = pageData;
    }

    public static class PageDataEntity {
        /**
         * callAnswerTime : 17:58
         * minuteInterval : 20
         * address : 中关村店
         * callEndTime : 2017-07-27 18:18:58.0
         * callStartTime : 2017-07-27 17:58:08.0
         * name : 啦啦啦123
         * mobile : 138****3620
         * id : 0d1ac3895f4b41eb8dfdb2da55d02891
         * status : 1
         */

        private String callAnswerTime;
        private int secondInterval;
        private int minuteInterval;
        private String address;
        private String callEndTime;
        private String callStartTime;
        private String name;
        private String mobile;
        private String id;
        //status (integer, optional): 状态， 0：未应答（30s未接听），1：接听，2：拒绝, 8：对方挂断 9：通话结束挂断 ,
        private int status;
        private String photo;
        private int ratingStar;

        public String getCallAnswerTime() {
            return callAnswerTime;
        }

        public void setCallAnswerTime(String callAnswerTime) {
            this.callAnswerTime = callAnswerTime;
        }

        public int getMinuteInterval() {
            return minuteInterval;
        }

        public void setMinuteInterval(int minuteInterval) {
            this.minuteInterval = minuteInterval;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getCallEndTime() {
            return callEndTime;
        }

        public void setCallEndTime(String callEndTime) {
            this.callEndTime = callEndTime;
        }

        public String getCallStartTime() {
            return callStartTime;
        }

        public void setCallStartTime(String callStartTime) {
            this.callStartTime = callStartTime;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public int getSecondInterval() {
            return secondInterval;
        }

        public void setSecondInterval(int secondInterval) {
            this.secondInterval = secondInterval;
        }

        public String getPhoto() {
            return photo;
        }

        public void setPhoto(String photo) {
            this.photo = photo;
        }

        public int getRatingStar() {
            return ratingStar;
        }

        public void setRatingStar(int ratingStar) {
            this.ratingStar = ratingStar;
        }

        public  String getOrderTime(){
            if(!TextUtils.isEmpty(callAnswerTime)){
                return callAnswerTime;
            }else{
                return callStartTime;
            }
        }

    }
}
