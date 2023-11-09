package com.xraph.plugin.flutter_unity_widget

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.hardware.input.InputManager
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import com.unity3d.player.IUnityPlayerLifecycleEvents
import com.unity3d.player.UnityPlayer

@SuppressLint("NewApi")
class CustomUnityPlayer(context: Activity, upl: IUnityPlayerLifecycleEvents?) : UnityPlayer(context, upl) {
    var validDeviceId: Int = 0

    init {
        var svc = context.getSystemService(Context.INPUT_SERVICE)
        if (svc is InputManager) {
            var ids = svc.getInputDeviceIds()
            for (id in ids) {
                var device = svc.getInputDevice(id)
                if (device.getSources() and InputDevice.SOURCE_TOUCHSCREEN == InputDevice.SOURCE_TOUCHSCREEN) {
                    validDeviceId = id
                    break
                }
            }
        }
    }

    companion object {
        internal const val LOG_TAG = "CustomUnityPlayer"
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        Log.i(LOG_TAG, "ORIENTATION CHANGED")
        super.onConfigurationChanged(newConfig)
    }

    override fun onAttachedToWindow() {
        Log.i(LOG_TAG, "onAttachedToWindow")
        super.onAttachedToWindow()
        UnityPlayerUtils.resume()
        UnityPlayerUtils.pause()
        UnityPlayerUtils.resume()
    }

    override fun onDetachedFromWindow() {
        Log.i(LOG_TAG, "onDetachedFromWindow")
        // todo: fix more than one unity view, don't add to background.
//        UnityPlayerUtils.addUnityViewToBackground()
        super.onDetachedFromWindow()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Sometimes we receive a touch up event with device id 0.
        // But down and move events with device id nonzero.
        // Which causes unity cannot handle touch up events and
        // entire input will stuck. Add a hack to prevent device id 0.
        var hackedEvent = MotionEvent.obtain(
            ev.getDownTime(),
            ev.getEventTime(),
            ev.getAction(),
            ev.getPointerCount(),
            (0 until ev.getPointerCount())
                .map { i ->
                    MotionEvent.PointerProperties().also { pointerProperties ->
                        ev.getPointerProperties(i, pointerProperties)
                    }
                }
                .toTypedArray(),
            (0 until ev.getPointerCount())
                .map { i ->
                    MotionEvent.PointerCoords().also { pointerCoords ->
                        ev.getPointerCoords(i, pointerCoords)
                    }
                }
                .toTypedArray(),
            ev.getMetaState(),
            ev.getButtonState(),
            ev.getXPrecision(),
            ev.getYPrecision(),
            validDeviceId,
            ev.getEdgeFlags(),
            InputDevice.SOURCE_TOUCHSCREEN,
            ev.getFlags());

        var result = super.dispatchTouchEvent(hackedEvent);
        hackedEvent.recycle();
        return result;
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean{
        if (event == null) return false

        // In flutter virtual displays mode, this line return false
        // causes entire unity cannot receive touch events.
        super.onTouchEvent(event)
        return true
    }

}