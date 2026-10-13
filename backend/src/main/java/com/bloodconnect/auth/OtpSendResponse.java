package com.bloodconnect.auth;


public record OtpSendResponse(OtpChannel channel, String destination, long expiresInSeconds,
                              long resendInSeconds, String devCode) {
}
