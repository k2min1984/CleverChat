package kr.co.cleverchat.domain.adminmanage.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IpCidrMatcherTest {

    @Test
    void matchesSingleIpv4Address() {
        assertThat(IpCidrMatcher.matches("203.0.113.10", "203.0.113.10")).isTrue();
        assertThat(IpCidrMatcher.matches("203.0.113.10", "203.0.113.11")).isFalse();
    }

    @Test
    void matchesIpv4CidrRange() {
        assertThat(IpCidrMatcher.matches("203.0.113.0/24", "203.0.113.200")).isTrue();
        assertThat(IpCidrMatcher.matches("203.0.113.0/24", "203.0.114.1")).isFalse();
    }

    @Test
    void matchesIpv6SingleAndCidr() {
        assertThat(IpCidrMatcher.matches("2001:db8::1", "2001:db8:0:0:0:0:0:1")).isTrue();
        assertThat(IpCidrMatcher.matches("2001:db8::/32", "2001:db8:abcd::1")).isTrue();
        assertThat(IpCidrMatcher.matches("2001:db8::/32", "2001:db9::1")).isFalse();
    }

    @Test
    void safelyFailsInvalidRules() {
        assertThat(IpCidrMatcher.matches("bad-rule", "203.0.113.10")).isFalse();
        assertThat(IpCidrMatcher.matches("203.0.113.0/99", "203.0.113.10")).isFalse();
        assertThat(IpCidrMatcher.matches("203.0.113.0/nope", "203.0.113.10")).isFalse();
        assertThat(IpCidrMatcher.matches("203.0.113.0/24", "2001:db8::1")).isFalse();
    }

    @Test
    void validatesRules() {
        assertThat(IpCidrMatcher.isValidRule("203.0.113.10")).isTrue();
        assertThat(IpCidrMatcher.isValidRule("203.0.113.0/24")).isTrue();
        assertThat(IpCidrMatcher.isValidRule("2001:db8::/32")).isTrue();
        assertThat(IpCidrMatcher.isValidRule("203.0.113.0/33")).isFalse();
        assertThat(IpCidrMatcher.isValidRule("not-ip")).isFalse();
    }
}
