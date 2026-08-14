package glowbook.service;

import glowbook.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class TurkishPhoneNumberService {

    public String normalize(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new BusinessException("Geçerli bir telefon numarası girilmelidir.");
        }

        String digits = rawPhone.replaceAll("[^0-9]", "");
        if (digits.startsWith("0090")) digits = digits.substring(2);
        if (digits.startsWith("0") && digits.length() == 11) digits = "90" + digits.substring(1);
        if (digits.startsWith("5") && digits.length() == 10) digits = "90" + digits;

        if (!digits.matches("905\\d{9}")) {
            throw new BusinessException("Geçerli bir Türkiye cep telefonu numarası girilmelidir.");
        }
        return "+" + digits;
    }
}
