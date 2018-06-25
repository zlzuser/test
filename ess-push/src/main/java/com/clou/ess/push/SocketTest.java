package com.clou.ess.push;

import com.clou.ess.util.Global;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

/**
 * @author :zhanglz
 * @project :ess-app
 * @discription :
 * @since :2018/3/19 10:32
 */
public class SocketTest {

    public static void main(String[] args) throws InterruptedException {
        for(int i=0;i<15;i++){
            Thread.sleep(1000);
            System.out.println("``````````");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Socket socket = null;
                    PrintStream ps = null;
                    try {
                        socket = new Socket("localhost", Global.port);
                        ps = new PrintStream(socket.getOutputStream());
                        Thread.sleep(15000);
                        ps.println("{\"user_id\":137,\"name\":\"Mahesh\", \"age\":20,\"close\":\"NO\"}");
                        System.out.println("#########");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        if (socket != null) {
                            try {
                                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                if(input.readLine()==null){
                                    ps.close();
                                    System.out.println("&&&&&&&&");
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        /*try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/
                        }
                    }
                }
            }).start();
        }


    }
}
