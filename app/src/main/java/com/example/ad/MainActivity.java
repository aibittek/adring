package com.example.ad;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ad.utils.AudioTrackManager;
import com.example.ad.utils.PermissionsUtils;
import com.example.ad.utils.VoiceMixerUtil;
import com.example.ad.utils.WebTTSWS;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;
import org.w3c.dom.Text;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;
import info.hoang8f.widget.FButton;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private FButton btnTTS = null;
    private EditText editView = null;
    private DiscreteSeekBar volumeSeekBar = null;
    private DiscreteSeekBar speedSeekBar = null;
    private TextView volumeTextView = null;
    private TextView speedTextView = null;
    private EditText editText = null;
    private CircleImageView[] vcn = new CircleImageView[4];
    private TextView[] vcnName = new TextView[4];
    private int mCurVcn = 0;
    private int mCurSpeed = 50;
    private int mCurVolume = 50;
    private String vcnString = "xiaoyan";
    private String[] vcnText = {
            "正在为您查询合肥的天气情况。今天是2020年1月1日，合肥市今天多云，最低温度9摄氏度，最高温度15摄氏度，微风。",
            "80年代中后期的流行语。越活越漂亮，越活越年轻，有谁不喜欢。在进口品牌的强大冲击下，国产制皂工业奋发图强，创造新概念的美容香皂。据回忆，该广告语是一群本地创意人拍脑袋的杰作，没有什么国际广告公司那一套：市场研究，消费调查，产品定位和广告策略等等，效果却非常好。难怪许多资深广告人常常说：广告没真理。",
            "白皮书说，党的十八大以来，中国的核安全事业进入安全高效发展的新时期。在核安全观引领下，中国逐步构建起法律规范、行政监管、行业自律、技术保障、人才支撑、文化引领、社会参与、国际合作等为主体的核安全治理体系，核安全防线更加牢固。",
            "您好，我是客户回访专员，为了更好的爱护您的车辆，请您在一到两个月内或3000公里左右来店进行车辆首保。首保是免费的，我们的服务顾问和维修技师会为您的车做一次全面的检查。"
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad);

        vcn[0] = findViewById(R.id.vcn1);
        vcn[1] = findViewById(R.id.vcn2);
        vcn[2] = findViewById(R.id.vcn3);
        vcn[3] = findViewById(R.id.vcn4);
        vcn[0].setOnClickListener(this);
        vcn[1].setOnClickListener(this);
        vcn[2].setOnClickListener(this);
        vcn[3].setOnClickListener(this);
        editView = findViewById(R.id.tts_text);
        btnTTS = findViewById(R.id.btn_tts);
        btnTTS.setOnClickListener(this);
        vcnName[0] = findViewById(R.id.vcn_name1);
        vcnName[1] = findViewById(R.id.vcn_name2);
        vcnName[2] = findViewById(R.id.vcn_name3);
        vcnName[3] = findViewById(R.id.vcn_name4);
        editText = findViewById(R.id.tts_text);
        editText.setText(vcnText[0]);
        volumeTextView = findViewById(R.id.volume_textview);
        volumeSeekBar = findViewById(R.id.volume_seekbar);
        speedTextView = findViewById(R.id.speed_textview);
        speedSeekBar = findViewById(R.id.speed_seekbar);
        setVcnLight(0, 0xFF4CAF50);
        SetVcnNameColor(0, 0xFF4CAF50);
        speedSeekBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
            }
            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                mCurSpeed = speedSeekBar.getProgress();
                if (mCurSpeed > 70) {
                    speedTextView.setText("快速");
                } else if (mCurSpeed < 30) {
                    speedTextView.setText("慢速");
                } else {
                    speedTextView.setText("正常");
                }
            }
        });
        volumeSeekBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener(){
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
            }
            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                mCurVolume = volumeSeekBar.getProgress();
                volumeTextView.setText(String.valueOf(mCurVolume));
            }
        });

        // 1.自动权限获取
        String[] permissions = {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.INTERNET,
                };
        PermissionsUtils.getInstance().checkPermissions(this, permissions, permissionsResult);
    }

    WebTTSWS.IResponseResult responseResult = new WebTTSWS.IResponseResult() {
        @Override
        public void setAudio(byte[] audio) {
            byte[] bgpcm = null;
            try {
                InputStream in = getResources().openRawResource(R.raw.bg1);
                //获取文件的字节数
                int lenght = in.available();
                //创建byte数组
                bgpcm = new byte[lenght];
                //将文件中的数据读到byte数组中
                in.read(bgpcm);
            } catch (Exception e) {
                e.printStackTrace();
            }
            byte[][] mix = {audio, bgpcm};
            byte[] mixpcm = VoiceMixerUtil.normalizationMix(mix);
            AudioTrackManager.getInstance().startPlay(mixpcm);
        }
    };

    //创建监听权限的接口对象
    PermissionsUtils.IPermissionsResult permissionsResult = new PermissionsUtils.IPermissionsResult() {
        @Override
        public void passPermissons() {
//            Toast.makeText(MainActivity.this, "权限通过", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void forbitPermissons() {
//            finish();
            Toast.makeText(MainActivity.this, "权限拒绝", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //就多一个参数this
        PermissionsUtils.getInstance().onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.vcn1:
                vcnString = "xiaoyan";
                setVcnLight(0, 0xFF4CAF50);
                SetVcnNameColor(0, 0xFF4CAF50);
                break;
            case R.id.vcn2:
                vcnString = "aisxping";
                setVcnLight(1, 0xFF4CAF50);
                SetVcnNameColor(1, 0xFF4CAF50);
                break;
            case R.id.vcn3:
                vcnString = "aisjiuxu";
                setVcnLight(2, 0xFF4CAF50);
                SetVcnNameColor(2, 0xFF4CAF50);
                break;
            case R.id.vcn4:
                vcnString = "x2_yifei";
                setVcnLight(3, 0xFF4CAF50);
                SetVcnNameColor(3, 0xFF4CAF50);
                break;
            case R.id.btn_tts:
                try {
                    WebTTSWS.getTTSData(editView.getText().toString(), vcnString, mCurVolume, mCurSpeed, responseResult);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }
    private void setVcnLight(int index, int color) {
        mCurVcn = index;
        editText.setText(vcnText[index]);
        for (int i=0; i<vcn.length; i++) {
            if (i == index) {
                vcn[i].setBorderColor(color);
            }else {
                vcn[i].setBorderColor(0xFFFFFF);
            }
        }
    }
    private void SetVcnNameColor(int index, int color) {
        for (int i=0; i<vcn.length; i++) {
            if (i == index) {
                vcnName[i].setTextColor(color);
            }else {
                vcnName[i].setTextColor(0xFF888888);
            }
        }
    }
}
