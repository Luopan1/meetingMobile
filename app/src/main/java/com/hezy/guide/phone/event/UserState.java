package com.hezy.guide.phone.event;

/**用户在线状态改变
 * Created by wufan on 2017/7/27.
 */

public class UserState {
    int state;

    public UserState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }


}
