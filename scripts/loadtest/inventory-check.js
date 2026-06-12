import http from 'k6/http';
import { check, sleep } from 'k6';
import { config } from './config.js';

export const options = {
  stages: [
    { duration: '30s', target: 30 },
    { duration: '1m', target: 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<100'],
    http_req_failed: ['rate<0.001'],
  },
};

export default function () {
  const headers = {
    'Content-Type': 'application/json',
  };

  // Check inventory (hot path - should be fast)
  const checkRes = http.post(`${config.baseURL}/api/v1/inventory/check`, JSON.stringify({
    skuId: 'SKU-001',
    quantity: 1,
  }), { headers });

  check(checkRes, { 'inventory check fast': (r) => r.status === 200 && r.timings.duration < 100 });

  sleep(0.1);
}
