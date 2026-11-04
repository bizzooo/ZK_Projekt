import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';

const baseUrl = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/+$/, '');
const measuredLevels = [
  { tag: 'baseline-5-vu', start: 30, end: 90 },
  { tag: 'stress-10-vu', start: 120, end: 180 },
  { tag: 'stress-20-vu', start: 210, end: 270 },
  { tag: 'stress-40-vu', start: 300, end: 360 },
  { tag: 'stress-80-vu', start: 390, end: 450 },
  { tag: 'recovery-5-vu', start: 510, end: 570 },
];

const thresholds = {};
for (const { tag } of measuredLevels) {
  thresholds[`checks{load_level:${tag}}`] = ['rate>0.99'];
  thresholds[`http_req_duration{load_level:${tag}}`] = ['p(95)<1000'];
  thresholds[`http_req_failed{load_level:${tag}}`] = ['rate<0.01'];
}

export const options = {
  stages: [
    { duration: '30s', target: 5 },
    { duration: '1m', target: 5 },
    { duration: '30s', target: 10 },
    { duration: '1m', target: 10 },
    { duration: '30s', target: 20 },
    { duration: '1m', target: 20 },
    { duration: '30s', target: 40 },
    { duration: '1m', target: 40 },
    { duration: '30s', target: 80 },
    { duration: '1m', target: 80 },
    { duration: '30s', target: 0 },
    { duration: '30s', target: 5 },
    { duration: '1m', target: 5 },
  ],
  thresholds,
};

function currentLoadLevel() {
  const elapsedSeconds = exec.instance.currentTestRunDuration / 1000;
  return measuredLevels.find(
    ({ start, end }) => elapsedSeconds >= start && elapsedSeconds < end,
  )?.tag ?? 'ramp';
}

export default function () {
  const loadLevel = currentLoadLevel();
  const tags = { load_level: loadLevel };
  const response = http.get(
    `${baseUrl}/api/public/tournaments?page=0&size=20`,
    { tags },
  );
  let body;
  try {
    body = response.json();
  } catch {
    body = null;
  }

  check(response, {
    'catalog returns HTTP 200': (res) => res.status === 200,
  }, tags);
  check(body, {
    'catalog returns a paginated response': (payload) =>
      Array.isArray(payload?.data?.items) &&
      Number.isInteger(payload?.data?.page?.page) &&
      Number.isInteger(payload?.data?.page?.size) &&
      Number.isInteger(payload?.data?.page?.totalElements),
  }, tags);

  sleep(1);
}
