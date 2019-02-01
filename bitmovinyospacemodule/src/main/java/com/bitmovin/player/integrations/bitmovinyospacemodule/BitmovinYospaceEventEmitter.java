package com.bitmovin.player.integrations.bitmovinyospacemodule;

import android.os.Handler;
import android.os.Looper;

import com.bitmovin.player.api.event.data.AdBreakFinishedEvent;
import com.bitmovin.player.api.event.data.AdBreakStartedEvent;
import com.bitmovin.player.api.event.data.AdClickedEvent;
import com.bitmovin.player.api.event.data.AdErrorEvent;
import com.bitmovin.player.api.event.data.AdFinishedEvent;
import com.bitmovin.player.api.event.data.AdSkippedEvent;
import com.bitmovin.player.api.event.data.AdStartedEvent;
import com.bitmovin.player.api.event.data.BitmovinPlayerEvent;
import com.bitmovin.player.api.event.data.ErrorEvent;
import com.bitmovin.player.api.event.data.WarningEvent;
import com.bitmovin.player.api.event.listener.EventListener;
import com.bitmovin.player.api.event.listener.OnAdBreakFinishedListener;
import com.bitmovin.player.api.event.listener.OnAdBreakStartedListener;
import com.bitmovin.player.api.event.listener.OnAdClickedListener;
import com.bitmovin.player.api.event.listener.OnAdErrorListener;
import com.bitmovin.player.api.event.listener.OnAdFinishedListener;
import com.bitmovin.player.api.event.listener.OnAdSkippedListener;
import com.bitmovin.player.api.event.listener.OnAdStartedListener;
import com.bitmovin.player.api.event.listener.OnErrorListener;
import com.bitmovin.player.api.event.listener.OnWarningListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class BitmovinYospaceEventEmitter {
    private ConcurrentHashMap<Class, List<EventListener>> eventListeners = new ConcurrentHashMap<>();

    public synchronized void addEventListener(EventListener listener) {
        Class listenerClass = listenerClass(listener);

        if (listenerClass != null) {
            List listeners = eventListeners.get(listenerClass);
            if (listeners == null) {
                eventListeners.put(listenerClass, new ArrayList<EventListener>());
            }
            eventListeners.get(listenerClass).add(listener);
        }
    }

    public synchronized void removeEventListener(EventListener listener) {

        Class listenerClass = listenerClass(listener);

        if (listenerClass != null) {
            List listeners = eventListeners.get(listenerClass);
            if (listeners != null) {
                listeners.remove(listener);
            }
        }
    }

    public synchronized void emit(BitmovinPlayerEvent event) {

        Handler mainHandler = new Handler(Looper.getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                if (event instanceof AdBreakStartedEvent) {
                    for (EventListener listener : eventListeners.get(OnAdBreakStartedListener.class)) {
                        ((OnAdBreakStartedListener) listener).onAdBreakStarted((AdBreakStartedEvent) event);
                    }
                } else if (event instanceof AdBreakFinishedEvent) {
                    for (EventListener listener : eventListeners.get(OnAdBreakFinishedListener.class)) {
                        ((OnAdBreakFinishedListener) listener).onAdBreakFinished((AdBreakFinishedEvent) event);
                    }
                } else if (event instanceof AdStartedEvent) {
                    for (EventListener listener : eventListeners.get(OnAdStartedListener.class)) {
                        ((OnAdStartedListener) listener).onAdStarted((AdStartedEvent) event);
                    }
                } else if (event instanceof AdFinishedEvent) {
                    for (EventListener listener : eventListeners.get(OnAdFinishedListener.class)) {
                        ((OnAdFinishedListener) listener).onAdFinished((AdFinishedEvent) event);
                    }
                } else if (event instanceof AdClickedEvent) {
                    for (EventListener listener : eventListeners.get(OnAdClickedListener.class)) {
                        ((OnAdClickedListener) listener).onAdClicked((AdClickedEvent) event);
                    }
                } else if (event instanceof AdErrorEvent) {
                    for (EventListener listener : eventListeners.get(OnAdErrorListener.class)) {
                        ((OnAdErrorListener) listener).onAdError((AdErrorEvent) event);
                    }
                } else if (event instanceof AdSkippedEvent) {
                    for (EventListener listener : eventListeners.get(OnAdSkippedListener.class)) {
                        ((OnAdSkippedListener) listener).onAdSkipped((AdSkippedEvent) event);
                    }
                } else if (event instanceof ErrorEvent) {
                    for (EventListener listener : eventListeners.get(OnErrorListener.class)) {
                        ((OnErrorListener) listener).onError((ErrorEvent) event);
                    }
                } else if (event instanceof WarningEvent) {
                    for (EventListener listener : eventListeners.get(OnWarningListener.class)) {
                        ((OnWarningListener) listener).onWarning((WarningEvent) event);
                    }
                }
            }
        };
        mainHandler.post(myRunnable);
    }

    private Class listenerClass(EventListener listener) {
        Class javaClass = null;

        if (listener instanceof OnAdBreakStartedListener) {
            javaClass = OnAdBreakStartedListener.class;
        } else if (listener instanceof OnAdBreakFinishedListener) {
            javaClass = OnAdBreakFinishedListener.class;
        } else if (listener instanceof OnAdClickedListener) {
            javaClass = OnAdClickedListener.class;
        } else if (listener instanceof OnAdErrorListener) {
            javaClass = OnAdErrorListener.class;
        } else if (listener instanceof OnAdFinishedListener) {
            javaClass = OnAdFinishedListener.class;
        } else if (listener instanceof OnAdStartedListener) {
            javaClass = OnAdStartedListener.class;
        } else if (listener instanceof OnAdSkippedListener) {
            javaClass = OnAdSkippedListener.class;
        } else if (listener instanceof OnErrorListener) {
            javaClass = OnErrorListener.class;
        } else if (listener instanceof OnWarningListener) {
            javaClass = OnWarningListener.class;
        }

        return javaClass;
    }
}
