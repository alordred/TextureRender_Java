package com.google.android.exoplayer2.AL;

/**
 * Created by lihongsheng on 12/19/17.
 */

public class ALCmd {
    public static int CURRENT_CHUNK = 0;
    public static int CURRENT_MOVE_STATE = -1;//前进
    public static final int MOVE_STATE_STOP = 0;//前进
    public static final int MOVE_STATE_FORWARD = 1;//前进
    public static final int MOVE_STATE_BACK = 2;//后退
    public static final int MOVE_STATE_LOOK_UP = 6;//向上看
    public static final int MOVE_STATE_LOOK_DOWN = 7;//向上看
    public static String MOVE_STATE_LOOK_UP_URL = "";//向上看URL
    public static String MOVE_STATE_LOOK_DOWN_URL = "";//向下看URL
}
