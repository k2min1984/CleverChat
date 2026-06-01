# 도메인 테이블 명명표준 정렬 매핑 (V20)

대상 23개 테이블 (auth-core/framework 제외)


## audit_log → tb_audit_log
| old | new | 비고 |
|-----|-----|------|
| id | audit_log_no |  |
| created_at | frst_reg_dt |  |

## chat_failure → tb_chat_failure
| old | new | 비고 |
|-----|-----|------|
| id | chat_failure_no |  |
| session_id | session_no |  |
| message_id | message_no |  |
| created_at | frst_reg_dt |  |

## chat_feedback → tb_chat_feedback
| old | new | 비고 |
|-----|-----|------|
| id | chat_feedback_no |  |
| message_id | message_no |  |
| created_at | frst_reg_dt |  |

## chat_message → tb_chat_message
| old | new | 비고 |
|-----|-----|------|
| id | chat_message_no |  |
| session_id | session_no |  |
| node_id | node_no |  |
| option_id | option_no |  |
| created_at | frst_reg_dt |  |

## chat_recommendation → tb_chat_recommendation
| old | new | 비고 |
|-----|-----|------|
| id | chat_recommendation_no |  |
| scenario_id | scenario_no |  |
| enabled | use_yn | boolean→char(1) Y/N 변환 |
| created_at | frst_reg_dt |  |
| updated_at | lst_chg_dt |  |

## chat_session → tb_chat_session
| old | new | 비고 |
|-----|-----|------|
| id | chat_session_no | uuid PK 유지 |
| anonymous_id | (유지) | 유지(테이블FK 아님) |
| user_id | user_no |  |
| scenario_id | scenario_no |  |
| version_id | version_no |  |
| current_node_id | current_node_no |  |
| created_at | frst_reg_dt |  |

## crawl_document → tb_crawl_document
| old | new | 비고 |
|-----|-----|------|
| id | crawl_document_no |  |
| target_id | target_no |  |
| created_at | frst_reg_dt |  |

## crawl_run_log → tb_crawl_run_log
| old | new | 비고 |
|-----|-----|------|
| id | crawl_run_log_no |  |
| target_id | target_no |  |
| document_id | document_no |  |
| created_at | frst_reg_dt |  |

## crawl_target → tb_crawl_target
| old | new | 비고 |
|-----|-----|------|
| id | crawl_target_no |  |
| enabled | use_yn | boolean→char(1) Y/N 변환 |
| created_by | frst_regr_empno | bigint(users FK) 유지 — 이름만 표준 |
| created_at | frst_reg_dt |  |
| updated_at | lst_chg_dt |  |
| (신규) | frst_regr_ip, lst_chgr_ip | IP 추적 컬럼 추가 |

## login_log → tb_login_log
| old | new | 비고 |
|-----|-----|------|
| id | login_log_no |  |
| created_at | frst_reg_dt |  |

## notice → tb_notice
| old | new | 비고 |
|-----|-----|------|
| id | notice_no |  |
| enabled | use_yn | boolean→char(1) Y/N 변환 |
| created_by | frst_regr_empno | bigint(users FK) 유지 — 이름만 표준 |
| updated_by | lst_chgr_empno | bigint(users FK) 유지 — 이름만 표준 |
| created_at | frst_reg_dt |  |
| updated_at | lst_chg_dt |  |
| (신규) | frst_regr_ip, lst_chgr_ip | IP 추적 컬럼 추가 |

## notification_channel → tb_notification_channel
| old | new | 비고 |
|-----|-----|------|
| id | notification_channel_no |  |
| enabled | use_yn | boolean→char(1) Y/N 변환 |
| created_by | frst_regr_empno | bigint(users FK) 유지 — 이름만 표준 |
| updated_by | lst_chgr_empno | bigint(users FK) 유지 — 이름만 표준 |
| created_at | frst_reg_dt |  |
| updated_at | lst_chg_dt |  |
| (신규) | frst_regr_ip, lst_chgr_ip | IP 추적 컬럼 추가 |

## notification_event → tb_notification_event
| old | new | 비고 |
|-----|-----|------|
| id | notification_event_no |  |
| created_at | frst_reg_dt |  |
| updated_at | lst_chg_dt |  |

## popular_query_daily → tb_popular_query_daily
| old | new | 비고 |
|-----|-----|------|
| top_result_scenario_id | top_result_scenario_no |  |
| created_at | frst_reg_dt |  |
| updated_at | lst_chg_dt |  |

## scenario → tb_scenario
| old | new | 비고 |
|-----|-----|------|
| id | scenario_no |  |
| category_id | category_no |  |
| active_version_id | active_version_no |  |
| created_at | frst_reg_dt |  |
| updated_at | lst_chg_dt |  |

## scenario_category → tb_scenario_category
| old | new | 비고 |
|-----|-----|------|
| id | scenario_category_no |  |
| parent_id | p_scenario_category_no |  |
| enabled | use_yn | boolean→char(1) Y/N 변환 |
| created_at | frst_reg_dt |  |
| updated_at | lst_chg_dt |  |

## scenario_keyword → tb_scenario_keyword
| old | new | 비고 |
|-----|-----|------|
| id | scenario_keyword_no |  |
| scenario_id | scenario_no |  |
| enabled | use_yn | boolean→char(1) Y/N 변환 |
| created_at | frst_reg_dt |  |
| updated_at | lst_chg_dt |  |

## scenario_node → tb_scenario_node
| old | new | 비고 |
|-----|-----|------|
| id | scenario_node_no |  |
| version_id | version_no |  |
| created_at | frst_reg_dt |  |
| updated_at | lst_chg_dt |  |

## scenario_node_option → tb_scenario_node_option
| old | new | 비고 |
|-----|-----|------|
| id | scenario_node_option_no |  |
| node_id | node_no |  |
| next_node_id | next_node_no |  |
| enabled | use_yn | boolean→char(1) Y/N 변환 |
| created_at | frst_reg_dt |  |
| updated_at | lst_chg_dt |  |

## scenario_synonym → tb_scenario_synonym
| old | new | 비고 |
|-----|-----|------|
| id | scenario_synonym_no |  |
| keyword_id | keyword_no |  |
| enabled | use_yn | boolean→char(1) Y/N 변환 |
| created_at | frst_reg_dt |  |
| updated_at | lst_chg_dt |  |

## scenario_version → tb_scenario_version
| old | new | 비고 |
|-----|-----|------|
| id | scenario_version_no |  |
| scenario_id | scenario_no |  |
| start_node_id | start_node_no |  |
| created_by | frst_regr_empno | bigint(users FK) 유지 — 이름만 표준 |
| created_at | frst_reg_dt |  |

## search_block_log → tb_search_block_log
| old | new | 비고 |
|-----|-----|------|
| id | search_block_log_no |  |
| user_id | user_no |  |
| created_at | frst_reg_dt |  |

## search_log → tb_search_log
| old | new | 비고 |
|-----|-----|------|
| id | search_log_no |  |
| top_scenario_id | top_scenario_no |  |
| user_id | user_no |  |
| created_at | frst_reg_dt |  |