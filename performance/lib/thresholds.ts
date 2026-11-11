/**
 * Thresholds als pass/fail voor k6. De smoke-run is een functionele rooktest: elk falend verzoek faalt
 * de run, de latency-grens is bewust ruim. Echte performancedrempels volgen uit de baseline (#428).
 */
export const smokeThresholds: Record<string, string[]> = {
  http_req_failed: ["rate==0"],
  checks: ["rate==1"],
  http_req_duration: ["p(95)<2000"],
};
