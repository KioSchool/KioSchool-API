# [Frontend] 테이블 Hash 기반 주문 생성 적용 가이드

이 문서는 KioSchool-API의 main 브랜치에 추가된 **테이블 hash 기반 주문 검증 기능**을 React 프론트엔드에 반영하기 위한 인수인계 문서다. 백엔드 변경은 이미 머지되어 있고, 프론트엔드는 호환되도록 클라이언트를 업데이트하면 된다.

---

## 1. 배경 / 문제

기존 주문 생성 API는 `workspaceId`와 `tableNumber`(정수)만으로 테이블을 식별했다.

문제: `tableNumber`는 1, 2, 3 ... 처럼 추측이 매우 쉽기 때문에, 손님이 자기 테이블이 아닌 다른 테이블 번호로 주문을 보내 다른 좌석에 음식을 시키는 장난/악용이 가능했다.

해결: 각 `WorkspaceTable`에 추측 불가능한 `tableHash`(UUID v4)를 부여하고, 주문 생성 시 `tableNumber` 대신 이 hash로 테이블을 식별하도록 변경.

---

## 2. 백엔드 변경 요약 (참고용)

### 2.1 엔티티 / DB
- `WorkspaceTable.tableHash`에 unique 제약 + NOT NULL 제약 추가 (Liquibase: `2026/04/28-01-changelog.xml`)
- 기존 테이블 데이터는 `WorkspaceService.updateWorkspaceTables`에서 `UUID.randomUUID().toString()`으로 자동 생성됨

### 2.2 API: `POST /order`

**요청 바디 (`CreateOrderRequestBody`)**

```ts
type CreateOrderRequestBody = {
  workspaceId: number;
  tableNumber: number;        // 과도기 동안 유지, hash로 전환 후 제거 예정
  tableHash: string | null;   // ★ 신규 필드 (nullable)
  orderProducts: OrderProductRequestBody[];
  customerName: string;
};
```

**서버 동작 (`OrderFacade.createOrder`)**
- `tableHash == null` → 기존처럼 `tableNumber`로 테이블 조회 (legacy fallback)
- `tableHash != null` → `(workspace, tableHash)`로 테이블 조회. 못 찾으면 **404 `WorkspaceTableNotFoundException`** ("존재하지 않는 태이블입니다.")
- 저장되는 `Order.tableNumber`는 **hash로 찾은 테이블의 실제 tableNumber**로 덮어써짐 → 클라이언트가 hash와 다른 tableNumber를 보내도 hash가 정답이 됨

### 2.3 응답: `WorkspaceTableDto`

워크스페이스 테이블 조회 응답에 이미 `tableHash` 필드가 포함되어 있다:

```ts
type WorkspaceTableDto = {
  id: number;
  tableNumber: number;
  tableHash: string;          // ★ 이 값을 QR 코드 / 주문 페이지 진입 URL에 사용
  orderSession: OrderSessionDto | null;
  createdAt: string | null;
  updatedAt: string | null;
};
```

---

## 3. 프론트엔드에서 해야 할 일

### 3.1 손님 주문 페이지 (Customer Order)

**(1) 진입 경로 확인**

손님이 테이블 QR을 찍어 진입할 때 URL에 `tableHash`가 실려 있어야 한다. 현재 라우팅 구조를 확인하고 다음 중 하나로 정렬:

- 권장: `/order/:workspaceId/:tableHash` 또는 query param `?tableHash=xxx`
- 기존이 `?tableNumber=3` 형태라면 → `?tableNumber=3&tableHash=xxx`로 점진 마이그레이션 (백엔드는 둘 다 받음)

**(2) 주문 생성 API 호출 시 `tableHash` 포함**

`POST /order` 호출하는 axios/fetch 코드에서 요청 바디에 `tableHash`를 추가:

```ts
await api.post('/order', {
  workspaceId,
  tableNumber,        // 과도기 동안 같이 보내도 OK
  tableHash,          // URL에서 받아온 값
  customerName,
  orderProducts,
});
```

**(3) 에러 처리**

- 404 (`WorkspaceTableNotFoundException`): hash가 잘못되었거나 만료됨 → "유효하지 않은 테이블입니다" 안내 + QR 다시 스캔 유도
- 기존 `NoOrderSessionException` 등 다른 에러는 변경 없음

### 3.2 어드민 페이지 (테이블 관리)

**QR 코드 / 테이블 링크 생성 로직**

테이블 목록을 보여주거나 QR 코드를 생성하는 화면이 있다면, 링크에 `tableNumber` 대신 `tableHash`를 사용:

```ts
// Before
const tableUrl = `${BASE}/order?workspaceId=${ws}&tableNumber=${t.tableNumber}`;

// After
const tableUrl = `${BASE}/order?workspaceId=${ws}&tableHash=${t.tableHash}`;
```

기존에 출력해둔 QR이 있다면 재출력이 필요할 수 있음 — 운영에 공지.

### 3.3 타입 정의 업데이트

- 요청 타입(`CreateOrderRequestBody`)에 `tableHash: string | null` (또는 `?: string`) 추가
- 워크스페이스 테이블 응답 타입에 `tableHash: string` 추가 (이미 응답에는 들어옴)

---

## 4. 마이그레이션 전략 (점진 전환)

백엔드는 의도적으로 `tableHash`를 nullable로 두었고 `tableNumber`도 계속 받는다. 프론트엔드는 다음 순서로 옮겨가는 것을 권장:

1. **1단계**: 어드민의 QR/링크 생성을 `tableHash` 기반으로 전환 (새 QR 출력은 hash 사용)
2. **2단계**: 손님 주문 페이지에서 URL의 `tableHash`를 읽어 요청에 포함 (없으면 fallback으로 `tableNumber` 사용)
3. **3단계**: 모든 매장이 새 QR로 갱신되었다고 확인되면 백엔드의 `tableNumber` 분기 제거 요청 (코드에 `// todo remove after use tableHash` 마커 3곳 존재)

---

## 5. 보안 노트

- `tableHash`는 UUID v4라 추측 공격 방어용으로는 충분하지만, **노출되면 그대로 다른 손님이 그 테이블로 주문 가능**하다. 즉 hash는 "비밀"이라기보다 "추측 방지"가 목적이다.
- 추가 강화가 필요해지면 별도 논의 (예: 세션 시작 시 만료되는 단기 토큰 발급 등). 이번 스코프는 아님.
- hash는 로그/Sentry 등에 풀로 찍히지 않게 주의 (URL이 외부 추적 도구에 노출되지 않도록).

---

## 6. 테스트 체크리스트

- [ ] 정상 hash로 주문 생성 → 200, 응답의 `tableNumber`가 hash 매핑과 일치
- [ ] 잘못된 hash로 주문 생성 → 404 처리 및 사용자 안내
- [ ] hash 없이 (legacy) 주문 생성 → 200 (과도기 동안 기존 동작 유지)
- [ ] 다른 워크스페이스의 hash로 주문 시도 → 404 (백엔드에서 workspace AND 조건으로 조회)
- [ ] hash와 다른 tableNumber를 같이 보낸 경우 → 저장된 주문의 tableNumber가 hash 기준으로 보정됨 (서버 동작 검증)
- [ ] 어드민 QR 페이지에서 새 hash 기반 URL이 생성되는지

---

## 7. 관련 백엔드 파일 (참고)

- `domain/order/controller/OrderController.kt` — `POST /order`
- `domain/order/dto/request/CreateOrderRequestBody.kt` — 요청 DTO
- `domain/order/facade/OrderFacade.kt:30~` — hash 분기 로직
- `domain/workspace/service/WorkspaceService.kt:201` — `getWorkspaceTableByHash`
- `domain/workspace/repository/WorkspaceTableRepository.kt:15` — `findByTableHashAndWorkspace`
- `domain/workspace/exception/WorkspaceTableNotFoundException.kt` — 404 응답
- `domain/workspace/entity/WorkspaceTable.kt` — `@Column(unique = true) tableHash`
- `db/changelog/2026/04/28-01-changelog.xml` — DB 제약 추가

관련 커밋: `e237e7b` (unique index), `7d767c5` (create order with table hash).
