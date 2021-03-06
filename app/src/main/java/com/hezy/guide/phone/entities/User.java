package com.hezy.guide.phone.entities;

/**
 * Created by wufan on 2017/7/20.
 */

public class User {


    /**
     * id : e414d731b8284431a0692e39dd11c95d
     * createDate : 2017-07-21 10:22:12
     * updateDate : 2017-07-21 10:22:12
     * delFlag : 0
     * name :
     * mobile :
     * address :
     * photo :
     * signature :
     * token : 1e15e141ed5c4b4c944cf3fef52b9ecd
     * type : 2
     * registerIp : 111.198.24.240
     * lastLoginIp : 111.198.24.240
     * lastLoginTime : 2017-07-21 10:22:12
     */

    private String id;
    private String createDate;
    private String updateDate;
    private String delFlag;
    private String name;
    private String mobile;
    private String address;
    private String photo;
    private String signature;
    private String token;
    private int type;
    private String registerIp;
    private String lastLoginIp;
    private String lastLoginTime;
    private int rank;

    private String areaId;
    private String areaInfo;
    private String areaName;
    private String customId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getRegisterIp() {
        return registerIp;
    }

    public void setRegisterIp(String registerIp) {
        this.registerIp = registerIp;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    public String getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(String lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public String getAreaInfo() {
        return areaInfo;
    }

    public void setAreaInfo(String areaInfo) {
        this.areaInfo = areaInfo;
    }

    public String getCustomId() {
        return customId;
    }

    public void setCustomId(String customId) {
        this.customId = customId;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", createDate='" + createDate + '\'' +
                ", updateDate='" + updateDate + '\'' +
                ", delFlag='" + delFlag + '\'' +
                ", name='" + name + '\'' +
                ", mobile='" + mobile + '\'' +
                ", address='" + address + '\'' +
                ", photo='" + photo + '\'' +
                ", signature='" + signature + '\'' +
                ", token='" + token + '\'' +
                ", type=" + type +
                ", registerIp='" + registerIp + '\'' +
                ", lastLoginIp='" + lastLoginIp + '\'' +
                ", lastLoginTime='" + lastLoginTime + '\'' +
                ", rank=" + rank +
                ", areaId='" + areaId + '\'' +
                ", areaInfo='" + areaInfo + '\'' +
                ", areaName='" + areaName + '\'' +
                ", customId='" + customId + '\'' +
                '}';
    }
}
