package com.example.support.dto;

import com.example.support.model.*;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MetadataDto {
    private Source source;
    private String browser;
    @JsonProperty("device_type") private DeviceType deviceType;

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }
    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }
    public DeviceType getDeviceType() { return deviceType; }
    public void setDeviceType(DeviceType deviceType) { this.deviceType = deviceType; }
}
