package cn.dreamchaser.bindlinkfinal01;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Handler;


//公有类，只需实例化这个类就行
public class MyTcp {
    ServerSocket mServerSocket;
    ClientSocketConnectThread csct;
    String ipAddrDev;//手机的IP
    //Socket socket;
    Context context;
    String ipAddr;
    int port;
    MyTcp(int port, Context context){
        try{
            mServerSocket = new ServerSocket(port);
            Log.e("MyServerSocket","已经创建对象");
            if(mServerSocket==null){
                Log.e("MyServerSocket","怎么回事儿");}
        }catch (IOException e){
            e.printStackTrace();
            Log.e("MyServerSocket","创建对象失败");
            //Toast.makeText(context, "监听服务开启失败",Toast.LENGTH_SHORT).show();
            return;
        }
    }

    //客户端的构造方法
    MyTcp(String ipAddr,int port,String ipAddrDev,Context context){
        this.ipAddr = ipAddr;
        this.port = port;
        this.ipAddrDev = ipAddrDev;
        this.context = context;
    }

    public void startListening(){
        ServerSocketAcceptThread ssat = new ServerSocketAcceptThread(mServerSocket,context);
        if(mServerSocket==null){Log.e("startListening","mServerSocket is NULL");}
        ssat.start();
    }

    public void connectToPC(){
        csct = new ClientSocketConnectThread(ipAddr,port,ipAddrDev,context);
        csct.start();
    }

    public void sendStr(String str){
        csct.writeMsg(str);
    }

    public boolean initConnection(){
        csct = new ClientSocketConnectThread(ipAddr,port,ipAddrDev,context);
        return csct.initConnection();
    }

}


//线程类，在公有类中实例化，用户无需手动实例化
class ServerSocketAcceptThread extends Thread{
    ServerSocket mServerSocket;
    Socket mSocket;
    Context context;
    InputStream mInStream;
    OutputStream mOutStream;
    InputStreamReader inputStreamReader;
    BufferedReader bufferedReader;
    ServerSocketAcceptThread(ServerSocket _mServerSocket, Context _context){
        this.mServerSocket = _mServerSocket;
        this.context = _context;

    }
    @Override
    public void run(){
        try{
            //初始化
            //建立新线程，防止阻塞主界面
            //等待客户端(电脑)连接
            Log.e("Thread","等待连接中");
            if(mServerSocket == null){Log.e("Thread","mServerSocket is NULL");}
            mSocket = mServerSocket.accept();
            Log.e("Thread","接受连接");
            //获取输入输出流
            mInStream = mSocket.getInputStream();
            mOutStream = mSocket.getOutputStream();
            //初始化输入流读取器
            inputStreamReader = new InputStreamReader(mInStream,"UTF-8");
            bufferedReader = new BufferedReader(inputStreamReader);
        }catch(IOException e){
            e.printStackTrace();
            return;
        }

        //用于存放收到的数据
        String receiveData = null;
        //循环读取收到的数据
        while(true){
            //怕卡死，延时500ms
            try {
                Thread.sleep(500);
            }catch(InterruptedException e){
                e.printStackTrace();
            }

            Log.e("test","test");

            if(receiveData!=null){
                Log.e("data",receiveData);
            }

            //读取数据，读不到换行符的话就会卡死
            receiveData = getMsg();
            if(receiveData!=null){
                Log.e("INFO","收到数据"+receiveData);
                //Toast.makeText(context,"收到数据"+receiveData,Toast.LENGTH_SHORT).show();

                writeMsg("你好电脑\n");
            }else{
                //断开后getMsg方法会返回null，此时跳出循环
                try{
                    mSocket.shutdownInput();
                    mSocket.shutdownOutput();
                    mSocket.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
                break;
            }
        }
        Log.e("Connection","已经断开连接");
    }

    public void writeMsg(String msg){
        if(msg.length()==0 || mOutStream==null){
            return;
        }
        try{
            mOutStream.write(msg.getBytes());
            mOutStream.flush();
            Log.e("Out","正在回复"+msg);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public String getMsg(){
        Log.e("getMsg","test2");
        String info = null;
        try{
            //这里，读不到换行符就会卡住
            info = bufferedReader.readLine();
        }catch(IOException e){
            e.printStackTrace();
        }
        Log.e("getMsg","test3");
        return info;
    }
}


class ClientSocketConnectThread extends Thread{
    boolean isConnected = false;
    String ipAddrPC;
    String ipAddrDev;
    int port;
    Socket mSocket;
    BufferedReader in;
    PrintWriter out;

    ClientSocketConnectThread(String ipAddrPC, int port, String ipAddrDev,Context context){
        this.ipAddrPC = ipAddrPC;
        this.port = port;
        this.ipAddrDev = ipAddrDev;
    }

    public boolean initConnection(){
        try{
            mSocket = new Socket(ipAddrPC,port);
            in = new BufferedReader(new InputStreamReader(mSocket.getInputStream(),"UTF-8"));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream())),true);
            writeMsg("Connected");
            return true;
        }
        catch(UnknownHostException e){
            e.printStackTrace();
        }
        catch(IOException e){
            //return false;
            e.printStackTrace();
        }finally {
            //return false;
        }
        return true;
    }



    public void writeMsg(String msg){
        if(msg.length()==0 || out==null){
            return;
        }
        if(mSocket.isConnected()){
            if(!mSocket.isOutputShutdown()){
                out.println(msg);
                Log.e("MyTcp","已经发送"+msg);
            }
        }

    }

    public String getMsg(){
        Log.e("getMsg","test2");
        String info = null;
        try{
            //这里，读不到换行符就会卡住
            info = in.readLine();
        }catch(IOException e){
            e.printStackTrace();
        }
        Log.e("getMsg","test3");
        return info;
    }

    @Override
    public void run(){
        try{
            mSocket = new Socket(ipAddrPC,port);
            in = new BufferedReader(new InputStreamReader(mSocket.getInputStream(),"UTF-8"));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream())),true);
            writeMsg("Connected");
        }catch(IOException e){
            e.printStackTrace();
        }

        //用于存放收到的数据
        String receiveData = null;
        //循环读取收到的数据
        while(true){
            //怕卡死，延时500ms
            //没连上就一直发IP
            if(!isConnected){
                writeMsg("#IP\t"+ipAddrDev);
            }
            try {
                Thread.sleep(500);
            }catch(InterruptedException e){
                e.printStackTrace();
            }

            if(receiveData!=null){
                Log.e("data",receiveData);
            }

            //读取数据，读不到换行符的话就会卡死
            receiveData = getMsg();
            if(receiveData!=null){
                Log.e("INFO","收到数据"+receiveData);
                //Toast.makeText(context,"收到数据"+receiveData,Toast.LENGTH_SHORT).show();
                //break;
                if(receiveData.equals("Connected")){
                    isConnected = true;
                    Log.e("Connection","连接已经建立");
                    //TextView textView = findViewById(R.id.);
                    //Toast.makeText(context)
                }
                //writeMsg("你好电脑\n");
            }else{

                break;
            }
        }
        Log.e("Connection","连接结束");



    }


}