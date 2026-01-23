/**
 * Latency 메트릭 수집 및 계산
 */
class MetricsCollector {
  constructor() {
    this.latencies = [];
  }

  /**
   * Latency 기록 (ms)
   */
  record(latencyMs) {
    this.latencies.push(latencyMs);
  }

  /**
   * 통계 계산
   */
  calculate() {
    if (this.latencies.length === 0) {
      return {
        count: 0,
        min: 0,
        max: 0,
        avg: 0,
        p50: 0,
        p95: 0,
        p99: 0,
      };
    }

    const sorted = [...this.latencies].sort((a, b) => a - b);
    const sum = sorted.reduce((acc, val) => acc + val, 0);

    return {
      count: sorted.length,
      min: sorted[0],
      max: sorted[sorted.length - 1],
      avg: sum / sorted.length,
      p50: this.percentile(sorted, 0.5),
      p95: this.percentile(sorted, 0.95),
      p99: this.percentile(sorted, 0.99),
    };
  }

  /**
   * Percentile 계산
   */
  percentile(sortedArray, p) {
    const index = Math.ceil(sortedArray.length * p) - 1;
    return sortedArray[Math.max(0, index)];
  }

  /**
   * 결과 출력
   */
  print(label) {
    const stats = this.calculate();
    console.log(`\n=== ${label} ===`);
    console.log(`Count: ${stats.count}`);
    console.log(`Min: ${stats.min.toFixed(2)}ms`);
    console.log(`Max: ${stats.max.toFixed(2)}ms`);
    console.log(`Avg: ${stats.avg.toFixed(2)}ms`);
    console.log(`P50: ${stats.p50.toFixed(2)}ms`);
    console.log(`P95: ${stats.p95.toFixed(2)}ms`);
    console.log(`P99: ${stats.p99.toFixed(2)}ms`);
  }

  /**
   * CSV 형식으로 출력
   */
  toCSV(label) {
    const stats = this.calculate();
    return `${label},${stats.count},${stats.min.toFixed(2)},${stats.max.toFixed(2)},${stats.avg.toFixed(2)},${stats.p50.toFixed(2)},${stats.p95.toFixed(2)},${stats.p99.toFixed(2)}`;
  }

  /**
   * 초기화
   */
  reset() {
    this.latencies = [];
  }
}

module.exports = { MetricsCollector };
