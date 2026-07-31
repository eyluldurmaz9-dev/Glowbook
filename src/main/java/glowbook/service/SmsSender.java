package glowbook.service;

public interface SmsSender {

    void sendSms(String phone, String message);
}
