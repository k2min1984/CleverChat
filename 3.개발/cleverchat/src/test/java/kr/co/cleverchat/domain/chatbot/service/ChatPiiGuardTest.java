package kr.co.cleverchat.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChatPiiGuardTest {

    private final ChatPiiGuard guard = new ChatPiiGuard();

    @Test
    void detectsEmailPhoneResidentNumberAndCardNumber() {
        assertThat(
                        guard.detectTypes(
                                "email test@example.com phone 010-1234-5678 rrn 900101-1234567 card 4111-1111-1111-1111"))
                .containsExactly("EMAIL", "PHONE", "RRN", "CARD");
    }

    @Test
    void returnsEmptyForNormalQuestion() {
        assertThat(guard.detectTypes("shipping question")).isEmpty();
    }

    @Test
    void masksEmailAndPhone() {
        assertThat(guard.maskLowRisk("email test@example.com phone 010-1234-5678"))
                .isEqualTo("email te***@example.com phone 010-****-**78");
    }

    @Test
    void luhnInvalidNumberIsNotCardPii() {
        assertThat(guard.detectTypes("card 4111-1111-1111-1112")).isEmpty();
    }

    @Test
    void highRiskTypesAreRrnAndCardOnly() {
        assertThat(guard.hasHighRiskTypes(java.util.List.of("EMAIL", "PHONE"))).isFalse();
        assertThat(guard.hasHighRiskTypes(java.util.List.of("CARD"))).isTrue();
        assertThat(guard.hasHighRiskTypes(java.util.List.of("RRN"))).isTrue();
    }
}
