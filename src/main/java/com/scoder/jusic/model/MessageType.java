package com.scoder.jusic.model;

/**
 * @author H
 */

public enum MessageType {
    /**
     *
     */
    NOTICE("NOTICE"),
    ONLINE("ONLINE"),
    SETTING_NAME("SETTING_NAME"),
    AUTH("AUTH"),
    AUTH_ROOT("AUTH_ROOT"),
    AUTH_ADMIN("AUTH_ADMIN"),
    MUSIC("MUSIC"),
    PICK("PICK"),
    CHAT("CHAT"),
    SEARCH("SEARCH"),
    SEARCH_PICTURE("SEARCH_PICTURE"),
    SEARCH_HOUSE("SEARCH_HOUSE"),
    SEARCH_SONGLIST("SEARCH_SONGLIST"),
    SEARCH_USER("SEARCH_USER"),
    ENTER_HOUSE("ENTER_HOUSE"),
    ENTER_HOUSE_START("ENTER_HOUSE_START"),
    ADD_HOUSE("ADD_HOUSE"),
    ADD_HOUSE_START("ADD_HOUSE_START"),
    HOUSE_USER("HOUSE_USER"),
    SESSION_INFO("SESSION_INFO"),
    ANNOUNCEMENT("ANNOUNCEMENT"),
    CIRCLEMODEL("CIRCLEMODEL"),
    LISTMODEL("LISTMODEL"),
    RANDOMMODEL("RANDOMMODEL"),
    AV_SIGNAL("AV_SIGNAL"),
    AV_PLAYBACK_STATE("AV_PLAYBACK_STATE"),
    PAUSE("PAUSE"),
    VOLUMN("VOLUMN"),
    GOODMODEL("GOODMODEL");

    MessageType(String type) {
        this.type = type;
    }

    private String type;

    public String type() {
        return this.type;
    }
}
