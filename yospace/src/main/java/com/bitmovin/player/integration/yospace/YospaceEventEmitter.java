package com.bitmovin.player.integration.yospace;

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
import com.bitmovin.player.api.event.data.TimeChangedEvent;
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
import com.bitmovin.player.api.event.listener.OnTimeChangedListener;
import com.bitmovin.player.api.event.listener.OnWarningListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class YospaceEventEmitter {
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
        if (event instanceof AdBreakStartedEvent) {
            BitLog.INSTANCE.d("Emitting AdBreakStartedEvent");
            List<EventListener> listeners = eventListeners.get(OnAdBreakStartedListener.class);
            if (listeners != null) {
                for (EventListener listener : eventListeners.get(OnAdBreakStartedListener.class)) {
                    ((OnAdBreakStartedListener) listener).onAdBreakStarted((AdBreakStartedEvent) event);
                }
            }
        } else if (event instanceof AdBreakFinishedEvent) {
            BitLog.INSTANCE.d("Emitting AdBreakFinishedEvent");
            List<EventListener> listeners = eventListeners.get(OnAdBreakFinishedListener.class);
            if (listeners != null) {
                for (EventListener listener : eventListeners.get(OnAdBreakFinishedListener.class)) {
                    ((OnAdBreakFinishedListener) listener).onAdBreakFinished((AdBreakFinishedEvent) event);
                }
            }
        } else if (event instanceof AdStartedEvent) {
            BitLog.INSTANCE.d("Emitting AdStartedEvent");
            List<EventListener> listeners = eventListeners.get(OnAdStartedListener.class);
            if (listeners != null) {
                for (EventListener listener : eventListeners.get(OnAdStartedListener.class)) {
                    ((OnAdStartedListener) listener).onAdStarted((AdStartedEvent) event);
                }
            }
        } else if (event instanceof AdFinishedEvent) {
            BitLog.INSTANCE.d("Emitting AdFinishedEvent");
            List<EventListener> listeners = eventListeners.get(OnAdFinishedListener.class);
            if (listeners != null) {
                for (EventListener listener : eventListeners.get(OnAdFinishedListener.class)) {
                    ((OnAdFinishedListener) listener).onAdFinished((AdFinishedEvent) event);
                }
            }
        } else if (event instanceof AdClickedEvent) {
            List<EventListener> listeners = eventListeners.get(OnAdClickedListener.class);
            if (listeners != null) {
                for (EventListener listener : eventListeners.get(OnAdClickedListener.class)) {
                    ((OnAdClickedListener) listener).onAdClicked((AdClickedEvent) event);
                }
            }
        } else if (event instanceof AdErrorEvent) {
            List<EventListener> listeners = eventListeners.get(OnAdErrorListener.class);
            if (listeners != null) {
                for (EventListener listener : eventListeners.get(OnAdErrorListener.class)) {
                    ((OnAdErrorListener) listener).onAdError((AdErrorEvent) event);
                }
            }
        } else if (event instanceof AdSkippedEvent) {
            BitLog.INSTANCE.d("Emitting AdSkippedEvent");
            List<EventListener> listeners = eventListeners.get(OnAdSkippedListener.class);
            if (listeners != null) {
                for (EventListener listener : eventListeners.get(OnAdSkippedListener.class)) {
                    ((OnAdSkippedListener) listener).onAdSkipped((AdSkippedEvent) event);
                }
            }
        } else if (event instanceof ErrorEvent) {
            List<EventListener> listeners = eventListeners.get(OnErrorListener.class);
            if (listeners != null) {
                for (EventListener listener : eventListeners.get(OnErrorListener.class)) {
                    ((OnErrorListener) listener).onError((ErrorEvent) event);
                }
            }
        } else if (event instanceof WarningEvent) {
            List<EventListener> listeners = eventListeners.get(OnWarningListener.class);
            if (listeners != null) {
                for (EventListener listener : eventListeners.get(OnWarningListener.class)) {
                    ((OnWarningListener) listener).onWarning((WarningEvent) event);
                }
            }
        } else if (event instanceof TimeChangedEvent) {
            List<EventListener> listeners = eventListeners.get(OnTimeChangedListener.class);
            if (listeners != null) {
                for (EventListener listener : listeners) {
                    ((OnTimeChangedListener) listener).onTimeChanged((TimeChangedEvent) event);
                }
            }
        } else if (event instanceof TruexAdFreeEvent) {
            BitLog.INSTANCE.d("Emitting TruexAdFreeEvent");
            List<EventListener> listeners = eventListeners.get(OnTruexAdFreeListener.class);
            if (listeners != null) {
                for (EventListener listener : listeners) {
                    ((OnTruexAdFreeListener) listener).onTruexAdFree((TruexAdFreeEvent) event);
                }
            }
        }
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
        } else if (listener instanceof OnTimeChangedListener) {
            javaClass = OnTimeChangedListener.class;
        } else if (listener instanceof OnTruexAdFreeListener) {
            javaClass = OnTruexAdFreeListener.class;
        }

        return javaClass;
    }
}
