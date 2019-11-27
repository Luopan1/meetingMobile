package com.zhongyou.meet.mobile.ameeting.meetmodule.di.component;

import dagger.Component;
import com.jess.arms.di.component.AppComponent;

import com.zhongyou.meet.mobile.ameeting.meetmodule.di.module.NetworkModule;
import com.zhongyou.meet.mobile.ameeting.meetmodule.di.module.MeetChairManActivityModule;

import com.jess.arms.di.scope.ActivityScope;
import com.zhongyou.meet.mobile.ameeting.meetmodule.mvp.ui.activity.MeetChairManActivityActivity;

@ActivityScope
@Component(modules = {MeetChairManActivityModule.class, NetworkModule.class},dependencies = AppComponent.class)
public interface MeetChairManActivityComponent {
    void inject(MeetChairManActivityActivity activity);
}