package dev.vlab.tweetsms.apiclient;

import dev.vlab.tweetsms.helper.UrlContainer;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;


public interface ApiInterface {


    @FormUrlEncoded
    @POST("login")
    Call<String> sendLoginRequest(
            @Field("username") String username,
            @Field("password") String password
    );

   @FormUrlEncoded
    @POST("password/email")
    Call<String> sendForgotPasswordRequest(
            @Field("value") String username
    );

   @FormUrlEncoded
    @GET("resend-verify/email")
    Call<String> resendOTP();

   @FormUrlEncoded
    @POST("password/verify-code")
    Call<String> otpVerificationRequest(
        @Field("email") String email,
        @Field("code") String otp

    );
   @FormUrlEncoded
    @POST("password/reset")
    Call<String> resetPassword(
        @Field("email") String email,
        @Field("token") String otp,
        @Field("password") String password,
        @Field("password_confirmation") String confirmPassword

    );




    @FormUrlEncoded
    @POST("message/received")
    Call<String> uploadMessage(
            @Header("Authorization") String token,
            @Field("device_id") String deviceId,
            @Field("mobile_number") String sender,
            @Field("message") String message,
            @Field("device_slot_name") String simSlotName,
            @Field("device_slot_number") String simSlotNumber
    );


    @FormUrlEncoded
    @POST("logout")
    Call<String> sendLogOutRequest(@Header("Authorization") String token, @Field("device_id") String deviceId);

    @FormUrlEncoded
    @POST("add-device")
    Call<String> sendLoginRequestWithQr(
            @Header("Authorization") String token,
            @Field("scan_data") String scanData,
            @Field("device_id") String deviceId,
            @Field("device_name") String deviceName,
            @Field("device_model") String deviceModel,
            @Field("android_version") String androidVersion,
            @Field("app_version") String appVersion,
            @Field("sim") String simInfo
    );


    //sim info: 1**grameenphone***2**robi
    @FormUrlEncoded
    @POST("message/update/{message_id}")
    Call<String> updateMessageStatus(
            @Header("Authorization") String token,
            @Path("message_id") String messageId,
            @Field("status") String status,
            @Field("error_code") String errorCode);
}
