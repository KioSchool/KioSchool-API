# 워크스페이스별 동적 OG 이미지

작성일: 2026-05-04
브랜치: `docs/og-image-spec`
관련 리포: `KioSchool` (프론트, AWS Amplify), `KioSchool-API` (이 리포, 백엔드 — 본 작업 대상)

---

## 1. 목표

손님이나 사장님이 `https://kio-school.com/order?workspaceId=42` 같은 키오스쿨 주문 페이지 URL을 카카오톡·페이스북·슬랙 등에 공유했을 때, **해당 워크스페이스(주점)의 대표 사진**을 미리보기 카드로 노출한다. 현재는 모든 워크스페이스가 동일한 정적 이미지(`https://kio-school.com/preview.png`)를 보여주고 있어, 사장님 입장에선 "내 주점이라는 게 안 드러난다"는 한계가 있다.

## 2. 적용 URL 범위

- ✅ `https://kio-school.com/order?workspaceId={id}` (손님 진입 + 사장님이 인스타 바이오 등에 공유하는 동일 URL)
- ❌ `/order-basket`, `/order-pay`, `/order-wait`, `/order-complete` — 사용자가 직접 공유하지 않는 주문 플로우 중간 단계
- ❌ 그 외 (`/admin/*`, `/login`, 홈 `/` 등) — 기존 정적 OG 메타 그대로 유지

`/order` 외 페이지에서 공유 케이스가 확인되면 그때 확장한다.

## 3. 제약과 결정 배경

- 프론트는 **Vite SPA**다. SSR이 없으므로 React에서 `<meta>` 태그를 동적으로 바꿔도 크롤러에는 보이지 않는다.
- 프론트 호스팅은 **AWS Amplify** (CloudFront + S3). Amplify는 정적 호스팅이며 백엔드 코드를 거치지 않고 `index.html`을 그대로 반환한다.
- 백엔드는 **Spring Boot 3.1.5 / Kotlin 1.9 / Java 17**, `https://api.kio-school.com`에 노출. nginx 1.21.5 리버스 프록시.

따라서 "Spring이 모든 SPA 응답에 메타태그를 주입한다" 같은 단순한 SSR 패턴은 불가하다. **Amplify의 User-Agent 조건 리라이트**로 크롤러 트래픽만 Spring으로 우회시키는 방식을 택한다.

## 4. 전체 아키텍처

```
[크롤러: KAKAOTALK / facebookexternalhit / Slackbot 등]
     │
     │ GET https://kio-school.com/order?workspaceId=42
     ▼
┌──────────────────────────────────────────────┐
│ AWS Amplify (CloudFront + S3)                 │
│ ─ User-Agent 매칭 ─                            │
│   • 크롤러? → 200 리라이트 → Spring `/og/order`  │
│   • 일반 사용자? → 그대로 SPA index.html        │
└──────────────────────────────────────────────┘
     │
     │ (크롤러만)
     ▼
┌──────────────────────────────────────────────┐
│ Spring  /og/order                              │
│  1. workspaceId 파싱                           │
│  2. Workspace.ogImageUrl 조회                   │
│  3. 미니 HTML 반환 (og:* 메타태그)              │
└──────────────────────────────────────────────┘
     │
     │ og:image
     ▼
┌──────────────────────────────────────────────┐
│ S3 (workspace 이미지 동일 버킷)                │
│  $workspacePath/workspace{id}/og/{hash}.png   │
└──────────────────────────────────────────────┘

[관리자가 워크스페이스 사진 업로드/삭제]
     │  (기존 PUT /workspace/image 흐름)
     │  → @WorkspaceUpdateEvent AOP가 WorkspaceUpdatedEvent 발행
     ▼
┌──────────────────────────────────────────────┐
│ WorkspaceOgImageListener (신규)                │
│  @TransactionalEventListener(AFTER_COMMIT)     │
│  대표 사진(첫 번째 등록 사진) → cover scale →   │
│  배지 PNG 합성 → S3 업로드 → og_image_url 저장   │
└──────────────────────────────────────────────┘
```

핵심 흐름은 두 갈래:
1. **공유 시점(크롤러)**: Amplify가 UA로 분기, Spring은 메타태그 박힌 미니 HTML만 응답.
2. **합성 시점(워크스페이스 저장)**: 사진 업로드 트랜잭션이 커밋된 직후 비동기로 카드 합성·업로드.

## 5. OG 카드 디자인

### 5.1 시안
- 1200×630 PNG.
- **사진을 cover로 깔고**, **우하단에 키오스쿨 알약 배지 PNG를 합성**한다.
- 배지: 검정 반투명(0.55) + blur, 키오스쿨 로고 + "키오스쿨" 텍스트가 들어간 디자이너 산출물.
- **워크스페이스 이름은 카드에 들어가지 않는다.** 주점의 대표 사진엔 보통 간판/네온/포스터로 가게 이름이 이미 박혀 있어 중복이며, 사진의 무드를 가린다. 우리 카드의 역할은 "이 주점은 키오스쿨로 주문받는다"를 알리는 것에 한정.

### 5.2 합성 방식
- **백엔드는 텍스트를 렌더하지 않는다.** 폰트 로딩, 한글 자간 처리, 트래커 등 텍스트 렌더링 인프라가 일절 필요 없음.
- 디자이너가 `og-badge.png` (투명 배경, 2x 권장 — 약 336×88) 한 장을 산출물로 제공한다. 리포의 `src/main/resources/og/og-badge.png`에 커밋한다.
- 합성은 **scrimage** (`com.sksamuel.scrimage`) 라이브러리 사용. 이미 `S3Service.uploadResizedWebpImage`가 scrimage의 `ImmutableImage`로 동작 중이므로 동일 라이브러리 일관성 유지.

```kotlin
val photo = ImmutableImage.loader().fromStream(downloaded)
val card  = photo.cover(1200, 630)
                 .overlay(badge,
                          x = 1200 - 26 - badge.width,
                          y = 630 - 22 - badge.height)
val bytes = card.bytes(PngWriter.NoCompression)
```

### 5.3 폴백
사진이 없는 워크스페이스 또는 합성 실패 시:
- `og:image`를 `https://kio-school.com/preview.png`(글로벌 기본 이미지)로 폴백.
- application yml의 `kio.og.fallback-image-url` 키로 설정값 분리.

## 6. 합성·저장 모델

### 6.1 키 규칙
- S3 경로: `$workspacePath/workspace{workspaceId}/og/{hash}.png`
  - `workspacePath` 변수는 기존 `WorkspaceService.saveWorkspaceImages`에서 쓰는 동일 경로 컨벤션 재사용
  - `hash` = SHA-1(`sourcePhotoUrl`).take(8)
  - 사진이 바뀌면 hash도 바뀌어 **새 S3 객체**가 생성된다 → CDN 캐시 무효화가 자연스럽게 됨
- Cache-Control: `public, max-age=31536000, immutable` (객체가 immutable이므로 영구 캐시 안전)

### 6.2 "대표 사진" 선택 규칙
`workspace.images.minByOrNull { it.id }` — **가장 먼저 등록한 사진**.

(현재 `Workspace.images`에는 `@OrderBy`가 없어 순서가 결정적이지 않으므로 명시적 규칙을 둔다. 별도 "대표 사진 지정" UI는 본 스코프 밖.)

### 6.3 이전 OG 카드 객체 청소
- **즉시 삭제하지 않는다.** 카카오톡/페북의 외부 캐시가 옛 URL을 가리키고 있을 수 있어 즉시 삭제 시 미리보기가 깨진다.
- **S3 lifecycle rule**: `og-cards/` prefix(또는 `og/` 디렉토리) 90일 후 자동 삭제. 카카오 ~24h, 페북 ~7d TTL을 충분히 넘긴다.
- 워크스페이스 자체가 삭제될 때만 즉시 cascade delete (별도 작업 — 본 스펙에선 정의만, 실 구현은 워크스페이스 삭제 흐름이 어디서 일어나는지 확인 후 결정).
- **lifecycle rule prefix는 `*/og/*` 디렉토리 패턴**(또는 버킷 정책상 가능한 가장 가까운 표현). S3 lifecycle은 prefix 매칭만 지원하므로 워크스페이스별로 묶이는 prefix 형태(`workspace1/og/`, `workspace2/og/` …)에 대해 룰을 어떻게 적용할지 인프라 작업자와 협의 — 예: 별도 `og-cards/` 평면 prefix로 키 컨벤션을 옮기는 안도 검토 가능. 본 스펙 기본은 워크스페이스 디렉토리 구조 일관성을 위해 `workspace{id}/og/`.

## 7. DB 스키마 변경

### 7.1 마이그레이션 (Liquibase XML)
파일: `src/main/resources/db/changelog/2026/05/04-01-add-workspace-og-image-url.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
  xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                      http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.24.xsd">
  <changeSet author="rune" id="1777890000000-1">
    <addColumn tableName="workspace">
      <column name="og_image_url" type="VARCHAR(512)"/>
    </addColumn>
  </changeSet>
</databaseChangeLog>
```

master changelog include 룰을 확인하여 자동 픽업되는지 검증한다.

### 7.2 엔티티
`Workspace.kt`에 nullable 컬럼 추가:
```kotlin
@Entity
@Table(name = "workspace")
class Workspace(
    /* 기존 필드들 그대로 */
    var ogImageUrl: String? = null,
) : BaseEntity()
```

JPA dirty checking으로 충분. 별도 update 메서드를 새로 추가하지 않는다.

## 8. 백엔드 구성요소 명세

### 8.1 신규 파일

| 경로 | 책임 |
|---|---|
| `src/main/resources/db/changelog/2026/05/04-01-add-workspace-og-image-url.xml` | 컬럼 추가 |
| `src/main/resources/og/og-badge.png` | 디자이너 산출물 (투명 PNG) |
| `src/main/kotlin/com/kioschool/kioschoolapi/global/og/controller/OgController.kt` | `/og/order` 엔드포인트 |
| `src/main/kotlin/com/kioschool/kioschoolapi/domain/workspace/service/OgCardGenerator.kt` | 사진 + 배지 합성 → S3 업로드 |
| `src/main/kotlin/com/kioschool/kioschoolapi/domain/workspace/listener/WorkspaceOgImageListener.kt` | `WorkspaceUpdatedEvent` 구독, OG 카드 갱신 |
| `src/main/kotlin/com/kioschool/kioschoolapi/global/schedule/script/V08__BackfillOgCards.kt` | 1회성 백필 스크립트 (V07 패턴 따름) |

### 8.2 수정 파일

| 경로 | 변경 |
|---|---|
| `domain/workspace/entity/Workspace.kt` | `ogImageUrl: String?` 컬럼 1개 추가 |
| `global/aws/S3Service.kt` | `uploadBytes(...)` 및 `urlFor(path)` 메서드 추가 |
| SecurityConfig (위치 확인 필요) | `/og/**` 경로 permitAll 추가 |
| `application.yml` | `kio.og.fallback-image-url: https://kio-school.com/preview.png` |

### 8.3 OgController — `/og/order`

```kotlin
@RestController
class OgController(
    private val workspaceRepository: WorkspaceRepository,
    @Value("\${kio.og.fallback-image-url}")
    private val fallbackImageUrl: String,
) {
    @GetMapping("/og/order", produces = [MediaType.TEXT_HTML_VALUE])
    fun ogOrder(@RequestParam(required = false) workspaceId: Long?): ResponseEntity<String> {
        val workspace = workspaceId?.let { workspaceRepository.findById(it).orElse(null) }
        val title = workspace?.let { "${it.name} · 키오스쿨" } ?: "키오스쿨"
        val image = workspace?.ogImageUrl ?: fallbackImageUrl
        val canonical = "https://kio-school.com/order" +
            (workspaceId?.let { "?workspaceId=$it" } ?: "")
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
            .body(renderOgHtml(canonical, title, image))
    }
}
```

응답 본문(약 ~1KB):
```html
<!doctype html>
<html lang="ko"><head>
  <meta charset="utf-8">
  <meta property="og:url"         content="...">
  <meta property="og:title"       content="○○주점 · 키오스쿨">
  <meta property="og:description" content="대학 주점 테이블 오더 서비스, 키오스쿨입니다!">
  <meta property="og:type"        content="website">
  <meta property="og:image"       content="https://...og-cards/42-9e8f7a6b.png">
  <meta property="og:site_name"   content="키오스쿨">
  <title>○○주점 · 키오스쿨</title>
</head><body></body></html>
```

설계 결정:
- 응답은 봇만 본다 — 본문은 비워둠. (Amplify rewrite UA 조건을 통과해야만 도달하므로 사람 노출은 사실상 없음.)
- 잘못된 / 없는 `workspaceId`도 폴백으로 200 응답. 봇에 4xx를 주면 미리보기만 깨진다.
- `Cache-Control: public, max-age=600` — 같은 워크스페이스에 대한 빠른 재조회는 nginx/CDN이 흡수.

### 8.4 OgCardGenerator

```kotlin
@Service
class OgCardGenerator(
    private val s3Service: S3Service,
    @Value("\${kio.workspace.path:workspace}")
    private val workspacePath: String,
) {
    private val badge: ImmutableImage by lazy {
        javaClass.getResourceAsStream("/og/og-badge.png").use {
            ImmutableImage.loader().fromStream(it)
        }
    }

    fun generate(workspaceId: Long, sourcePhotoUrl: String): String {
        val photo = s3Service.downloadFileStream(sourcePhotoUrl).use {
            ImmutableImage.loader().fromStream(it)
        }
        val card = photo.cover(1200, 630)
            .overlay(badge,
                     x = 1200 - 26 - badge.width,
                     y = 630 - 22 - badge.height)
        val bytes = card.bytes(PngWriter.NoCompression)
        val hash = sha1(sourcePhotoUrl).take(8)
        val path = "$workspacePath/workspace${workspaceId}/og/$hash.png"
        return s3Service.uploadBytes(bytes, path, "image/png")
    }
}
```

`S3Service`에 신규 메서드 두 개:
```kotlin
fun uploadBytes(bytes: ByteArray, path: String, contentType: String): String {
    val metadata = ObjectMetadata().apply {
        contentLength = bytes.size.toLong()
        this.contentType = contentType
        cacheControl = "public, max-age=31536000, immutable"
    }
    amazonS3Client.putObject(bucketName, path, ByteArrayInputStream(bytes), metadata)
    return amazonS3Client.getUrl(bucketName, path).toString()
}

/** S3 키 → 퍼블릭 URL. 업로드 없이도 listener가 hash 선검사에 사용. */
fun urlFor(path: String): String =
    amazonS3Client.getUrl(bucketName, path).toString()
```

방어 사항:
- 다운로드 타임아웃 10초, 사이즈 캡 10MB.
- 사진 URL의 도메인 화이트리스트 검증(우리 S3 버킷 도메인만 허용) — SSRF 방어.
- MIME 화이트리스트(`image/jpeg`, `image/png`, `image/webp`, `image/gif`).

### 8.5 WorkspaceOgImageListener

이미 존재하는 `WorkspaceUpdatedEvent` 재사용. `@WorkspaceUpdateEvent` AOP 어노테이션이 `WorkspaceService.saveWorkspaceImages`에 이미 붙어 있어, **새 이벤트 발행 코드는 추가하지 않는다**.

`WorkspaceFacade.updateWorkspaceImage` 흐름은 `deleteWorkspaceImages` → `saveWorkspaceImages`를 항상 함께 호출하므로(`saveWorkspaceImages`가 `imageFiles`가 비어있어도 `workspaceRepository.save` 실행), **이미지 삭제만 일어나는 케이스에서도 이벤트는 발행된다**.

`WorkspaceUpdatedEvent`는 사진 외 변경(이름·메모·테이블 수·order setting 등)에도 발행되므로, listener는 **합성 작업 자체를 사진이 실제 바뀐 경우에만 수행**해야 한다. 사진을 매번 다운로드/합성/업로드하는 건 자원 낭비.

```kotlin
@Component
class WorkspaceOgImageListener(
    private val workspaceRepository: WorkspaceRepository,
    private val ogCardGenerator: OgCardGenerator,
) {
    @Async
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: WorkspaceUpdatedEvent) {
        val workspace = workspaceRepository.findById(event.workspaceId).orElse(null) ?: return
        val primaryPhotoUrl = workspace.images.minByOrNull { it.id }?.url

        // 합성을 시도하기 전에 hash 비교로 빠른 스킵 — 사진이 안 바뀐 케이스(이름·메모 등 변경)에서
        // 다운로드/합성/S3 업로드 비용을 들이지 않는다.
        val expectedOgUrl = primaryPhotoUrl?.let { ogCardGenerator.expectedUrl(workspace.id, it) }
        if (workspace.ogImageUrl == expectedOgUrl) return

        val newOgUrl: String? = if (primaryPhotoUrl == null) {
            null  // 사진이 모두 삭제됨 → 폴백으로 회귀
        } else {
            runCatching { ogCardGenerator.generate(workspace.id, primaryPhotoUrl) }
                .getOrElse {
                    // 합성 실패 시 og_image_url을 그대로 유지(이전 카드 보존)하고 종료.
                    Sentry.captureException(it)
                    return
                }
        }
        workspace.ogImageUrl = newOgUrl
        workspaceRepository.save(workspace)
    }
}
```

`OgCardGenerator`에 추가될 메서드 (`generate`와 동일 키 규칙):
```kotlin
fun expectedUrl(workspaceId: Long, sourcePhotoUrl: String): String {
    val hash = sha1(sourcePhotoUrl).take(8)
    val path = "$workspacePath/workspace${workspaceId}/og/$hash.png"
    return s3Service.urlFor(path)   // S3Service에 신규 메서드: 키 → 퍼블릭 URL
}
```

설계 결정:
- **`@TransactionalEventListener(AFTER_COMMIT)`** — 트랜잭션 커밋 후에 실행되어 `workspace.images`의 최신 상태를 안전하게 본다. 기존 `WorkspaceCacheEvictListener`는 `@Async @EventListener` 패턴이지만, 캐시 evict는 idempotent라 race를 허용한 것이다. OG 합성은 race가 발생하면 옛 사진으로 카드를 만들 수 있으므로 `@TransactionalEventListener`로 강화한다.
- **`@Transactional` 필수** — `workspace.images`가 lazy 필드라 트랜잭션 없이 접근 시 `LazyInitializationException`. `@TransactionalEventListener(AFTER_COMMIT)`이 fire될 시점엔 원본 트랜잭션이 이미 끝나 있으므로 listener 자체에 새 트랜잭션이 필요하다.
- **`@Async` + `@TransactionalEventListener` 조합** — 둘 다 프록시 기반이므로 빈 자체가 final이 아니어야 함(Kotlin은 `kotlin-spring` 플러그인이 `@Component`에 자동 open 적용 — `build.gradle.kts`에서 확인됨, 안전).
- **hash 선검사로 사진 미변경 시 스킵** — Sentry 캡처 노이즈도 줄고, S3 PUT/DB UPDATE 비용도 줄임. listener의 멱등성도 자연스럽게 강화됨.
- **사용자 작업은 절대 막지 않는다** — async 실패는 Sentry 알림만, 사용자 응답은 이미 200으로 끝.
- **재진입 안전성** — listener의 `workspaceRepository.save`는 `@WorkspaceUpdateEvent` 어노테이션이 없으므로 AOP를 다시 트리거하지 않는다. 무한 이벤트 루프 없음.
- **합성 실패 시 보존** — 외부 사진 URL이 깨졌거나 S3 업로드가 실패하면 `og_image_url`을 변경하지 않고 종료. 이전에 만들어진 정상 카드가 그대로 노출되도록 보존.

### 8.6 백필 스크립트

`global/schedule/script/V08__BackfillOgCards.kt`. V07(`ResizeExistingImages`) 패턴 그대로 따른다.

요구사항:
- 모든 워크스페이스에 대해 첫 번째 등록 사진 → OG 카드 합성 → `og_image_url` 채움.
- 이미 `og_image_url`이 채워져 있으면 스킵.
- 사진이 없는 워크스페이스는 스킵 (자연 폴백).
- 한 번에 합성하면 부하가 있으니 워크스페이스당 ~50ms 정도의 짧은 sleep 또는 `taskExecutor`로 분산.
- 1회성 → 멱등성 보장(이미 처리된 워크스페이스는 다시 안 함).

## 9. Amplify 리라이트 (프론트 배포 측 작업)

본 리포에서는 코드 변경 없음. 별도 인프라 작업으로 Amplify 콘솔에 두 룰 추가.

```jsonc
[
  {
    "source":   "/order",
    "target":   "https://api.kio-school.com/og/order",
    "status":   "200",
    "condition": "<User-Agent: ^.*(facebookexternalhit|KAKAOTALK|kakaostory|Slackbot|Twitterbot|Discordbot|TelegramBot|LinkedInBot|WhatsApp|naver|Yeti|Daum|Googlebot|bingbot).*$>"
  },

  // 기존 SPA fallback 룰 — 이미 박혀 있을 가능성이 높음
  {
    "source": "</^[^.]+$|\\.(?!(css|gif|ico|jpg|js|png|txt|svg|woff|woff2|ttf|map|json|webp)$)([^.]+$)/>",
    "target": "/index.html",
    "status": "200"
  }
]
```

핵심:
- `status: "200"`은 **리라이트** (URL 유지). 클라이언트 URL은 `kio-school.com/order?workspaceId=42` 그대로 표시되고, 응답 본문만 Spring에서 가져옴.
- 쿼리 스트링 `?workspaceId=42`는 자동으로 보존되어 Spring에 전달.
- dev/prod 분리: dev Amplify는 `target: https://api-dev.kio-school.com/og/order` (또는 dev API 호스트)로.

### ⚠️ POC 필수 — Amplify의 외부 도메인 200 리라이트

Amplify rewrite의 `target`을 같은 도메인이 아닌 외부 도메인(`api.kio-school.com`)으로 둘 때 동작이 일부 환경에서 기대와 다를 수 있다 (CORS 헤더 손실, 응답 사이즈 캡 등). 우리 응답은 ~1KB 미니 HTML이라 문제 가능성은 낮지만 **반드시 검증**한다.

검증 명령:
```bash
curl -i -H "User-Agent: Mozilla/5.0 (compatible; KAKAOTALK 9.0.0)" \
     "https://kio-school.com/order?workspaceId=42" | head -40
```

응답 본문에 `<meta property="og:image" ...>`가 박혀있으면 OK.

만약 외부 target rewrite가 실패한다면, **백업안**:
- Amplify 룰을 `status: "302"` 리다이렉트로 바꾸고 `target: https://og.kio-school.com/order`로 보낸다.
- Spring이 `og.kio-school.com` 서브도메인을 직접 서비스 (또는 nginx에서 라우팅).
- 카카오톡/페북/슬랙 모두 302 리다이렉트를 따라간다.

## 10. Security 설정

`/og/**` 경로는 인증 없이 접근 가능해야 한다 (크롤러는 헤더 없이 친다).

- 현재 SecurityConfig 위치를 확인하여 `permitAll()` 매처에 `/og/**` 추가.
- CSRF: 이 경로는 GET만 허용하므로 영향 없음 (Spring Security 기본은 GET CSRF skip).
- CORS: 응답을 받는 건 크롤러뿐이라 CORS 헤더 추가는 불필요.

## 11. 시나리오 — 사진 변경/삭제

| 시나리오 | 동작 |
|---|---|
| 사진을 다른 사진으로 교체 | 새 hash → 새 S3 객체 → DB 업데이트. 이전 객체는 lifecycle rule에 의해 90일 후 삭제. |
| 대표 사진 삭제 (다른 사진은 있음) | 두 번째 사진이 첫 번째가 됨 → 위와 동일하게 재생성. |
| 사진 모두 삭제 (`images[]` 빔) | `og_image_url = NULL`. `/og/order`는 글로벌 `kio-school.com/preview.png`로 폴백. |
| 워크스페이스 자체 삭제 | 별도 cascade delete (본 스코프 밖, 별도 티켓). |
| 외부 사진 URL이 깨짐 | listener의 `runCatching`이 잡고 Sentry 알림. `og_image_url`은 변경하지 않음 (이전 카드 유지). |

## 12. 외부 SNS 캐시 (우리가 통제 못 하는 영역)

- 카카오톡, 페북, 슬랙은 og:image URL을 자체 캐시한다 (TTL: 카카오 ~24h, 페북 ~7d).
- 사장님이 사진을 바꾼 직후엔 **카카오톡에서 옛 카드가 그대로 보일 수 있다**. og:image URL이 새 hash로 이미 바뀌어 있어 캐시 만료 후엔 자연 갱신.
- 즉시 갱신: 카카오 [공유 디버거](https://developers.kakao.com/tool/clear/og), 페북 [Sharing Debugger](https://developers.facebook.com/tools/debug/).

## 13. 테스트 계획

### 13.1 단위/통합 테스트

| 대상 | 케이스 |
|---|---|
| `OgCardGenerator.generate` | (1) 정상 사진 → 1200×630 PNG (2) 잘못된 URL → 예외 (3) S3 업로드 실패 → 예외 |
| `WorkspaceOgImageListener.on` | (1) 사진 1장 → ogImageUrl 채워짐 (2) 사진 0장 → ogImageUrl=null (3) 합성 실패 → ogImageUrl 변경 안 됨, Sentry 호출 (4) ogImageUrl 동일 → save 스킵 |
| `OgController.ogOrder` | (1) 정상 workspaceId → og:image=ogImageUrl, 본문에 메타태그 5개 (2) null/유효하지 않은 ID → fallback URL (3) Cache-Control 헤더 |
| Liquibase | CI 스키마 검증 |

### 13.2 수동 검증 (배포 전 POC)

```bash
# 일반 사용자 (SPA HTML)
curl -i https://kio-school.com/order?workspaceId=42 | head -20

# KAKAOTALK 위장 (Spring 미니 HTML)
curl -i -H "User-Agent: Mozilla/5.0 (compatible; KAKAOTALK 9.0.0)" \
     "https://kio-school.com/order?workspaceId=42" | head -40
```

후자에 `<meta property="og:image" content="...">`가 박혀있어야 OK.

카카오 디버거에서도 워크스페이스 사진 + 우하단 키오스쿨 배지 카드가 정상 렌더되는지 확인.

## 14. 관측

- **Sentry**: 카드 생성 실패 캡처 (`runCatching ... onFailure { Sentry.captureException }`).
- **로그**: `OgCardGenerator`에 INFO 한 줄 (`workspaceId={}: og card generated, hash={}`).
- **메트릭(선택, 첫 출시 미포함)**: Micrometer로 `kio.og.card.generated{result=success|failure}` 카운터.

## 15. 롤아웃 순서

1. **PR 1 (이 리포)**: 마이그레이션 + `Workspace.ogImageUrl` + `S3Service.uploadBytes`
2. **PR 2 (이 리포)**: `OgCardGenerator` + `WorkspaceOgImageListener` + Security
3. **PR 3 (이 리포)**: `OgController` + 응답 템플릿 + `application.yml`
4. **PR 4 (이 리포)**: `V08__BackfillOgCards.kt` (별도 PR로 분리하여 부하 모니터링 용이)
5. **(검증)** 스테이징에 배포, `curl`로 UA 위장 검증, 카카오 디버거 검증.
6. **Amplify 콘솔**: 스테이징 → 프로덕션 순으로 rewrite 룰 추가.
7. **백필 스크립트 실행** (PR 4의 스크립트가 자동 실행되거나 수동 트리거).
8. **모니터링**: Sentry 1~2일 관찰.

## 16. 롤백

- Amplify rewrite 룰 제거 → 즉시 이전 동작(정적 og:image)으로 복귀. 백엔드 코드는 그대로 두어도 무해.
- 컬럼 `og_image_url`은 NULL 허용이라 롤백 시에도 데이터 정합성 문제 없음.

## 17. 스코프 외 (향후 작업)

- 사장님이 카카오톡 미리보기를 직접 갱신할 수 있는 어드민 버튼 (카카오 디버거 URL을 새 창으로 열어주는 단순 링크).
- 어드민 워크스페이스 편집 페이지의 사진 업로드 영역 안내문 ("미리보기 갱신까지 최대 24시간").
- 어드민에서 OG 카드 프리뷰 (`<img src={workspace.ogImageUrl} />`).
- 워크스페이스 이외의 페이지(메뉴, 리뷰 등)에 OG 확장.
- 워크스페이스 삭제 시 og 카드 cascade delete.
- 사장님이 **대표 사진을 직접 지정**할 수 있는 UI (현재는 "가장 먼저 등록한 사진" 규칙).

## 18. 결정 로그

| 결정 | 선택 | 근거 |
|---|---|---|
| 호스팅 환경 대응 | Amplify User-Agent 리라이트 | Lambda@Edge나 prerender 서비스 대비 인프라 추가 0, 운영 단순. Amplify 공식 지원 패턴. |
| 카드 디자인 | 사진 + 우하단 알약 배지 | 워크스페이스 이름은 사진에 이미 박혀있어 중복. "이 주점은 키오스쿨 사용 중"만 알리면 충분. |
| 카드 합성 라이브러리 | scrimage | `S3Service`가 이미 사용 중. 일관성. AWT 직접 합성 대비 API 깔끔. |
| 합성 시점 | Eager (사진 변경 시점) | 크롤러 응답이 즉시 빠름. CDN 영구 캐시 안전. 운영 단순. |
| 트랜잭션 가시성 | `@TransactionalEventListener(AFTER_COMMIT)` | 합성은 race가 있으면 옛 사진으로 카드를 만들 수 있어 안전성 우선. |
| "대표 사진" 규칙 | `images.minByOrNull { it.id }` | 가장 먼저 등록한 사진. 별도 UI 없이 결정적. |
| OG 컨트롤러 위치 | `global/og/` | 워크스페이스 도메인에 종속될 이유 없음. 향후 다른 도메인 페이지로 확장 가능. |
| 폴백 | `kio-school.com/preview.png` | 글로벌 기본 이미지. 텍스트 카드 인프라를 만들 명분 없음. |
| 청소 정책 | S3 lifecycle 90일 | 외부 SNS 캐시 TTL을 충분히 넘김. 즉시 삭제 시 미리보기 깨짐 위험. |
| URL 범위 | `/order`만 | 다른 주문 플로우는 사용자가 직접 공유하지 않음. |
| 백필 | Eager (V08 스크립트) | 출시 직후부터 사장님 사진으로 노출. V07이 동일 패턴 선례. |
