package com.navinfo.server.utils;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ArrayUtil;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import lombok.extern.slf4j.Slf4j;


/**
 * dll / so 调用工具
 * so路径 /usr/lib
 */
@Slf4j
public class NativeUtils {

    /**
     * 地图so
     * db路径/usr/map
     */
    public interface MapDBAdaptor extends Library {
        MapDBAdaptor instance = Native.load("MapDBAdaptor", MapDBAdaptor.class);
    }

    /**
     * 算路so
     */
    public interface ServerRoute extends Library {
        MapDBAdaptor MAPDBADAPTOR = MapDBAdaptor.instance;
        ServerRoute instance = Native.load("ServerRoute", ServerRoute.class);

        /*请求算路资源*/
        int startServerRoute();

        /*释放算路资源*/
        int stopServerRoute();

        /*算路*/
        int requestServerRoute(byte[] bufIn, int bufLengthIn, PointerByReference bufOut, IntByReference bufLengthOut);

        /*释放路径规划结果*/
        int releaseServerRouteResult(Pointer bufOut);
    }

    /**
     * 算路方法
     *
     * @param bytes
     * @return
     */
    public static byte[] callRoutePlanning(byte[] bytes) {
        log.info("jna call....");
        byte[] result = null;
        StopWatch watch = new StopWatch("callRoute");
        watch.start("startServerRoute");
        int startResult = ServerRoute.instance.startServerRoute();
        watch.stop();
        if (startResult == 0) { //启动资源失败
            log.info("start server route failed");
            return result;
        }
        try {
            PointerByReference bufOut = new PointerByReference(Pointer.NULL);
            IntByReference bufLengthOut = new IntByReference();
            watch.start("requestServerRoute");
            int routeResult = ServerRoute.instance.requestServerRoute(bytes, bytes.length, bufOut, bufLengthOut);
            watch.stop();
            if (routeResult != 0) {
                int dataLen = bufLengthOut.getValue();
                byte[] data = bufOut.getValue().getByteArray(4, dataLen - 4);
                result = ArrayUtil.clone(data);
                Pointer pointer = Pointer.createConstant(getPeer(bufOut.getValue()));
                watch.start("releaseServerRouteResult");
                int releaseResult = ServerRoute.instance.releaseServerRouteResult(pointer);
                watch.stop();
                log.info("route resource release : " + releaseResult);
            }
        } catch (Exception e) {
            log.error("call requestServerRoute error", e);
            throw e;
        } finally {
            watch.start("stopServerRoute");
            int stopResult = ServerRoute.instance.stopServerRoute();
            watch.stop();
            log.info("route resource stop : " + stopResult);
        }
        log.info(watch.prettyPrint());
        return result;
    }

    /**
     * 获取指针指向的地址
     *
     * @param p
     * @return
     */
    public static long getPeer(Pointer p) {
        String peer = p.toString();
        return Long.parseLong(peer.substring(peer.indexOf("x") + 1), 16);
    }

}
