package com.doit.net.socket;

import com.doit.net.protocol.LTEReceiveManager;
import com.doit.net.utils.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Author：Libin on 2020/5/20 15:43
 * Email：1993911441@qq.com
 * Describe：socket服务端
 */
public class ServerSocketUtils {
    private static ServerSocketUtils mInstance;
    private ServerSocket mServerSocket;
    public final static String LOCAL_IP = "192.168.1.133";   //本机ip
    public final static int LOCAL_PORT = 7003;   //本机端口
    private final static int READ_TIME_OUT = 60000;  //超时时间
    public static  String REMOTE_4G_HOST = "";  //4G设备host
    public static String REMOTE_2G_HOST = "";     //2G设备host

    private Map<String, Socket> map = new HashMap<>();


    private ServerSocketUtils() {
        try {
            mServerSocket = new ServerSocket(LOCAL_PORT);
        } catch (IOException e) {
            e.printStackTrace();
            LogUtils.log("创建ServerSocket异常："+e.getMessage());
        }
    }

    public static ServerSocketUtils getInstance() {
        if (mInstance == null) {
            synchronized (ServerSocketUtils.class) {
                if (mInstance == null) {
                    mInstance = new ServerSocketUtils();
                }
            }

        }
        return mInstance;
    }


    /**
     * @param onSocketChangedListener 线程接收连接
     */
    public void startTCP(OnSocketChangedListener onSocketChangedListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Socket socket = mServerSocket.accept();  //获取socket
                        String remoteIP = socket.getInetAddress().getHostAddress();  //远程ip
                        int remotePort = socket.getPort();    //远程端口

                        socket.setSoTimeout(READ_TIME_OUT);      //设置超时
                        socket.setKeepAlive(true);
                        socket.setTcpNoDelay(true);

                        LogUtils.log("TCP收到设备连接,ip：" + remoteIP + "；端口：" + remotePort);
                        if (remoteIP.startsWith("192.168.1")) {
                            String host = remoteIP + ":" + remotePort;
                            map.put(host, socket);

                            if (onSocketChangedListener != null) {
                                onSocketChangedListener.onChange(host);
                            }
                            new ReceiveThread(socket, host, onSocketChangedListener).start();

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        LogUtils.log("TCP异常：" + e.getMessage());
                    }
                }
            }
        }).start();
    }


    /**
     * 接收线程
     */
    public class ReceiveThread extends Thread {
        private Socket socket;
        private String host;
        private OnSocketChangedListener onSocketChangedListener;

        public ReceiveThread(Socket socket, String host, OnSocketChangedListener onSocketChangedListener) {
            this.socket = socket;
            this.host = host;
            this.onSocketChangedListener = onSocketChangedListener;
        }

        @Override
        public void run() {
            super.run();

            //数据缓存
            byte[] bytesReceived = new byte[1024];
            //接收到流的数量
            int receiveCount;
            LTEReceiveManager lteReceiveManager = new LTEReceiveManager();
            long lastTime = 0; //上次读取时间

            while (true) {
                //获取输入流
                try {
                    InputStream inputStream = socket.getInputStream();
                    //循环接收数据
                    while ((receiveCount = inputStream.read(bytesReceived)) != -1) {
                        lteReceiveManager.parseData(host, bytesReceived, receiveCount);
                        lastTime = System.currentTimeMillis();
                    }
                    LogUtils.log(host + "：socket异常，读取长度：" + receiveCount);
                    closeSocket(socket, host, onSocketChangedListener);
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                    LogUtils.log(host + "：socket异常:" + e.toString());
                    if (e instanceof SocketTimeoutException) {
                        //捕获读取超时异常，若超时时间内收到数据，不做处理

                        long timeout = System.currentTimeMillis() - lastTime;
                        LogUtils.log(host + "：socket异常:读取超时" + timeout);
                        if (timeout > READ_TIME_OUT) {
                            closeSocket(socket, host, onSocketChangedListener);
                            break;
                        }
                    } else {
                        closeSocket(socket, host, onSocketChangedListener);
                        break;
                    }
                }

            }


        }
    }

    private void closeSocket(Socket socket, String host, OnSocketChangedListener onSocketChangedListener) {
        try {
            socket.close();
            map.remove(host);
            if (onSocketChangedListener != null) {
                onSocketChangedListener.onChange(host);
            }
            LogUtils.log(host + "：关闭socket");
        } catch (Exception ex) {
            ex.printStackTrace();
            LogUtils.log(host + "：socket关闭失败:" + ex.toString());
        }
    }


    /**
     * 发送数据
     *
     * @param data
     * @return
     */
    public void sendData(String host, byte[] data) {

        Socket socket = map.get(host);
        if (socket != null && socket.isConnected()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(data);
                        outputStream.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                        LogUtils.log("socket发送失败：" + host + "," + e.getMessage());
                    }
                }
            }).start();
        } else {
            LogUtils.log("socket未连接");
        }

    }

}
