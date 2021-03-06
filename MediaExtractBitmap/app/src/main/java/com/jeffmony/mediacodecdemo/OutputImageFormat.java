package com.jeffmony.mediacodecdemo;

public enum OutputImageFormat {

    I420("I420"),
    NV21("NV21"),
    JPEG("JPEG");
    private String friendlyName;

    private OutputImageFormat(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String toString() {
        return friendlyName;
    }
}
