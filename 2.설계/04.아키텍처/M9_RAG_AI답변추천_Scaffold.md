# M9 RAG/AI 답변 추천 Scaffold

## 1차 기준

- M9 1차는 실제 AI provider 연동이 아니라 provider-neutral scaffold다.
- 기본값은 `cleverchat.ai.answer-suggestion.enabled=false`이며, 꺼져 있으면 기존 M4 검색 fallback 메시지를 그대로 사용한다.
- 외부 LLM 호출, API key, pgvector, embedding 생성, 벡터 검색, 관리자 AI 설정 화면은 후속 범위다.

## Runtime 연결

- M3 자유 텍스트 직접 매칭 실패 후 M4 검색 결과가 있을 때만 AI suggestion service를 호출할 수 있다.
- 검색 결과가 없으면 AI provider를 호출하지 않고 기존 `NO_MATCH` 실패 큐 흐름을 유지한다.
- provider가 빈 응답, `generated=false`, 예외를 반환하면 기존 검색 fallback 메시지로 degrade 한다.
- `/chat/api/**` public 응답 구조와 `ApiResponse` envelope는 변경하지 않는다.

## 보안 기준

- prompt 입력은 기존 `ChatPiiGuard`를 지난 safe text만 사용한다.
- provider scaffold는 request body 원문, 개인정보 원문, API key, provider secret을 로그나 DB에 저장하지 않는다.
- 실제 provider 연동 전에는 timeout, rate limit, citation 기반 답변, 운영자 enable switch, provider별 민감정보 처리 약관을 별도 승인한다.

## 후속 범위

- OpenAI/LLM provider adapter
- pgvector embedding schema와 backfill batch
- 관리자 AI 설정 화면
- 답변 품질 평가, hallucination 방지 정책, citation UI
