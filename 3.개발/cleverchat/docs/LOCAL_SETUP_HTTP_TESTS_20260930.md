# CLEVERCHAT 실제 HTTP 시나리오 결과

실행시각: 2026-09-30T00:52:26.122928

| 항목 | 결과 | 상세 |
|---|---|---|
| 관리자 실제 로그인 | 통과 | {"http": 200, "path": "/admin"} |
| 관리자 익명 접근 차단 | 통과 | 302 |
| GET /admin | 통과 | {"http": 200} |
| GET /admin/audit-logs | 통과 | {"http": 200} |
| GET /admin/chat/failures | 통과 | {"http": 200} |
| GET /admin/chat/feedback | 통과 | {"http": 200} |
| GET /admin/chat/recommendations | 통과 | {"http": 200} |
| GET /admin/chat/sessions | 통과 | {"http": 200} |
| GET /admin/crawl-targets | 통과 | {"http": 200} |
| GET /admin/manage/codes | 통과 | {"http": 200} |
| GET /admin/manage/ip-whitelist | 통과 | {"http": 200} |
| GET /admin/manage/menus | 통과 | {"http": 200} |
| GET /admin/manage/permissions | 통과 | {"http": 200} |
| GET /admin/notices | 통과 | {"http": 200} |
| GET /admin/notifications | 통과 | {"http": 200} |
| GET /admin/scenarios | 통과 | {"http": 200} |
| GET /admin/scenarios/order | 통과 | {"http": 200} |
| GET /admin/search/blocks | 통과 | {"http": 200} |
| GET /admin/search/logs | 통과 | {"http": 200} |
| GET /admin/search/popular | 통과 | {"http": 200} |
| GET /admin/statistics | 통과 | {"http": 200} |
| GET /admin/system-settings | 통과 | {"http": 200} |
| GET /admin/api/scenario-categories | 통과 | {"http": 200} |
| POST /admin/api/scenarios | 통과 | {"http": 400} |
| GET /admin/api/scenarios | 통과 | {"http": 200} |
| POST /admin/api/scenarios | 통과 | {"http": 200} |
| POST /admin/api/scenarios/2/versions | 통과 | {"http": 200} |
| POST /admin/api/scenarios/versions/3/publish | 통과 | {"http": 400} |
| POST /admin/api/scenarios/1/versions | 통과 | {"http": 200} |
| GET /admin/scenarios/1/versions/4/graph | 통과 | {"http": 200} |
| PUT /admin/api/scenarios/versions/4/graph | 통과 | {"http": 200} |
| GET /admin/api/scenarios/versions/4/graph | 통과 | {"http": 200} |
| PUT /admin/api/scenarios/versions/4/graph | 통과 | {"http": 403} |
| POST /admin/api/scenarios/versions/4/publish | 통과 | {"http": 200} |
| POST /admin/api/scenarios/1/activate/4 | 통과 | {"http": 200} |
| PUT /admin/api/scenarios/versions/4/graph | 통과 | {"http": 409} |
| DELETE /admin/api/scenarios/1 | 통과 | {"http": 409} |
| PUT /admin/api/scenarios/1/keywords | 통과 | {"http": 200} |
| GET /chat/api/scenarios | 통과 | {"http": 200} |
| POST /chat/api/sessions | 통과 | {"http": 200} |
| 시작 노드 및 선택지 | 통과 | {"state": "ACTIVE", "options": 1} |
| POST /chat/api/sessions/7f198273-eb11-485a-8590-4c54b4d8a0c2/select-option | 통과 | {"http": 200} |
| 선택지 답변 및 링크 | 통과 | {"messages": 3, "canGoBack": true} |
| POST /chat/api/messages/25/feedback | 통과 | {"http": 200} |
| POST /chat/api/sessions/7f198273-eb11-485a-8590-4c54b4d8a0c2/back | 통과 | {"http": 200} |
| 대화 뒤로가기 | 통과 | {"options": 1} |
| POST /chat/api/sessions/7f198273-eb11-485a-8590-4c54b4d8a0c2/select-option | 통과 | {"http": 200} |
| POST /chat/api/sessions/7f198273-eb11-485a-8590-4c54b4d8a0c2/select-option | 통과 | {"http": 200} |
| 종료 노드 | 통과 | {"state": "COMPLETED"} |
| GET /chat/api/sessions/7f198273-eb11-485a-8590-4c54b4d8a0c2/history | 통과 | {"http": 200} |
| GET /chat/api/sessions/7f198273-eb11-485a-8590-4c54b4d8a0c2 | 통과 | {"http": 403} |
| POST /chat/api/sessions/auto | 통과 | {"http": 200} |
| 자유입력 검색에서 시나리오 후보 | 통과 | {"candidates": [{"crawlDocumentNo": null, "scenarioNo": 1, "scenarioNodeNo": null, "label": "로컬 설치 확인 안내", "matchedField": "SCENARIO", "optionType": "SCENARIO"}]} |
| POST /chat/api/sessions/1bb3528f-8bea-491d-b407-9caa939d4f9a/select-search-result | 통과 | {"http": 200} |
| 검색 후보에서 상담 전환 | 통과 | {"scenarioId": 1} |
| GET /admin/api/chat/sessions | 통과 | {"http": 200} |
| GET /admin/api/chat/feedback | 통과 | {"http": 200} |