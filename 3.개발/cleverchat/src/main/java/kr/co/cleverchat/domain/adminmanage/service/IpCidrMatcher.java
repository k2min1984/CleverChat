package kr.co.cleverchat.domain.adminmanage.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public final class IpCidrMatcher {

    private IpCidrMatcher() {}

    public static boolean matches(String ipCidrRule, String clientIp) {
        if (ipCidrRule == null || ipCidrRule.isBlank() || clientIp == null || clientIp.isBlank()) {
            return false;
        }
        String rule = ipCidrRule.trim();
        if (!rule.contains("/")) {
            return normalizedBytes(rule).equals(normalizedBytes(clientIp));
        }
        String[] parts = rule.split("/", -1);
        if (parts.length != 2 || parts[1].isBlank()) {
            return false;
        }
        try {
            byte[] ruleBytes = InetAddress.getByName(parts[0].trim()).getAddress();
            byte[] clientBytes = InetAddress.getByName(clientIp.trim()).getAddress();
            if (ruleBytes.length != clientBytes.length) {
                return false;
            }
            int prefixLength = Integer.parseInt(parts[1].trim());
            if (prefixLength < 0 || prefixLength > ruleBytes.length * 8) {
                return false;
            }
            return prefixMatches(ruleBytes, clientBytes, prefixLength);
        } catch (IllegalArgumentException | UnknownHostException e) {
            return false;
        }
    }

    public static boolean isValidRule(String ipCidrRule) {
        if (ipCidrRule == null || ipCidrRule.isBlank()) {
            return false;
        }
        String rule = ipCidrRule.trim();
        if (!rule.contains("/")) {
            return normalizedBytes(rule).length() > 0;
        }
        String[] parts = rule.split("/", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return false;
        }
        try {
            byte[] addressBytes = InetAddress.getByName(parts[0].trim()).getAddress();
            int prefixLength = Integer.parseInt(parts[1].trim());
            return prefixLength >= 0 && prefixLength <= addressBytes.length * 8;
        } catch (IllegalArgumentException | UnknownHostException e) {
            return false;
        }
    }

    private static boolean prefixMatches(byte[] ruleBytes, byte[] clientBytes, int prefixLength) {
        int fullBytes = prefixLength / 8;
        int remainingBits = prefixLength % 8;
        for (int i = 0; i < fullBytes; i++) {
            if (ruleBytes[i] != clientBytes[i]) {
                return false;
            }
        }
        if (remainingBits == 0) {
            return true;
        }
        int mask = 0xFF << (8 - remainingBits);
        return (ruleBytes[fullBytes] & mask) == (clientBytes[fullBytes] & mask);
    }

    private static NormalizedAddress normalizedBytes(String ip) {
        try {
            return new NormalizedAddress(InetAddress.getByName(ip.trim()).getAddress());
        } catch (RuntimeException | UnknownHostException e) {
            return new NormalizedAddress(new byte[0]);
        }
    }

    private record NormalizedAddress(byte[] bytes) {
        @Override
        public boolean equals(Object other) {
            return other instanceof NormalizedAddress address
                    && bytes.length > 0
                    && Arrays.equals(bytes, address.bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }

        int length() {
            return bytes.length;
        }
    }
}
