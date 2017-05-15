package com.dji.djiflightcontrol.common;

/**
 * 动作类型
 */

public enum ActionType {
    UP, Down, Left, Right, Fount, Back, takephoto;

    @Override
    public String toString() {
        String s;
        switch (this) {
            case UP:
                s = "向上飞行";
                break;
            case Down:
                s = "向下飞行";
                break;
            case Left:
                s = "向左飞行";
                break;
            case Right:
                s = "向右飞行";
                break;
            case Fount:
                s = "向前飞行";
                break;
            case Back:
                s = "向后飞行";
                break;
            default:
                s = "拍照";
                break;
        }
        return s;
    }
}
