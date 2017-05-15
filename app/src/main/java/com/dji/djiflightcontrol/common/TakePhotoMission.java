package com.dji.djiflightcontrol.common;

import java.util.ArrayList;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.missionmanager.DJICustomMission;
import dji.sdk.missionmanager.missionstep.DJIMissionStep;
import dji.sdk.missionmanager.missionstep.DJITakeoffStep;
import dji.sdk.missionmanager.missionstep.DJIWaypointStep;

public class TakePhotoMission extends DJICustomMission {
    private static final List<DJIMissionStep> missionList = new ArrayList<>();

    public TakePhotoMission(float distance, final int n) {
        this(missionList);
        missionList.add(new DJITakeoffStep(new DJICommonCallbacks.DJICompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null)
                    DJISampleApplication.util.showToast(djiError.getDescription());
                else
                    DJISampleApplication.util.showToast("起飞成功");
            }
        }));
        missionList.add(new MoveStep(new Action(distance, n, ActionType.UP), new DJICommonCallbacks.DJICompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null)
                    DJISampleApplication.util.showToast(djiError.getDescription());
            }
        }));
        missionList.add(new MoveStep(new Action(distance, n, ActionType.Down), new DJICommonCallbacks.DJICompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null)
                    DJISampleApplication.util.showToast(djiError.getDescription());
            }
        }));
    }


    public TakePhotoMission(List<DJIMissionStep> list) {
        super(list);
    }

    public class MoveStep extends DJIWaypointStep {
        private Action action;

        public MoveStep(Action action, DJICommonCallbacks.DJICompletionCallback djiCompletionCallback) {
            super(null, djiCompletionCallback);
            this.action = action;
        }

        @Override
        public void onPause(DJICommonCallbacks.DJICompletionCallback djiCompletionCallback) {
            action.onPause();
        }

        @Override
        public void onResume(DJICommonCallbacks.DJICompletionCallback djiCompletionCallback) {
            action.onResume();
        }

        @Override
        public void onCancel(DJICommonCallbacks.DJICompletionCallback djiCompletionCallback) {
            DJIError error = DJIError.COMMON_UNDEFINED;
            error.setDescription("任务取消");
            djiCompletionCallback.onResult(error);
            action.cancel();
        }

        @Override
        public void run() {
            action.start();
        }
    }
}
