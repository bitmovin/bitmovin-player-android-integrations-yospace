package com.bitmovin.player.integrations.bitmovinyospacemodule;

import com.bitmovin.player.api.event.listener.EventListener;
import com.bitmovin.player.api.event.listener.OnAdBreakFinishedListener;
import com.bitmovin.player.api.event.listener.OnAdBreakStartedListener;
import com.bitmovin.player.api.event.listener.OnAdClickedListener;
import com.bitmovin.player.api.event.listener.OnAdErrorListener;
import com.bitmovin.player.api.event.listener.OnAdFinishedListener;
import com.bitmovin.player.api.event.listener.OnAdSkippedListener;
import com.bitmovin.player.api.event.listener.OnAdStartedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class BitmovinYoSpaceEventEmitter {
    private ConcurrentHashMap<Class, List<EventListener>> eventListeners = new ConcurrentHashMap<>();

    public ConcurrentHashMap<Class, List<EventListener>> getEventListeners() {
        return eventListeners;
    }

    public void addEventListener(EventListener listener) {

        Class javaClass = null;

        if (listener instanceof OnAdBreakStartedListener){
            javaClass = OnAdBreakStartedListener.class;
        }else if (listener instanceof OnAdBreakFinishedListener){
            javaClass = OnAdBreakFinishedListener.class;
        }else if (listener instanceof OnAdClickedListener){
            javaClass = OnAdClickedListener.class;
        }else if (listener instanceof OnAdErrorListener){
            javaClass = OnAdErrorListener.class;
        }else if (listener instanceof OnAdFinishedListener){
            javaClass = OnAdFinishedListener.class;
        }else if (listener instanceof OnAdStartedListener){
            javaClass = OnAdStartedListener.class;
        }else if (listener instanceof OnAdSkippedListener){
            javaClass = OnAdSkippedListener.class;
        }

        if(javaClass != null) {
            List listeners = eventListeners.get(javaClass);
            if (listeners == null) {
                eventListeners.put(javaClass, new ArrayList<EventListener>());
            }
            eventListeners.get(javaClass).add(listener);
        }
    }
}
