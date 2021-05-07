package cn.dreamchaser.bindlinkfinal01;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {
    String pcIpAddr;
    String pathToOpen;//从电脑接收的文件的路径
    String devIpAddr;//设备（手机）的ip地址
    int port;
    EditText et_ipAddr;
    EditText et_Port;
    TextView textView_status;
    SharedPreferences.Editor editor;
    SharedPreferences preferences;
    MyTcp mTcp;
    Intent intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preferences = getSharedPreferences("data",MODE_PRIVATE);
        editor = getSharedPreferences("data",MODE_PRIVATE).edit();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化控件
        et_ipAddr = findViewById(R.id.editTextIpAddr);
        et_Port = findViewById(R.id.editTextPort);
        textView_status = findViewById(R.id.textView);





        //读入配置
        et_ipAddr.setText(preferences.getString("ipAddr","Default"));
        et_Port.setText(preferences.getString("port","2335"));
        //初始化变量
        devIpAddr = getDevIP();
        intent = getIntent();
        Log.e("MainActivity",devIpAddr);

        //解析电脑发来的IP
        pcIpAddr = intent.getStringExtra("PcIpAddr");
        TextView txtv_pcAddr = findViewById(R.id.textView_pcaddr);
        txtv_pcAddr.setText("电脑IP：" + pcIpAddr);
        if(pcIpAddr!=null){//有内容的话，就发送自己的IP
            et_ipAddr.setText(pcIpAddr);
            port = Integer.valueOf(et_Port.getText().toString()).intValue();
            mTcp = new MyTcp(pcIpAddr,port,devIpAddr,this);
            new Thread(){
                @Override
                public void run(){
                    if(mTcp.initConnection()) {
                        mTcp.connectToPC();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.e("conToBeSent:", devIpAddr);
                        mTcp.sendStr("#IP\t"+devIpAddr);
                        //Toast.makeText(MainActivity.this,"已发送到电脑",Toast.LENGTH_SHORT).show();
                        //textView_status.setText("已经发送");
                        finish();
                    }
                }
            }.start();
            //保存配置文件
        }
        //初始化文件路径或文本内容
        if(pcIpAddr==null){pcIpAddr = et_ipAddr.getText().toString();}
        port = Integer.valueOf(et_Port.getText().toString()).intValue();
        mTcp = new MyTcp(pcIpAddr,port,devIpAddr,this);
        String conToBeSent = initContent();
        //发送数据前判断是否需要
        if(conToBeSent!=null){
            //如果是从分享打开的
            new Thread(){
                @Override
                public void run(){
                    if(mTcp.initConnection()) {
                        mTcp.connectToPC();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.e("conToBeSent:", conToBeSent);
                        mTcp.sendStr(conToBeSent);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //最后自动退出程序
                        finish();
                    }
                }
            }.start();
        }
        editor.putString("ipAddr",pcIpAddr);
        editor.putString("port",Integer.toString(port));
        editor.apply();
    }







    //点击“保存IP”按钮
    public void connectManual(View v){
        Log.e("MainActivity","手动连接");
        pcIpAddr = et_ipAddr.getText().toString();
        port = Integer.valueOf(et_Port.getText().toString()).intValue();
        //mTcp = new MyTcp(pcIpAddr,port,devIpAddr,this);
        //保存IP地址以供下次使用
        editor.putString("ipAddr",pcIpAddr);
        editor.putString("port",Integer.toString(port));
        editor.apply();
        mTcp.connectToPC();
        //向电脑发送IP地址
        mTcp.sendStr("#IP\t"+devIpAddr);
    }

    //获取本机的IP
    public String getDevIP(){
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address))
                    {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        }
        catch (SocketException ex){
            ex.printStackTrace();
        }
        return null;
    }

    //获取要发送的内容
    String initContent(){

        String type = intent.getType();
        String sendContent = "";
        Uri fileUri;
        MyFileUtils fu = new MyFileUtils(this);
        String action = intent.getAction();
        Log.e("log",action);
        //判断 所有分享行为 ACTION_SEND
        if (Intent.ACTION_SEND.equals(action)&&type!=null) {
            //判断纯文本和链接分享
            if ("text/plain".equals(type)) {
                Log.e("link", intent.getStringExtra(Intent.EXTRA_TEXT));
                if(intent.getStringExtra(Intent.EXTRA_TITLE)!=null){
                    Log.e("Title:",intent.getStringExtra(Intent.EXTRA_TITLE));
                    //sendContent += "Title:"+intent.getStringExtra(Intent.EXTRA_TITLE);

                }
                sendContent += "#TEXT\t"+intent.getStringExtra(Intent.EXTRA_TEXT);
                if(intent.getStringExtra(Intent.EXTRA_TEXT)!=null){
                    Log.e("Text:",intent.getStringExtra(Intent.EXTRA_TEXT));
                    //sendContent += "   "+"Text:"+ intent.getStringExtra(Intent.EXTRA_TEXT);
                }
            }
            //判断 文件分享行为
            else{
                fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if(fileUri!=null) {
                    //txtv_path.setText(fu.getFilePathByUri(fileUri));
                    sendContent += "#FILE\t" + fu.getFilePathByUri(fileUri);
                }else{
                    Toast.makeText(this,"不是文件分享",Toast.LENGTH_SHORT).show();
                }
            }
        }
        //判断 文件打开行为 ACTION_VIEW
        else if(intent.ACTION_VIEW.equals(action)&&type!=null) {
            Log.e("file",intent.getDataString());
            fileUri = intent.getData();
            if(fileUri!=null) {
                //txtv_path.setText(fu.getFilePathByUri(fileUri));
                sendContent += "#FILE\t" + fu.getFilePathByUri(fileUri);
            }else{
                Toast.makeText(this,"不是文件分享",Toast.LENGTH_SHORT).show();
            }
        }
        else{
            Log.e("Error","不支持的操作");
            //Toast.makeText(this,"不支持的操作",Toast.LENGTH_SHORT).show();
            return null;
        }
        Log.e("ToBeSend",sendContent);
        Log.e("sendContent:",sendContent);
        return sendContent;
    }


}