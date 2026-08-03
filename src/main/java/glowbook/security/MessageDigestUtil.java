package glowbook.security;

import java.security.MessageDigest;

final class MessageDigestUtil {

    private MessageDigestUtil() {
    }

    static boolean equals(byte[] first, byte[] second) {
        return MessageDigest.isEqual(first, second);
    }
}
