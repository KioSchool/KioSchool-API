# KioSchool API

키오스쿨(대학 축제 주점을 위한 QR 테이블 주문 서비스)의 백엔드 서버.

- 서비스: https://kio-school.com
- 프론트엔드: [KioSchool](https://github.com/KioSchool/KioSchool)
- 운영 API: `https://api.kio-school.com`, 개발 API: `https://dev-api.kio-school.com`

## 하는 일

- 손님 주문 접수, 주문 상태 변경(미결제, 결제완료, 서빙완료, 취소), 운영진 화면으로 실시간 주문 전달
- 운영진 가입(학교 이메일 인증), 로그인(JWT 쿠키, refresh token)
- 주점, 메뉴·카테고리, 테이블과 테이블별 QR, 테이블 사용 세션 관리
- 손님이 송금할 주점 계좌 등록(예금주 조회)
- 일별 주문 통계, 운영진 대시보드, 인사이트 카드
- 문의 접수(봇 차단, 디스코드 알림), 변경 이력, 후원 클릭 기록
- 슈퍼어드민: 주점·사용자 관리, 가입 가능한 이메일 도메인, 은행 목록, 문의, 축제 달력, 캐시

경로 앞부분으로 권한을 나눈다. `/admin/**`은 운영진, `/super-admin/**`은 슈퍼어드민, 나머지는 손님용 공개 API다.

## 기술 스택

| 구분 | 사용 |
|---|---|
| 기본 | Kotlin 1.9, Spring Boot 3.1, Java 17 |
| 데이터 | PostgreSQL 17, Spring Data JPA, QueryDSL, Liquibase |
| 캐시·메시징 | Redis (캐시, 실시간 메시지 pub/sub) |
| 실시간 | WebSocket + STOMP (SockJS) |
| 인증 | Spring Security, JWT |
| 외부 연동 | AWS S3(이미지), Resend SMTP(메일), PortOne(예금주 조회), Discord 웹훅, Cloudflare Turnstile |
| 모니터링 | Actuator, Micrometer + Prometheus, GCP Cloud Logging·Trace |
| 테스트 | Kotest, MockK |
| API 문서 | springdoc-openapi (Swagger UI) |

## 로컬에서 실행하기

필요한 것: JDK 17, Docker

### 1. DB와 Redis 띄우기

```bash
docker compose -f src/main/resources/db/docker/docker-compose.yml up -d --build
```

- PostgreSQL 17(ko_KR 로케일)이 `localhost:2345`, Redis가 `localhost:6379`에 뜬다
- PostgreSQL은 prod와 같은 메이저 버전(17)으로 고정했다. 버전이 다르면 prod 백업 덤프를 로컬에 복원할 수 없다

### 2. 환경 변수

`local` 프로필로 실행한다. DB·Redis 접속 정보는 `application-local.yml`에 들어 있고, 아래 값은 직접 넣어야 한다. 해당 기능을 쓰지 않으면 아무 값이나 넣어도 서버는 뜬다.

| 변수 | 쓰는 곳 |
|---|---|
| `MAIL_FROM`, `MAIL_PASSWORD` | 메일 발송(가입 인증 메일 등) |
| `AWS_S3_BUCKET`, `AWS_S3_ACCESS_KEY`, `AWS_S3_SECRET_KEY` | 주점·상품 이미지 업로드 |
| `PORTONE_API_KEY`, `PORTONE_API_SECRET` | 계좌 등록 시 예금주 조회 |
| `DISCORD_WEBHOOK_URL` | 문의 접수 등 운영 알림 |
| `JWT_SECRET_KEY` | 선택. 넣으면 `application-local.yml`의 키 대신 이 값으로 토큰을 서명한다 |
| `TURNSTILE_SECRET_KEY` | 선택. 비워 두면 문의 봇 검증을 건너뛴다 |

dev·prod 서버는 여기에 `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `REDIS_HOST`, `REDIS_PASSWORD`를 더 쓴다.

### 3. 서버 실행

위 환경 변수를 넣은 상태에서 실행한다. IntelliJ에서는 실행 설정에 active profile `local`과 환경 변수를 넣으면 된다.

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

- 주소는 `http://localhost:8080/api`다(`local` 프로필은 context path가 `/api`)
- 서버가 뜰 때 Liquibase가 DB 마이그레이션을 실행한다
- Swagger UI: http://localhost:8080/api/swagger-ui/index.html (prod에서는 꺼져 있다)
- 프론트엔드 개발 서버(`localhost:3000`)가 `/api` 요청을 이 서버로 넘긴다. local 프로필의 CORS·WebSocket 허용 주소도 `localhost:3000` 기준이다
- 실시간 주문은 SockJS 엔드포인트 `/ws`(local은 `/api/ws`)에 연결해 `/sub/order/{주점 ID}`를 구독하면 받는다. 새 주문이나 주문 변경이 생기면 서버가 Redis pub/sub으로 이 주소에 보낸다

### 4. 기본 데이터 넣기

Liquibase는 테이블만 만들고 기준 데이터는 넣지 않는다. 화면을 써 보려면 아래 데이터가 필요하다.

- 가입: `email_domain`에 있는 도메인의 이메일로만 가입할 수 있다. 인증 메일은 `MAIL_*`에 실제 값이 있어야 발송된다
- 계좌 등록: `bank`에 은행 목록이 있어야 한다
- 슈퍼어드민: 가입한 계정의 `role`을 `0`으로 바꾼다. `role`은 `UserRole` enum의 순서로 저장된다(`0` = `SUPER_ADMIN`, `1` = `ADMIN`)

```bash
docker exec -it kioschool-database psql -U myuser -d kio-school-database
```

```sql
INSERT INTO email_domain (created_at, updated_at, domain, name) VALUES (now(), now(), 'kio-univ.ac.kr', '키오대학교');
INSERT INTO bank (created_at, updated_at, name, code, toss_name) VALUES (now(), now(), 'KB국민은행', '004', 'KB국민은행');
UPDATE "user" SET role = 0 WHERE login_id = '<아이디>';
```

## 테스트

```bash
./gradlew test
```

Kotest·MockK로 작성한 단위 테스트라 DB나 Redis 없이 돈다. `main`에 push하면 GitHub Actions에서도 실행된다.

## 폴더 구조

```
src/main/kotlin/com/kioschool/kioschoolapi/
├── domain/           도메인별 패키지
│   ├── order/        주문, 주문 상품, 테이블 사용 세션, 실시간 주문 이벤트
│   ├── workspace/    주점, 테이블, 주점 설정·이미지, 멤버
│   ├── product/      상품, 카테고리
│   ├── user/         가입, 로그인, 로그인 세션, 유입 경로 설문
│   ├── account/      계좌, 은행
│   ├── email/        이메일 인증 코드, 가입 가능한 도메인
│   ├── statistics/   일별 주문 통계
│   ├── dashboard/    운영진·슈퍼어드민 대시보드
│   ├── insight/      인사이트 카드
│   ├── inquiry/      문의
│   ├── donation/     후원 클릭
│   └── changelog/    변경 이력
└── global/           보안(JWT), WebSocket, 캐시, 스케줄러, 에러 처리, 로깅·모니터링,
                      외부 연동(S3, PortOne, Discord, Turnstile, Toss)

src/main/resources/
├── application.yml, application-{local,dev,prod}.yml
└── db/
    ├── changelog-master.yaml   changelog/ 아래 파일을 이름 순서로 모두 적용
    ├── changelog/YYYY/MM/      Liquibase changelog
    └── docker/                 로컬 DB·Redis
```

도메인 패키지는 `controller`, `facade`, `service`, `repository`, `entity`, `dto`로 나뉜다. 컨트롤러는 facade를 부르고, facade가 여러 service를 묶는다.

## 개발할 때 주의할 점

- 스키마는 Liquibase changelog로만 바꾼다(JPA `ddl-auto`는 쓰지 않는다). 새 파일은 `db/changelog/<연>/<월>/<일>-<번호>-changelog.xml` 형식으로 만든다. 파일 이름 순서대로 적용되므로 이름 규칙을 지켜야 한다
- 엔티티 필드 이름이 대문자 한 글자로 끝나면(`positionX` 등) `@Column(name = "...")`을 꼭 붙인다. Hibernate가 `positionx`로 매핑해 실행 중에야 오류가 난다
- `spring.jpa.open-in-view`는 켜 둔다. 여러 API가 응답을 만들 때 지연 로딩에 기대고 있어서, 끄면 `LazyInitializationException`으로 500이 난다
- `UserRole` 같은 enum이 순서 값으로 저장되는 컬럼이 있다. enum 항목의 순서를 바꾸지 않는다

## 브랜치와 배포

| 브랜치 | 역할 |
|---|---|
| `main` | 기본 브랜치. 기능 브랜치를 여기서 따서 여기로 PR을 보낸다 |
| `development` | 개발 서버. `main`을 머지한 뒤 `dev-x.y.z` 태그를 push하면 배포된다 |
| `release` | 운영 서버. `main`을 머지한 뒤 `x.y.z` 태그를 push하면 배포된다 |

태그가 push되면 GitHub Actions가 jar를 빌드해 Docker 이미지(`kioschool-server`, amd64·arm64)를 Docker Hub에 올리고, 서버에 접속해 배포 스크립트를 실행한다. 서버의 docker compose·nginx·모니터링 설정은 비공개 저장소 KioSchool-Infra에서 관리한다. 운영 배포는 블루그린 방식이고, 끝나면 `/actuator/health`로 서버 상태를 확인한다.

## PR 규칙

- 제목은 `종류 - 설명` 형식으로 쓴다. 종류는 `ADD`, `FIX`, `EDIT`, `REFACTOR`, `REMOVE`, `DEPLOY`, `REVERT` 중 하나다(프론트엔드와 같은 규칙)
- 본문은 PR 템플릿의 개요, 리뷰 예상 시간, 중요한 변경점을 채운다
- 브랜치 이름에 노션 태스크 번호(`KIO-123`)를 넣으면, PR을 열 때 태스크가 '진행 중', 머지되면 '완료'로 바뀐다
- 공개 저장소이므로 보안 문제의 재현 방법은 커밋 메시지나 PR에 적지 않는다

## 라이선스

[GPL-3.0](LICENSE)
