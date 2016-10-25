package com.github.SquidPony.gwt;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by Tommy Ettinger on 10/24/2016.
 */
public class Slider extends Widget {
    public int current;
    public final int min, max, step;
    public EventListener handler;
    public InputElement elem;
    public Slider()
    {
        this(0, 15, 1, 160, null);
    }
    public Slider(EventListener handler)
    {
        this(0, 15, 1, 160, handler);
    }
    public Slider(int mn, int mx, int step, int width, EventListener handler) {
        setElement(elem = createRangeInputElement(Document.get()));
        elem.setAttribute("type", "range");
        elem.setSize(width);
        if(mx < mn)
        {
            min = mx;
            max = mn;
        }
        else {
            min = mn;
            max = mx;
        }
        this.step = step;

        current = min;

        elem.setAttribute("min", String.valueOf(mn));
        elem.setAttribute("max", String.valueOf(mx));
        elem.setAttribute("step", String.valueOf(step));
        this.handler = handler;

        //sinkEvents(Event.ONCHANGE);
        Event.sinkEvents(elem, Event.ONCHANGE);
        Event.setEventListener(elem, new EventListener() {
            @Override
            public void onBrowserEvent(Event event) {
                switch (event.getTypeInt()) {
                    case Event.ONCHANGE:
                    try {
                        current = Integer.parseInt(elem.getValue());
                        if (Slider.this.handler != null)
                            Slider.this.handler.onBrowserEvent(event);
                    } catch (NumberFormatException nfe) {
                        current = min;
                    }
                    break;
                }
            }
        });
        //sinkEvents(Event.ONCHANGE);
    }
    public int getCurrent()
    {
        return current;
    }
    public void setCurrent(int value) {
        if (value >= min && value <= max) {
            elem.setValue(String.valueOf(value));
            current = value;
        }
    }
    public static native InputElement createRangeInputElement(Document doc) /*-{
    var e = doc.createElement("INPUT");
    e.type = "range";
    return e;
  }-*/;

}
