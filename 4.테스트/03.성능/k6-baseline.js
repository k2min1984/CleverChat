import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    chat_baseline: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 10),
      duration: __ENV.DURATION || '2m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1000'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const start = http.post(`${baseUrl}/chat/api/sessions`, JSON.stringify({ scenarioId: Number(__ENV.SCENARIO_ID || 1) }), {
    headers: { 'Content-Type': 'application/json' },
  });
  check(start, {
    'session start is 2xx or controlled 4xx': (r) => r.status < 500,
  });

  const search = http.get(`${baseUrl}/admin/api/search/logs?size=20`, {
    headers: adminHeaders(),
  });
  check(search, {
    'search test is not server error': (r) => r.status < 500,
  });

  const audit = http.get(`${baseUrl}/admin/api/audit-logs?limit=20`, {
    headers: adminHeaders(),
  });
  check(audit, {
    'audit list is not server error': (r) => r.status < 500,
  });

  sleep(1);
}

function adminHeaders() {
  const cookie = __ENV.ADMIN_COOKIE || '';
  return cookie ? { Cookie: cookie } : {};
}
