package com.navinfo.server.service.impl;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ClassUtil;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.navinfo.server.common.msg.utils.TimeUtils;
import com.navinfo.server.queue.QueueProducer;
import com.navinfo.server.queue.common.AsyncCallbackTemplate;
import org.omg.SendingContext.RunTime;

import java.util.concurrent.*;
import java.util.function.Consumer;

public class MapDataServiceTest {

    public static void main(String[] args) {

       /* String path = "E:/map/DATA0000000000000001";
        try(FileInputStream in = new FileInputStream(path)){
            FileChannel channel = in.getChannel();
            MappedByteBuffer fileBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        }catch (Exception e){

        }*/
        ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(1);
        ExecutorService callBackExecutor = Executors.newWorkStealingPool(5);
        ListeningExecutorService callExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

        ListenableFuture<byte[]> success = callExecutor.submit(() -> success());
        ListenableFuture<byte[]> failed = callExecutor.submit(() -> failed());
        ListenableFuture<byte[]> timeout = callExecutor.submit(() -> timeout());
        Consumer<byte[]> onCallSuccess = data -> {

        };



        AsyncCallbackTemplate.withCallbackAndTimeout(success, data -> {
                    System.out.println("1成功");
                },
                (t) -> {
                    System.out.println("1异常回调");
                    t.printStackTrace();
                }, 2000, timeoutExecutor, callBackExecutor);

        AsyncCallbackTemplate.withCallbackAndTimeout(failed, data -> {
                    System.out.println("2不会出现");
                },
                (t) -> {
                    System.out.println("2异常回调");
                    t.printStackTrace();
                }, 2000, timeoutExecutor, callBackExecutor);

        AsyncCallbackTemplate.withCallbackAndTimeout(timeout, data -> {
                    System.out.println("3不会出现");
                },
                (t) -> {
                    System.out.println("3异常回调");
                    t.printStackTrace();
                }, 2000, timeoutExecutor, callBackExecutor);
    }

    private static byte[] success() {
        return new byte[]{11,12};
    }

    private static byte[] failed() {
        int a = 1/0;
        return null;
    }

    private static byte[] timeout() {
        ThreadUtil.sleep(5000);
        return null;
    }

}
