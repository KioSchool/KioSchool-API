# KioSchool-API AI 에이전트 컨텍스트 가이드

## 📌 프로젝트 개요
- **프로젝트명**: KioSchool-API
- **설명**: 대학교 축제 주점용, QR 기반 테이블 오더 시스템의 백엔드입니다. 주점 단위 멀티테넌시(Multi-tenancy)를 지원합니다.
- **기술 스택**:
  - 언어: Kotlin 1.9
  - 프레임워크: Spring Boot 3.1.5 (WebMVC 기반, 코루틴 활용)
  - 데이터베이스: PostgreSQL (운영) / H2 (테스트), JPA, Hibernate
  - 쿼리: QueryDSL (복잡한 집계 및 필터링 용도)
  - 비동기 처리: Kotlin Coroutines & Async
  - 테스트: Kotest, MockK

## 🏛 아키텍처 패턴
- **계층 구조 (Layered Architecture)**: `Controller` -> `Facade` -> `Service` -> `Repository`
  - **Controller (컨트롤러)**: HTTP 요청 처리, 라우팅 및 Swagger (`@Operation`, `@Tag`) 문서를 담당합니다. 공통 `ApiResponse` 래퍼 객체 없이 DTO 인스턴스를 직접 반환합니다.
  - **Facade (퍼사드)**: 오케스트레이터 또는 BFF(Backend 전용 용도) 역할을 합니다. 여러 도메인 간의 의존성을 해결하고 DTO를 조합하는 역할을 수행합니다.
  - **Service (서비스)**: 순수하게 단일 도메인에 종속된 핵심 비즈니스 로직을 포함합니다.
  - **Repository (리포지토리)**: Spring Data JPA를 사용하며, 동적 쿼리를 처리하기 위해 QueryDSL(`*CustomRepository` 인터페이스)을 적극적으로 활용합니다.

## ✍️ 코딩 컨벤션 및 규칙
1. **Service 계층 간 호출 금지 (Strict Architecture)**:
   - `Service` 계층에서는 다른 `Service`를 직접 호출하면 절대 안 됩니다.
   - 타 트랜잭션/도메인의 Service를 호출해야 하는 로직은 **Facade 계층**으로 완전히 위임하여 오케스트레이션해야 합니다.
2. **관리자(Admin) 권한 검증**:
   - 관리자 기능을 처리하는 컨트롤러는 `Admin*` 접두사가 붙으며 `/admin/...` 엔드포인트에 매핑됩니다.
   - 커스텀 어노테이션인 `@AdminUsername username: String`을 파라미터로 사용하여 현재 요청한 관리자의 식별자를 깔끔하게 가져옵니다.
3. **멀티테넌시 (Multi-Tenancy)**:
   - 대부분의 데이터 베이스 조회/조작은 데이터를 격리하기 위해 `workspaceId`를 필요로 합니다. 항상 사용자가 해당 `workspaceId`에 접근할 권한이 있는지 확인해야 합니다.
4. **데이터베이스 및 트랜잭션 처리**:
   - 연관 관계 매핑 시 `BatchSize` 설정 등을 통해 O(1+1) 정도로 N+1 문제를 방어하며, **성능보다 가독성을 우선**합니다. 조회 쿼리가 다소 분리되더라도 가독성이 좋다면 허용합니다. 단, 성능 병목이 심각한 경우에만 제한적으로 `FetchJoin`을 채용합니다.
   - 조회된 데이터를 기반으로 복잡한 집계 수식을 계산할 경우, 복잡한 응답용 DTO를 바로 만들기보다 중간 형태의 **Intermediate DTO**로 먼저 프로젝션(Projection)하는 패턴을 선호합니다.
5. **데이터 초기화 (Data Initialization)**:
   - 기본 데이터 시드 및 설정은 표준 SQL 파일이 아닌, `global/schedule/script` 하위에 위치한 커스텀 Kotlin 스크립트들을 통해 관리 및 초기화됩니다.

## 🚨 주의 사항 및 안티 패턴 (Known Gotchas)
- 비동기 실행(Coroutines)과 Spring Data JPA 연동 시 `LazyInitializationException`에 각별히 주의해야 합니다. 스레드가 전환되기 전에 연관 관계 엔티티가 로드되도록 확실히 하거나 명시적인 Fetch Join을 사용하세요.
- 워크스페이스 접근 권한 확인은 컨트롤러나 퍼사드 계층에서 `WorkspaceService.checkAccessible(workspaceId, username)`을 호출하여 안전하게 처리하세요.
