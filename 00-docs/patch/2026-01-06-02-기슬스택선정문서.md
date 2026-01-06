# 아키텍처 설계

## 기술 스택

### 프론트엔드

- **프레임워크**: React + TypeScript
- **최소한의 기능만 구현**: 백엔드 서버 테스트 및 검증용

### 백엔드

- **언어/프레임워크**: Kotlin Spring Boot
- **추가 검토 중**: Go, NestJS (TypeScript)

### 데이터베이스

- **RDBMS**: MySQL (샤딩/파티셔닝)
- **Redis**: 캐싱, 세션 관리, 실시간 메시지 큐
- **Elasticsearch**: 메시지 검색

### 메시징/이벤트

- **Kafka**: 이벤트 스트리밍, 메시지 큐

### 인프라

- **Kubernetes**: 컨테이너 오케스트레이션
- **모니터링**: Prometheus + Grafana + Loki

### 테스트

- **부하테스트**: k6
- **스트레스 테스트**: k6

## 전체 시스템 구조

### 프론트엔드 애플리케이션

- 최소한의 채팅 UI 구현
- API Gateway를 통한 백엔드 통신
- WebSocket을 통한 실시간 메시지 수신

## 마이크로서비스 구조

### 1. API Gateway

- 모든 클라이언트 요청의 진입점
- 라우팅, 인증, 로드 밸런싱
- **언어/프레임워크**: Kotlin Spring Boot (Go 검토 중)

### 2. User Service

- 사용자 인증/인가
- 사용자 정보 관리
- **언어/프레임워크**: Kotlin Spring Boot
- **DB**: RDBMS (샤딩 가능)

### 3. Chat Room Service

- 채팅방 생성/수정/삭제
- 채팅방 멤버 관리
- **언어/프레임워크**: Kotlin Spring Boot
- **DB**: RDBMS (샤딩 가능)

### 4. Message Service

- 메시지 전송/수신
- 메시지 저장
- **언어/프레임워크**: Kotlin Spring Boot (Go 검토 중)
- **DB**: RDBMS (샤딩/파티셔닝 - 메시지 데이터는 시간/채팅방 기준으로 파티셔닝)
- **Kafka**: 메시지 이벤트 발행
- **Redis**: 실시간 메시지 캐싱

### 5. Search Service

- 메시지 검색
- **언어/프레임워크**: Kotlin Spring Boot (NestJS 검토 중)
- **Elasticsearch**: 메시지 인덱싱 및 검색
- **Kafka**: 메시지 이벤트 구독하여 Elasticsearch에 인덱싱

### 6. Notification Service

- 푸시 알림
- 알림 설정 관리
- **언어/프레임워크**: Kotlin Spring Boot (NestJS 검토 중)
- **Kafka**: 이벤트 구독

### 7. WebSocket Service (선택)

- 실시간 메시지 전송
- WebSocket 연결 관리
- **언어/프레임워크**: Kotlin Spring Boot (Go 검토 중)
- **Redis**: Pub/Sub을 통한 메시지 브로드캐스팅

## 데이터 흐름

### 메시지 전송 흐름

1. 프론트엔드 → API Gateway → Message Service
2. Message Service → RDBMS (메시지 저장, 파티셔닝)
3. Message Service → Kafka (메시지 이벤트 발행)
4. Kafka → Search Service → Elasticsearch (인덱싱)
5. Kafka → Notification Service (알림 처리)
6. Message Service → Redis (실시간 캐싱)
7. Redis Pub/Sub → WebSocket Service → 프론트엔드 (실시간 전송)

## 데이터베이스 운영 전략

### Database per Service 패턴

- **적용 범위**: 모든 데이터 저장소에 적용 (RDBMS, Redis, NoSQL 등)
- 각 마이크로서비스마다 독립적인 데이터 저장소 인스턴스 운영
- 서비스 간 데이터 결합도 최소화
- 서비스별 독립적인 스케일링 가능
- 장애 격리: 한 서비스의 데이터 저장소 장애가 다른 서비스에 영향 없음
- 서비스별로 다른 샤딩/파티셔닝 전략 적용 가능
- 데이터 일관성: 서비스 간 데이터 동기화는 이벤트 기반(Kafka)으로 처리

### Redis 운영 전략

- **서비스별 Redis 분리 (기본 원칙)**
  - 각 서비스마다 독립적인 Redis 인스턴스 운영
  - 서비스별 캐싱 전략 독립적으로 관리
  - 장애 격리 및 독립적인 스케일링 가능
- **공유 Redis (선택적)**
  - 세션 관리: 공유 Redis 사용 고려 (단일 로그인 요구사항 시)
  - Pub/Sub: WebSocket Service의 메시지 브로드캐스팅용 공유 Redis 고려

## 샤딩/파티셔닝 전략

### RDBMS 샤딩

- **User Service**: 사용자 ID 기준 샤딩
- **Chat Room Service**: 채팅방 ID 기준 샤딩
- **Message Service**:
  - 채팅방 ID 기준 샤딩 (최소 파티셔닝)
  - 시간 기반 파티셔닝 (월별/년별)

## Kubernetes 배포 전략

- 각 서비스를 독립적인 Deployment로 관리
- Service Mesh 고려 (Istio 등)
- ConfigMap, Secret을 통한 설정 관리
- Horizontal Pod Autoscaler (HPA) 설정

## 모니터링 및 관찰성

- 각 서비스의 메트릭 수집
- 분산 추적 (Distributed Tracing)
- 로그 집계
- 알림 시스템
