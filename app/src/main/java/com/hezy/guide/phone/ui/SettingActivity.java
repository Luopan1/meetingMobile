package com.hezy.guide.phone.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BasisActivity;

import io.agora.openlive.model.ConstantApp;

/**
 * Created by whatisjava on 17-9-5.
 */

public class SettingActivity extends BasisActivity {

    @Override
    public String getStatisticsTag() {
        return "设置";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        findViewById(R.id.resolution_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSelectDialog();
            }
        });

    }

    String[] items = new String[]{"流畅（网速慢时，小屏幕让你不卡顿）", "标准（大屏显示的清晰画质）", "高清（网速好时，可以选择高清画质）"};

    private void showSelectDialog() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int prefIndex = pref.getInt(ConstantApp.PrefManager.PREF_PROPERTY_PROFILE_IDX, ConstantApp.DEFAULT_PROFILE_IDX);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("分辨率设置");
        builder.setSingleChoiceItems(items, prefIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor editor = pref.edit();
                editor.putInt(ConstantApp.PrefManager.PREF_PROPERTY_PROFILE_IDX, which);
                editor.apply();
                dialog.dismiss();
                ((TextView)findViewById(R.id.resolution_text)).setText(resolutionText(which));
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private String resolutionText(int which) {
        switch (which) {
            case 0:
                return "流畅";
            case 1:
                return "标准（推荐）";
            case 2:
                return "高清";
            default:
                return "标准（推荐）";
        }
    }

}
