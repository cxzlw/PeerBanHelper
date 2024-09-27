package com.ghostchu.peerbanhelper.downloader.impl.bitcomet.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class BCLoginResponse {

    @SerializedName("error_code")
    private String errorCode;
    @SerializedName("error_message")
    private String errorMessage;
    @SerializedName("invite_token")
    private String inviteToken;
    @SerializedName("version")
    private String version;
}