package com.hezy.guide.phone.entities;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class ChatMesData {

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

    public static class PageDataEntity implements Parcelable {
        private String userName;
        private String replyTime;
        private String content;
        private int type;
        private String userLogo;

        protected PageDataEntity(Parcel in) {
            userName = in.readString();
            replyTime = in.readString();
            content = in.readString();
            type = in.readInt();
            userLogo = in.readString();
        }

        public static final Creator<PageDataEntity> CREATOR = new Creator<PageDataEntity>() {
            @Override
            public PageDataEntity createFromParcel(Parcel in) {
                return new PageDataEntity(in);
            }

            @Override
            public PageDataEntity[] newArray(int size) {
                return new PageDataEntity[size];
            }
        };

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getReplyTime() {
            return replyTime;
        }

        public void setReplyTime(String replyTime) {
            this.replyTime = replyTime;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getUserLogo() {
            return userLogo;
        }

        public void setUserLogo(String userLogo) {
            this.userLogo = userLogo;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(userName);
            parcel.writeString(replyTime);
            parcel.writeString(content);
            parcel.writeInt(type);
            parcel.writeString(userLogo);
        }
    }
}
