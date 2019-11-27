package com.zhongyou.meet.mobile.ameeting.meetmodule.di.module;

import com.jess.arms.di.scope.ActivityScope;

import dagger.Module;
import dagger.Provides;

import com.zhongyou.meet.mobile.ameeting.meetmodule.mvp.contract.MeetChairManActivityContract;
import com.zhongyou.meet.mobile.ameeting.meetmodule.mvp.model.MeetChairManActivityModel;


@Module
public class MeetChairManActivityModule {
    private MeetChairManActivityContract.View view;

    /**
     * 构建MeetChairManActivityModule时,将View的实现类传进来,这样就可以提供View的实现类给presenter
     * @param view
     */
    public MeetChairManActivityModule(MeetChairManActivityContract.View view) {
        this.view = view;
    }

    @ActivityScope
    @Provides
    MeetChairManActivityContract.View provideMeetChairManActivityView(){
        return this.view;
    }

    @ActivityScope
    @Provides
    MeetChairManActivityContract.Model provideMeetChairManActivityModel(MeetChairManActivityModel model){
        return model;
    }
}