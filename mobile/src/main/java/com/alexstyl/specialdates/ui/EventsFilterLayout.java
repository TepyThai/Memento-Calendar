package com.alexstyl.specialdates.ui;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;

import com.novoda.notils.logger.simple.Log;

import java.util.HashMap;
import java.util.Map;

public class EventsFilterLayout extends TabLayout {

    private OnSectionPressedListener listener = OnSectionPressedListener.NO_CALLBACK;
    private final Map<EventSection, Boolean> categoryStates = new HashMap<>(EventSection.values().length);

    private int numberOfActive = EventSection.values().length;

    public EventsFilterLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialiseTabs();
        addOnTabSelectedListener(onTabSelectedListener);
    }

    private void initialiseTabs() {
        for (EventSection section : EventSection.values()) {
            Tab contactsTab = newTab();
            contactsTab.setIcon(section.getEnabledResId());
            addTab(contactsTab);
        }

        for (EventSection section : EventSection.values()) {
            categoryStates.put(section, true);
        }
    }

    public void setOnSectionPressedListener(OnSectionPressedListener listener) {
        this.listener = listener;
    }

    private final SimpleTabSelectedListener onTabSelectedListener = new SimpleTabSelectedListener() {

        @Override
        public void onTabSelected(Tab tab) {
            int position = tab.getPosition();
            EventSection sectionPressed = EventSection.ofId(position);

            if (isTheLastEnabledSection(sectionPressed)) {
                return;
            }

            Boolean newState = updateStateOf(sectionPressed);
            int updatedIcon = getIconFor(sectionPressed, newState);
            tab.setIcon(updatedIcon);
            listener.onSectionPressed(sectionPressed, newState);
            if (!newState) {
                numberOfActive--;
            } else {
                numberOfActive++;
            }
        }

        private boolean isTheLastEnabledSection(EventSection sectionPressed) {
            return categoryStates.get(sectionPressed) && numberOfActive == 1;
        }

        @DrawableRes
        private int getIconFor(EventSection section, boolean enabled) {
            if (enabled) {
                return section.getEnabledResId();
            } else {
                return section.getDisabledResId();
            }
        }

        private Boolean updateStateOf(EventSection sectionPressed) {
            Boolean oldState = categoryStates.get(sectionPressed);
            Boolean newState = !oldState;
            categoryStates.put(sectionPressed, newState);
            return newState;
        }

        @Override
        public void onTabReselected(Tab tab) {
            onTabSelected(tab);
        }
    };

    public interface OnSectionPressedListener {
        OnSectionPressedListener NO_CALLBACK = new OnSectionPressedListener() {
            @Override
            public void onSectionPressed(EventSection section, boolean enabled) {
                Log.w("onSectionPressed() but no listener was set");
            }
        };

        void onSectionPressed(EventSection section, boolean enabled);
    }
}
