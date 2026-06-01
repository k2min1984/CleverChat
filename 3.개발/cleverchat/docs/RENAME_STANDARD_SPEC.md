# CleverChat DB 명명표준 정렬 — 전체 리네임 스펙 (단일 기준 문서)

이 문서는 OverseasNPP 표준에 맞춰 **도메인 23개 테이블**을 풀 리네임하는 작업의 기준이다.
DB는 `V20__naming_standard_alignment.sql`로 적용된다. 소스/프론트는 본 맵대로 모두 정렬한다.

## 절대 건드리지 않는 것 (범위 외)
- 테이블: `tb_auth`, `tb_auth_menu_adm`, `tb_code`, `tb_menu` (이미 표준)
- 프레임워크: `flyway_schema_history`, `spring_session`, `spring_session_attributes`
- 인증핵심(2차 회차): `users`, `roles`, `user_roles`, `admin_code`, `admin_menu`, `admin_role_menu`
  → 이 테이블 컬럼/자바필드/매퍼/JS는 그대로 둔다. (단, 도메인 테이블의 `created_by`등 users FK는 아래대로 이름만 변경, 타입 bigint·FK 유지)

## 핵심 변환 규칙
1. **DB 컬럼명**: 아래 표의 new 값으로. (V20이 이미 처리)
2. **자바 필드명**: new 컬럼의 camelCase. 영속 model POJO는 **이미 리네임 완료** — 사용처만 고친다.
3. **MyBatis `#{}`**: insert/update의 엔티티 프로퍼티 바인딩과 `keyProperty`는 새 자바 필드명으로. finder 메서드 인자(@Param/메서드 arg) 바인딩은 인자명 그대로 둬도 됨(컬럼만 new).
4. **SELECT**: `map-underscore-to-camel-case=true`이므로 new 컬럼명을 그대로 SELECT하면 new 자바필드에 자동매핑. 별도 alias 불필요. 기존 `AS category_name`처럼 이름 안 바뀐 alias는 유지.
5. **enabled → use_yn (boolean→String 'Y'/'N')**:
   - 자바: 필드 `String useYn`, `getUseYn()/setUseYn(String)`. `if(x.isEnabled())` → `if("Y".equals(x.getUseYn()))`. `x.setEnabled(true)` → `x.setUseYn("Y")`. 메서드레퍼런스 `X::isEnabled` → 람다 `x -> "Y".equals(x.getUseYn())`.
   - SQL: `enabled = TRUE` → `use_yn = 'Y'`. insert VALUES `#{enabled}` → `#{useYn}`.
   - JS: 응답의 `obj.enabled`(boolean) → `obj.useYn`(문자 'Y'/'N'). `op.enabled !== false` → `op.useYn !== 'N'`. 체크박스 checked→값 변환 시 `chk ? 'Y' : 'N'` 전송.
6. **프론트 JS / 템플릿**: JSON 응답 필드와 Thymeleaf `${x.id}` 등 모두 new 자바필드명으로. (`data.id`→`data.scenarioNo`, `op.enabled`→`op.useYn`)
7. **createdBy/updatedBy** (도메인 테이블의 users FK): 컬럼 `frst_regr_empno`/`lst_chgr_empno`, 자바 `frstRegrEmpno`/`lstChgrEmpno`. **타입은 Long 유지**(users.id FK). 단 `scenario_version.created_by`는 원래 VARCHAR(username)이라 String 유지.

## 테이블별 매핑 (old → new : 컬럼 / 자바필드)

### `audit_log` → `tb_audit_log`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | audit_log_no | id | auditLogNo |  |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |

### `chat_failure` → `tb_chat_failure`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | chat_failure_no | id | chatFailureNo |  |
| session_id | session_no | sessionId | sessionNo |  |
| message_id | message_no | messageId | messageNo |  |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |

### `chat_feedback` → `tb_chat_feedback`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | chat_feedback_no | id | chatFeedbackNo |  |
| message_id | message_no | messageId | messageNo |  |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |

### `chat_message` → `tb_chat_message`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | chat_message_no | id | chatMessageNo |  |
| session_id | session_no | sessionId | sessionNo |  |
| node_id | node_no | nodeId | nodeNo |  |
| option_id | option_no | optionId | optionNo |  |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |

### `chat_recommendation` → `tb_chat_recommendation`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | chat_recommendation_no | id | chatRecommendationNo |  |
| scenario_id | scenario_no | scenarioId | scenarioNo |  |
| enabled | use_yn | enabled | useYn | boolean→String Y/N |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |
| updated_at | lst_chg_dt | updatedAt | lstChgDt |  |

### `chat_session` → `tb_chat_session`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | chat_session_no | id | chatSessionNo | uuid PK 유지 |
| user_id | user_no | userId | userNo |  |
| scenario_id | scenario_no | scenarioId | scenarioNo |  |
| version_id | version_no | versionId | versionNo |  |
| current_node_id | current_node_no | currentNodeId | currentNodeNo |  |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |

### `crawl_document` → `tb_crawl_document`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | crawl_document_no | id | crawlDocumentNo |  |
| target_id | target_no | targetId | targetNo |  |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |

### `crawl_run_log` → `tb_crawl_run_log`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | crawl_run_log_no | id | crawlRunLogNo |  |
| target_id | target_no | targetId | targetNo |  |
| document_id | document_no | documentId | documentNo |  |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |

### `crawl_target` → `tb_crawl_target`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | crawl_target_no | id | crawlTargetNo |  |
| enabled | use_yn | enabled | useYn | boolean→String Y/N |
| created_by | frst_regr_empno | createdBy | frstRegrEmpno | Long 유지(users FK) |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |
| updated_at | lst_chg_dt | updatedAt | lstChgDt |  |

### `login_log` → `tb_login_log`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | login_log_no | id | loginLogNo |  |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |

### `notice` → `tb_notice`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | notice_no | id | noticeNo |  |
| enabled | use_yn | enabled | useYn | boolean→String Y/N |
| created_by | frst_regr_empno | createdBy | frstRegrEmpno | Long 유지(users FK) |
| updated_by | lst_chgr_empno | updatedBy | lstChgrEmpno | Long 유지(users FK) |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |
| updated_at | lst_chg_dt | updatedAt | lstChgDt |  |

### `notification_channel` → `tb_notification_channel`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | notification_channel_no | id | notificationChannelNo |  |
| enabled | use_yn | enabled | useYn | boolean→String Y/N |
| created_by | frst_regr_empno | createdBy | frstRegrEmpno | Long 유지(users FK) |
| updated_by | lst_chgr_empno | updatedBy | lstChgrEmpno | Long 유지(users FK) |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |
| updated_at | lst_chg_dt | updatedAt | lstChgDt |  |

### `notification_event` → `tb_notification_event`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | notification_event_no | id | notificationEventNo |  |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |
| updated_at | lst_chg_dt | updatedAt | lstChgDt |  |

### `popular_query_daily` → `tb_popular_query_daily`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| top_result_scenario_id | top_result_scenario_no | topResultScenarioId | topResultScenarioNo |  |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |
| updated_at | lst_chg_dt | updatedAt | lstChgDt |  |

### `scenario` → `tb_scenario`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | scenario_no | id | scenarioNo |  |
| category_id | category_no | categoryId | categoryNo |  |
| active_version_id | active_version_no | activeVersionId | activeVersionNo |  |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |
| updated_at | lst_chg_dt | updatedAt | lstChgDt |  |

### `scenario_category` → `tb_scenario_category`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | scenario_category_no | id | scenarioCategoryNo |  |
| parent_id | p_scenario_category_no | parentId | pScenarioCategoryNo |  |
| enabled | use_yn | enabled | useYn | boolean→String Y/N |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |
| updated_at | lst_chg_dt | updatedAt | lstChgDt |  |

### `scenario_keyword` → `tb_scenario_keyword`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | scenario_keyword_no | id | scenarioKeywordNo |  |
| scenario_id | scenario_no | scenarioId | scenarioNo |  |
| enabled | use_yn | enabled | useYn | boolean→String Y/N |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |
| updated_at | lst_chg_dt | updatedAt | lstChgDt |  |

### `scenario_node` → `tb_scenario_node`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | scenario_node_no | id | scenarioNodeNo |  |
| version_id | version_no | versionId | versionNo |  |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |
| updated_at | lst_chg_dt | updatedAt | lstChgDt |  |

### `scenario_node_option` → `tb_scenario_node_option`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | scenario_node_option_no | id | scenarioNodeOptionNo |  |
| node_id | node_no | nodeId | nodeNo |  |
| next_node_id | next_node_no | nextNodeId | nextNodeNo |  |
| enabled | use_yn | enabled | useYn | boolean→String Y/N |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |
| updated_at | lst_chg_dt | updatedAt | lstChgDt |  |

### `scenario_synonym` → `tb_scenario_synonym`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | scenario_synonym_no | id | scenarioSynonymNo |  |
| keyword_id | keyword_no | keywordId | keywordNo |  |
| enabled | use_yn | enabled | useYn | boolean→String Y/N |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |
| updated_at | lst_chg_dt | updatedAt | lstChgDt |  |

### `scenario_version` → `tb_scenario_version`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | scenario_version_no | id | scenarioVersionNo |  |
| scenario_id | scenario_no | scenarioId | scenarioNo |  |
| start_node_id | start_node_no | startNodeId | startNodeNo |  |
| created_by | frst_regr_empno | createdBy | frstRegrEmpno | String 유지(username) |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |

### `search_block_log` → `tb_search_block_log`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | search_block_log_no | id | searchBlockLogNo |  |
| user_id | user_no | userId | userNo |  |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |

### `search_log` → `tb_search_log`
| old col | new col | old javaField | new javaField | note |
|---|---|---|---|---|
| id | search_log_no | id | searchLogNo |  |
| top_scenario_id | top_scenario_no | topScenarioId | topScenarioNo |  |
| user_id | user_no | userId | userNo |  |
| created_at | frst_reg_dt | createdAt | frstRegDt |  |
