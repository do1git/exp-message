# 프로젝트 구조

## 전체 구조

```text
exp-message/
├── 01-infrastructure/           # 인프라 설정 (자세한 내용은 01-infrastructure/README.md 파일 참고)
├── 02-backend/                 # 백엔드 (자세한 내용은 02-backend/README.md 파일 참고)
├── 03-frontend/                # 프론트엔드
│   └── (프레임워크 선정 후 구조 결정)
├── 04-test/                    # 시스템 레벨 테스트
│   ├── integration-test/      # 통합 테스트 (서비스 간)
│   ├── load-test/             # 부하 테스트
│   └── stress-test/           # 스트레스 테스트
├── 05-scripts/                 # 스크립트
│   ├── setup.sh               # 초기 설정 스크립트
│   └── deploy.sh              # 배포 스크립트
├── 00-docs/                    # 문서
│   ├── patch/                 # 패치노트
│   ├── architecture.md        # 아키텍처 문서
│   └── project-structure.md   # 프로젝트 구조 문서
└── README.md                   # 프로젝트 메인 README
```

## 프론트엔드 구조

```text
frontend/
├── src/                        # 소스 코드
│   ├── components/            # 컴포넌트
│   ├── pages/                 # 페이지
│   ├── services/              # API 서비스
│   └── utils/                 # 유틸리티
├── public/                     # 정적 파일
├── Dockerfile                  # Docker 이미지 빌드
└── README.md                  # 프론트엔드 README
```

## 백엔드 구조

백엔드 구조에 대한 자세한 내용은 [02-backend/README.md](../02-backend/README.md)를 참고하세요.

## 테스트 구조

### 시스템 레벨 테스트 (test/)

전체 시스템을 대상으로 하는 테스트:

```text
test/
├── integration-test/         # 통합 테스트 (서비스 간 통신 검증)
│   └── scenarios/            # 통합 테스트 시나리오
├── load-test/                # 부하 테스트
│   ├── k6/                   # k6 스크립트
│   └── jmeter/               # JMeter 테스트 계획
└── stress-test/             # 스트레스 테스트
    └── scenarios/            # 스트레스 테스트 시나리오
```

### 서비스 레벨 테스트

각 서비스의 단위 테스트는 해당 서비스 디렉토리 내부에 위치:

- `backend/service-name/tests/`: 각 서비스의 단위 테스트
- `frontend/tests/`: 프론트엔드 단위 테스트

## 인프라 구조

인프라 설정에 대한 자세한 내용은 [01-infrastructure/README.md](../01-infrastructure/README.md)를 참고하세요.

## 설계 원칙

이 프로젝트 구조는 **명확한 역할 분리**와 **관리의 편의성**을 위해 다음과 같은 원칙에 따라 설계되었습니다:

- **역할 기반 분리**: 프론트엔드, 백엔드, 테스트, 인프라를 독립적으로 관리
- **중앙 집중식 관리**: 인프라 설정과 공통 스크립트를 한 곳에서 관리
- **서비스 독립성**: 각 서비스는 독립적으로 배포 및 스케일링 가능
- **확장성**: 새로운 서비스나 테스트 유형을 쉽게 추가 가능
