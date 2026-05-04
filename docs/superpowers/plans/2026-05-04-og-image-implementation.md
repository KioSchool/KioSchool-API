# 워크스페이스별 동적 OG 이미지 백엔드 구현 Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 워크스페이스 사진 업로드 시 1200×630 OG 카드(사진 + 우하단 키오스쿨 배지)를 비동기로 합성·S3 업로드하고, 크롤러용 `/og/order` 엔드포인트가 워크스페이스별 og 메타태그가 박힌 미니 HTML을 반환하도록 한다.

**Architecture:** 두 갈래 흐름. (1) 합성 시점: 기존 `@WorkspaceUpdateEvent` AOP가 발행하는 `WorkspaceUpdatedEvent`를 신규 `WorkspaceOgImageListener`가 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`로 구독, 사진 hash 비교로 미변경 시 스킵하고 변경 시에만 `OgCardGenerator`가 scrimage로 합성→S3 업로드→`workspace.og_image_url` 갱신. (2) 응답 시점: Amplify가 User-Agent로 크롤러를 식별해 Spring `/og/order`로 200 리라이트, Spring은 ~1KB 미니 HTML을 반환. Amplify 룰은 본 리포 스코프 외, 별도 인프라 작업.

**Tech Stack:** Spring Boot 3.1.5 / Kotlin 1.9.22 / Java 17, Liquibase 4.24, scrimage 4.3.0 (`com.sksamuel.scrimage`), AWS SDK S3 1.12.595, Kotest 5.8 + MockK 1.13.9.

**스펙 vs 현실 차이 (확정 사항):**
- 스펙은 `Sentry.captureException`을 명시하지만 본 리포에 Sentry 의존성이 없음 → `org.slf4j.LoggerFactory` 기반 `logger.error("...", e)`로 대체. Sentry 도입은 별도 티켓.
- 스펙 §10은 `SecurityConfig`에 `/og/**` permitAll 추가를 명시하지만, `SecurityConfiguration.kt:42`가 이미 `requestMatchers("/**").permitAll()`을 기본값으로 둠 → **별도 보안 변경 불필요**. 본 plan에서는 작업 없음.
- 스펙은 `PngWriter.NoCompression`을 명시하지만 scrimage 4.3.0 API 호환성을 위해 안전하게 `PngWriter()` 기본 인스턴스로 시작. 결과물 사이즈 점검 후 필요시 후속 PR에서 튜닝.
- 백필 스크립트는 본 리포의 V07 패턴(`Runnable` + `@Component` + `OneTimeScheduler` 자동 실행)을 그대로 따름. 스펙의 `@PostConstruct` 등 가정은 사용 안 함.

**롤아웃 PR 분할 (스펙 §15 기반):**
- **PR1**: Task 1–3 (마이그레이션 + 엔티티 컬럼 + S3Service 확장)
- **PR2**: Task 4–6 (배지 리소스 + OgCardGenerator + WorkspaceOgImageListener)
- **PR3**: Task 7–8 (application.yml 키 + OgController)
- **PR4**: Task 9 (V08 백필 스크립트)

각 PR은 독립 배포 가능. PR1은 컬럼만 추가되어 무해, PR2는 데이터만 채워지고 사용처가 없어 무해, PR3에서 처음으로 외부 응답이 바뀜, PR4는 백필.

---

## File Structure

### 신규 파일
| 경로 | 책임 |
|---|---|
| `src/main/resources/db/changelog/2026/05/04-01-add-workspace-og-image-url.xml` | `workspace.og_image_url VARCHAR(512)` 컬럼 추가 |
| `src/main/resources/og/og-badge.png` | 디자이너 산출물 (투명 배경 알약 배지 PNG) |
| `src/main/kotlin/com/kioschool/kioschoolapi/domain/workspace/service/OgCardGenerator.kt` | 사진 다운로드 + cover 스케일 + 배지 overlay + S3 업로드 |
| `src/main/kotlin/com/kioschool/kioschoolapi/domain/workspace/listener/WorkspaceOgImageListener.kt` | `WorkspaceUpdatedEvent` 구독, hash 비교로 사진 변경 시에만 합성 트리거 |
| `src/main/kotlin/com/kioschool/kioschoolapi/global/og/controller/OgController.kt` | `/og/order` 엔드포인트, og:* 메타태그 미니 HTML 반환 |
| `src/main/kotlin/com/kioschool/kioschoolapi/global/schedule/script/V08__BackfillOgCards.kt` | 1회성 백필. 모든 워크스페이스에 대해 OG 카드 합성 |
| `src/test/kotlin/com/kioschool/kioschoolapi/workspace/service/OgCardGeneratorTest.kt` | OgCardGenerator 단위 테스트 |
| `src/test/kotlin/com/kioschool/kioschoolapi/workspace/listener/WorkspaceOgImageListenerTest.kt` | listener 단위 테스트 |
| `src/test/kotlin/com/kioschool/kioschoolapi/og/controller/OgControllerTest.kt` | OgController 단위 테스트 |

### 수정 파일
| 경로 | 변경 |
|---|---|
| `src/main/kotlin/com/kioschool/kioschoolapi/domain/workspace/entity/Workspace.kt` | `var ogImageUrl: String? = null` 필드 추가 |
| `src/main/kotlin/com/kioschool/kioschoolapi/global/aws/S3Service.kt` | `uploadBytes(...)`, `urlFor(...)` 메서드 2개 추가 |
| `src/main/resources/application.yml` | `kio.og.fallback-image-url` 키 추가 |

---

## Task 1: Liquibase changelog — `og_image_url` 컬럼 추가

**Files:**
- Create: `src/main/resources/db/changelog/2026/05/04-01-add-workspace-og-image-url.xml`

리소스 변경이라 TDD 사이클이 없음. 변경 → Liquibase 검증 → 커밋.

- [ ] **Step 1: changelog 파일 작성**

파일 내용:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  objectQuotingStrategy="QUOTE_ONLY_RESERVED_WORDS"
  xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
  xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                      http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.24.xsd">
  <changeSet author="rune" id="1777939200000-1">
    <addColumn tableName="workspace">
      <column name="og_image_url" type="VARCHAR(512)"/>
    </addColumn>
  </changeSet>
</databaseChangeLog>
```

**주의:**
- 기존 `2026/05/03-01-changelog.xml`과 동일한 폴더 컨벤션. master changelog `db/changelog-master.yaml`이 `includeAll: path: db/changelog/`로 자동 픽업하므로 별도 include 작업 없음.
- `id` 값은 epoch ms + suffix 컨벤션. 다른 changeSet과 충돌하지 않게 미래 epoch(2026-05-04 ~)로 설정.
- `nullable=true` 기본값. 컬럼이 처음에는 NULL로 채워지고 listener/백필이 점진 채움.

- [ ] **Step 2: Liquibase 검증 — 로컬 부트런으로 마이그레이션 적용**

Run:
```bash
./gradlew bootRun --args='--spring.profiles.active=local' 2>&1 | grep -i 'changeset\|liquibase\|error' | head -20
```

Expected: `1777939200000-1` changeSet이 성공적으로 적용된 로그. 에러 없음.

대안 검증 (DB 직접 확인, local 프로필이 H2/postgres에 따라 다름):
```bash
./gradlew test --tests "*FlywayMigrationTest*" 2>&1 | tail -5
```

(없으면 스킵 가능. CI에서 자동 검증됨.)

- [ ] **Step 3: 커밋**

```bash
git add src/main/resources/db/changelog/2026/05/04-01-add-workspace-og-image-url.xml
git commit -m "feat(workspace): add og_image_url column"
```

---

## Task 2: Workspace 엔티티에 `ogImageUrl` 필드 추가

**Files:**
- Modify: `src/main/kotlin/com/kioschool/kioschoolapi/domain/workspace/entity/Workspace.kt:60` (마지막 필드 `workspaceSetting` 다음)

엔티티 컬럼 추가는 JPA dirty checking으로 동작 검증되며 별도 단위 테스트가 가치 없음. 컴파일 + Liquibase 매핑 검증으로 충분.

- [ ] **Step 1: Workspace.kt에 필드 추가**

`Workspace.kt`의 생성자 파라미터 마지막에 추가. 현재 파일은 `workspaceSetting: WorkspaceSetting,` 다음에 `) : BaseEntity()`로 끝남.

변경 전 (`Workspace.kt:55-62`):
```kotlin
    var isOnboarding: Boolean = true,
    @OneToOne(
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    var workspaceSetting: WorkspaceSetting,
) : BaseEntity()
```

변경 후:
```kotlin
    var isOnboarding: Boolean = true,
    @OneToOne(
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    var workspaceSetting: WorkspaceSetting,
    @Column(name = "og_image_url")
    var ogImageUrl: String? = null,
) : BaseEntity()
```

`@Column(name = "og_image_url")` 명시 — Hibernate naming strategy가 camelCase → snake_case로 자동 변환하지만, 마이그레이션과 매핑 일치를 코드에서 명확히 하기 위해 명시.

- [ ] **Step 2: 컴파일 확인**

Run:
```bash
./gradlew compileKotlin 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: 기존 테스트 회귀 확인**

`WorkspaceServiceTest.kt:34-43`처럼 `Workspace` 생성자를 직접 호출하는 테스트가 있을 수 있음. nullable 기본값 `null`이므로 기존 호출 깨지지 않음.

Run:
```bash
./gradlew test --tests "*Workspace*" 2>&1 | tail -20
```

Expected: 모든 워크스페이스 관련 테스트 PASS.

- [ ] **Step 4: 커밋**

```bash
git add src/main/kotlin/com/kioschool/kioschoolapi/domain/workspace/entity/Workspace.kt
git commit -m "feat(workspace): add ogImageUrl field to Workspace entity"
```

---

## Task 3: `S3Service`에 `uploadBytes` / `urlFor` 메서드 추가

**Files:**
- Modify: `src/main/kotlin/com/kioschool/kioschoolapi/global/aws/S3Service.kt:48` (마지막 메서드 `deleteFile` 다음)
- Test: 기존 `S3Service` 테스트가 없으면 신규는 만들지 않고 통합 테스트(`OgCardGeneratorTest`)로 간접 검증.

- [ ] **Step 1: 실패하는 테스트 작성 — `OgCardGeneratorTest`에서 간접 호출 예정**

S3Service는 외부 AWS와 통신하는 얇은 어댑터라 단독 단위 테스트는 가치가 낮음(MockK로 `amazonS3Client.putObject` 호출 검증만 가능). `OgCardGenerator` 테스트에서 `s3Service.uploadBytes` 호출을 verify하는 형태로 검증한다. 본 task에서는 메서드만 추가하고 컴파일·기존 회귀만 확인.

- [ ] **Step 2: `S3Service`에 두 메서드 추가**

`S3Service.kt:43-47`의 `deleteFile` 다음에 추가.

기존 import (파일 상단)에 `java.io.ByteArrayInputStream`이 있는지 확인하고, 없으면 추가.

추가할 메서드:

```kotlin
fun uploadBytes(bytes: ByteArray, path: String, contentType: String): String {
    val metadata = ObjectMetadata().apply {
        contentLength = bytes.size.toLong()
        this.contentType = contentType
        cacheControl = "public, max-age=31536000, immutable"
    }
    amazonS3Client.putObject(
        bucketName,
        path,
        ByteArrayInputStream(bytes),
        metadata
    )
    return amazonS3Client.getUrl(bucketName, path).toString()
}

fun urlFor(path: String): String =
    amazonS3Client.getUrl(bucketName, path).toString()
```

설계 결정:
- `Cache-Control: public, max-age=31536000, immutable`은 OG 카드 한정 정책이지만 `uploadBytes`는 범용 시그니처. 본 단계에선 OG 한정 사용처만 있어 무해. 추후 다른 사용처가 생기면 파라미터로 헤더를 받도록 시그니처 확장.
- `urlFor`는 S3 PUT 없이 키 → 퍼블릭 URL만 계산. listener의 hash 선검사에서 사용.

- [ ] **Step 3: 컴파일 + 기존 S3 사용처 회귀 확인**

Run:
```bash
./gradlew compileKotlin compileTestKotlin 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: 커밋**

```bash
git add src/main/kotlin/com/kioschool/kioschoolapi/global/aws/S3Service.kt
git commit -m "feat(s3): add uploadBytes and urlFor helpers"
```

---

## Task 4: 디자이너 산출물 `og-badge.png` 리소스 커밋

**Files:**
- Create: `src/main/resources/og/og-badge.png`

본 단계는 디자이너에게 산출물을 받는 외부 의존이 있다. 디자이너 산출물이 없는 동안 합성 코드를 막을 필요는 없으므로 **placeholder PNG**(투명 1×1 또는 임시 알약 형태)를 우선 커밋하고, 디자이너 산출물 도착 시 동일 경로로 교체하는 후속 PR을 만든다.

- [ ] **Step 1: 디자이너 산출물 또는 placeholder PNG를 `src/main/resources/og/og-badge.png` 위치에 둔다**

요구사항(스펙 §5.1):
- 투명 배경 PNG.
- 권장 사이즈 ~336×88 (2x).
- 키오스쿨 로고 + "키오스쿨" 텍스트, 검정 반투명(0.55) + blur 알약.

placeholder가 필요하면 다음 명령으로 임시 투명 1×1 PNG 생성 (디자이너 산출물 도착 시 즉시 교체):
```bash
mkdir -p src/main/resources/og
printf '\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\rIDATx\x9cc\xfa\xcf\x00\x00\x00\x02\x00\x01\xe5\'\xde\xfc\x00\x00\x00\x00IEND\xaeB`\x82' > src/main/resources/og/og-badge.png
```

(주의: 위 placeholder는 1×1 투명 PNG. 합성 결과 카드는 사진만 보이고 배지가 안 보일 수 있음 — 디자이너 산출물 도착 후 교체 필수.)

- [ ] **Step 2: 리소스가 클래스패스에서 로드 가능한지 확인**

```bash
./gradlew processResources 2>&1 | tail -5
ls -la build/resources/main/og/og-badge.png
```

Expected: 파일이 build 디렉토리에 복사됨.

- [ ] **Step 3: 커밋**

```bash
git add src/main/resources/og/og-badge.png
git commit -m "feat(og): add og-badge placeholder asset"
```

(디자이너 산출물 도착 시 후속 커밋: `git commit -m "chore(og): replace badge placeholder with designer artwork"`)

---

## Task 5: `OgCardGenerator` 작성

**Files:**
- Create: `src/main/kotlin/com/kioschool/kioschoolapi/domain/workspace/service/OgCardGenerator.kt`
- Test: `src/test/kotlin/com/kioschool/kioschoolapi/workspace/service/OgCardGeneratorTest.kt`
- Test fixture: `src/test/resources/og/test-photo.jpg` (작은 샘플 사진, 100×100 정도)

설계 결정:
- 배지 PNG는 `lazy`로 한 번만 로드 (합성 빈번 시 GC 압박 방지).
- 사진 다운로드는 `S3Service.downloadFileStream` 재사용 — 외부 사진 URL이 우리 S3 도메인이 아닐 경우 대비해 SSRF 방어 필요. 본 단계에서는 입력이 항상 우리 워크스페이스 사진 URL(`workspace.images.url`)이므로 추가 검증 생략. 단 `downloadFileStream`이 `URI(url).toURL().openStream()`이라 외부 URL이 들어오면 그대로 fetch한다는 점은 인지 — 후속 보안 강화 시 도메인 화이트리스트 추가.
- `cover(1200, 630)`은 사진을 비율 유지하며 잘라서 1200×630에 맞춤. scrimage 4.3.0 정상 지원.
- `overlay(badge, x, y)`은 우상단이 아닌 좌상단 좌표 기준. 우하단 26/22px 마진 = `x = 1200 - 26 - badge.width`, `y = 630 - 22 - badge.height`.

- [ ] **Step 1: 테스트 fixture 준비**

`src/test/resources/og/` 디렉토리 생성, 100×100 정도의 작은 JPG를 둔다.

```bash
mkdir -p src/test/resources/og
# 실제 작은 JPG (수동으로 받거나 fixture 도구 사용)
# 임시: 작은 PNG로 대체 (scrimage가 PNG도 처리)
cp src/main/resources/og/og-badge.png src/test/resources/og/test-photo.jpg 2>/dev/null || true
```

(실제 fixture는 100×100 이상 더미 JPG 권장. scrimage는 1×1도 cover 가능하지만 의미있는 검증을 위해 더 큰 이미지 권장.)

- [ ] **Step 2: 실패하는 테스트 작성 — `OgCardGeneratorTest.kt`**

```kotlin
package com.kioschool.kioschoolapi.workspace.service

import com.kioschool.kioschoolapi.domain.workspace.service.OgCardGenerator
import com.kioschool.kioschoolapi.global.aws.S3Service
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream

class OgCardGeneratorTest : DescribeSpec({
    val s3Service = mockk<S3Service>()
    val sut = OgCardGenerator(s3Service, workspacePath = "test-path")

    fun loadFixture(name: String): ByteArray =
        OgCardGeneratorTest::class.java.getResourceAsStream("/og/$name")!!.readBytes()

    describe("generate") {
        it("uploads a 1200x630 PNG to the expected key and returns its URL") {
            val photoBytes = loadFixture("test-photo.jpg")
            val photoUrl = "https://bucket.s3.ap-northeast-2.amazonaws.com/foo.jpg"
            every { s3Service.downloadFileStream(photoUrl) } returns ByteArrayInputStream(photoBytes)
            val capturedBytes = slot<ByteArray>()
            val capturedPath = slot<String>()
            every { s3Service.uploadBytes(capture(capturedBytes), capture(capturedPath), "image/png") } returns
                "https://bucket.s3.ap-northeast-2.amazonaws.com/test-path/workspace42/og/HASH8CHR.png"

            val resultUrl = sut.generate(workspaceId = 42L, sourcePhotoUrl = photoUrl)

            assert(capturedPath.captured.startsWith("test-path/workspace42/og/"))
            assert(capturedPath.captured.endsWith(".png"))
            assert(capturedBytes.captured.isNotEmpty())
            assert(resultUrl.contains("test-path/workspace42/og/"))
            verify { s3Service.uploadBytes(any(), any(), "image/png") }
        }

        it("uses sha1(sourceUrl).take(8) as the filename hash so different photos produce different keys") {
            val photoBytes = loadFixture("test-photo.jpg")
            every { s3Service.downloadFileStream(any()) } returns ByteArrayInputStream(photoBytes)
            val paths = mutableListOf<String>()
            every { s3Service.uploadBytes(any(), capture(paths), any()) } returns "url"

            sut.generate(1L, "https://example/a.jpg")
            sut.generate(1L, "https://example/b.jpg")

            assert(paths[0] != paths[1]) { "Different sources should yield different hashes" }
        }

        it("expectedUrl matches the path used by generate for the same input") {
            val photoBytes = loadFixture("test-photo.jpg")
            every { s3Service.downloadFileStream(any()) } returns ByteArrayInputStream(photoBytes)
            val uploadPath = slot<String>()
            every { s3Service.uploadBytes(any(), capture(uploadPath), any()) } returns "ignored"
            every { s3Service.urlFor(any()) } answers { "computed:${firstArg<String>()}" }

            sut.generate(7L, "https://example/x.jpg")
            val expected = sut.expectedUrl(7L, "https://example/x.jpg")

            verify { s3Service.urlFor(uploadPath.captured) }
            assert(expected == "computed:${uploadPath.captured}")
        }

        it("propagates the exception when the source photo cannot be decoded as an image") {
            every { s3Service.downloadFileStream(any()) } returns ByteArrayInputStream(byteArrayOf(0, 1, 2, 3))

            assertThrows<Exception> {
                sut.generate(1L, "https://example/broken")
            }
        }
    }
})
```

- [ ] **Step 3: 테스트 실행 — 실패 확인**

Run:
```bash
./gradlew test --tests "*OgCardGeneratorTest*" 2>&1 | tail -15
```

Expected: FAIL with `Unresolved reference: OgCardGenerator`.

- [ ] **Step 4: `OgCardGenerator.kt` 구현**

```kotlin
package com.kioschool.kioschoolapi.domain.workspace.service

import com.kioschool.kioschoolapi.global.aws.S3Service
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest

@Service
class OgCardGenerator(
    private val s3Service: S3Service,
    @Value("\${cloud.aws.s3.default-path}")
    private val workspacePath: String,
) {
    private val logger = LoggerFactory.getLogger(OgCardGenerator::class.java)

    private val badge: ImmutableImage by lazy {
        val resource = javaClass.getResourceAsStream("/og/og-badge.png")
            ?: error("og-badge.png not found on classpath at /og/og-badge.png")
        resource.use { ImmutableImage.loader().fromStream(it) }
    }

    fun generate(workspaceId: Long, sourcePhotoUrl: String): String {
        val photo = s3Service.downloadFileStream(sourcePhotoUrl).use {
            ImmutableImage.loader().fromStream(it)
        }
        val card = photo.cover(CARD_WIDTH, CARD_HEIGHT)
            .overlay(
                badge,
                CARD_WIDTH - BADGE_MARGIN_X - badge.width,
                CARD_HEIGHT - BADGE_MARGIN_Y - badge.height,
            )
        val bytes = card.bytes(PngWriter())
        val path = pathFor(workspaceId, sourcePhotoUrl)
        val url = s3Service.uploadBytes(bytes, path, "image/png")
        logger.info("OG card generated: workspaceId={}, path={}", workspaceId, path)
        return url
    }

    fun expectedUrl(workspaceId: Long, sourcePhotoUrl: String): String =
        s3Service.urlFor(pathFor(workspaceId, sourcePhotoUrl))

    private fun pathFor(workspaceId: Long, sourcePhotoUrl: String): String {
        val hash = sha1(sourcePhotoUrl).take(8)
        return "$workspacePath/workspace${workspaceId}/og/$hash.png"
    }

    private fun sha1(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val CARD_WIDTH = 1200
        const val CARD_HEIGHT = 630
        const val BADGE_MARGIN_X = 26
        const val BADGE_MARGIN_Y = 22
    }
}
```

설계 결정 추가:
- `workspacePath` 키는 기존 `WorkspaceService.kt:25-26`의 `cloud.aws.s3.default-path`를 동일하게 재사용 — 한 워크스페이스의 사진과 OG 카드가 같은 prefix 아래 묶임.
- SHA-1 hash로 8자 — 충돌 가능성 1/2^32 워크스페이스 내, 무시 가능.
- 합성 실패 시 예외는 그대로 throw — listener가 잡아서 `logger.error`로 처리.

- [ ] **Step 5: 테스트 실행 — 통과 확인**

Run:
```bash
./gradlew test --tests "*OgCardGeneratorTest*" 2>&1 | tail -20
```

Expected: 4개 테스트 모두 PASS.

테스트가 fixture 이미지 디코딩에 실패하면(예: placeholder PNG 1×1로 cover 시도 시 일부 scrimage 버전이 실패), fixture를 더 크고 의미있는 JPG/PNG로 교체.

- [ ] **Step 6: 커밋**

```bash
git add src/main/kotlin/com/kioschool/kioschoolapi/domain/workspace/service/OgCardGenerator.kt \
        src/test/kotlin/com/kioschool/kioschoolapi/workspace/service/OgCardGeneratorTest.kt \
        src/test/resources/og/test-photo.jpg
git commit -m "feat(og): add OgCardGenerator for photo+badge composition"
```

---

## Task 6: `WorkspaceOgImageListener` 작성

**Files:**
- Create: `src/main/kotlin/com/kioschool/kioschoolapi/domain/workspace/listener/WorkspaceOgImageListener.kt`
- Test: `src/test/kotlin/com/kioschool/kioschoolapi/workspace/listener/WorkspaceOgImageListenerTest.kt`

설계 결정 (스펙 §8.5 기반, 본 리포 차이점 반영):
- `WorkspaceUpdatedEvent`는 사진 외 변경(이름/메모/테이블 수 등)에도 발행됨(`WorkspaceEventAspect.kt:21`). hash 선검사로 사진 미변경 시 스킵 → 다운로드/합성/PUT 비용 0.
- `@TransactionalEventListener(AFTER_COMMIT)` — 트랜잭션 커밋 후에 listener가 fire되어야 `workspace.images`의 최종 상태를 본다. 기존 `WorkspaceCacheEvictListener`는 `@EventListener`(즉시)로 evict하지만, OG 합성은 race 시 옛 사진으로 카드를 만들 수 있어 안전성을 우선.
- `@Async` + `@Transactional` 조합 — listener가 별도 스레드에서 실행되며 자체 트랜잭션 시작. `workspace.images`가 lazy 컬렉션이라 트랜잭션 없이 접근 시 `LazyInitializationException`.
- 본 리포 `kotlin-spring` 플러그인이 `@Component`에 자동 open 적용 (`build.gradle.kts:13-23` allOpen 설정에 `org.springframework.stereotype.Component` 포함) → 프록시 정상 동작.
- 합성 실패 시 `runCatching` → `logger.error`로 캡처 + `og_image_url` 변경 안 함(이전 카드 보존). Sentry 도입 시 `logger.error` → `Sentry.captureException` 마이그레이션.
- listener 안의 `workspaceRepository.save`는 `@WorkspaceUpdateEvent` 어노테이션이 없는 호출이라 AOP 재트리거 없음 → 무한 루프 없음.

- [ ] **Step 1: 실패하는 테스트 작성 — `WorkspaceOgImageListenerTest.kt`**

```kotlin
package com.kioschool.kioschoolapi.workspace.listener

import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceImage
import com.kioschool.kioschoolapi.domain.workspace.event.WorkspaceUpdatedEvent
import com.kioschool.kioschoolapi.domain.workspace.listener.WorkspaceOgImageListener
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.domain.workspace.service.OgCardGenerator
import com.kioschool.kioschoolapi.factory.SampleEntity
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

class WorkspaceOgImageListenerTest : DescribeSpec({
    val workspaceRepository = mockk<WorkspaceRepository>()
    val ogCardGenerator = mockk<OgCardGenerator>()
    val sut = WorkspaceOgImageListener(workspaceRepository, ogCardGenerator)

    fun workspaceWith(images: List<String>, ogImageUrl: String? = null): Workspace {
        val ws = SampleEntity.workspace
        ws.ogImageUrl = ogImageUrl
        ws.images.clear()
        images.forEachIndexed { idx, url ->
            // SampleEntity.workspace.id is fixed; WorkspaceImage.id stays 0 by default.
            // Use reflection or factory to set distinct ids if minByOrNull resolution matters.
            val img = WorkspaceImage(workspace = ws, url = url)
            // WorkspaceImage extends BaseEntity with `val id: Long = 0`. Set via reflection for test ordering.
            val idField = img.javaClass.superclass.getDeclaredField("id")
            idField.isAccessible = true
            idField.setLong(img, (idx + 1).toLong())
            ws.images.add(img)
        }
        return ws
    }

    describe("on(WorkspaceUpdatedEvent)") {
        it("regenerates and saves ogImageUrl when the primary photo's expected url differs") {
            val ws = workspaceWith(images = listOf("https://photo/a.jpg"), ogImageUrl = null)
            every { workspaceRepository.findById(ws.id) } returns Optional.of(ws)
            every { ogCardGenerator.expectedUrl(ws.id, "https://photo/a.jpg") } returns "https://og/a-hash.png"
            every { ogCardGenerator.generate(ws.id, "https://photo/a.jpg") } returns "https://og/a-hash.png"
            every { workspaceRepository.save(ws) } returns ws

            sut.on(WorkspaceUpdatedEvent(ws.id))

            assert(ws.ogImageUrl == "https://og/a-hash.png")
            verify { ogCardGenerator.generate(ws.id, "https://photo/a.jpg") }
            verify { workspaceRepository.save(ws) }
        }

        it("skips generate and save when ogImageUrl already matches expected hash (no photo change)") {
            val ws = workspaceWith(
                images = listOf("https://photo/a.jpg"),
                ogImageUrl = "https://og/a-hash.png",
            )
            every { workspaceRepository.findById(ws.id) } returns Optional.of(ws)
            every { ogCardGenerator.expectedUrl(ws.id, "https://photo/a.jpg") } returns "https://og/a-hash.png"

            sut.on(WorkspaceUpdatedEvent(ws.id))

            verify(exactly = 0) { ogCardGenerator.generate(any(), any()) }
            verify(exactly = 0) { workspaceRepository.save(any()) }
        }

        it("clears ogImageUrl to null when the workspace has no images and a previous og card existed") {
            val ws = workspaceWith(images = emptyList(), ogImageUrl = "https://og/old.png")
            every { workspaceRepository.findById(ws.id) } returns Optional.of(ws)
            every { workspaceRepository.save(ws) } returns ws

            sut.on(WorkspaceUpdatedEvent(ws.id))

            assert(ws.ogImageUrl == null)
            verify { workspaceRepository.save(ws) }
        }

        it("preserves existing ogImageUrl when generation throws (Sentry-style soft failure)") {
            val ws = workspaceWith(
                images = listOf("https://photo/broken.jpg"),
                ogImageUrl = "https://og/previous.png",
            )
            every { workspaceRepository.findById(ws.id) } returns Optional.of(ws)
            every { ogCardGenerator.expectedUrl(ws.id, "https://photo/broken.jpg") } returns "https://og/new.png"
            every { ogCardGenerator.generate(ws.id, "https://photo/broken.jpg") } throws RuntimeException("boom")

            sut.on(WorkspaceUpdatedEvent(ws.id))

            assert(ws.ogImageUrl == "https://og/previous.png")
            verify(exactly = 0) { workspaceRepository.save(any()) }
        }

        it("uses the image with the smallest id as the primary photo") {
            val ws = workspaceWith(
                images = listOf("https://photo/first.jpg", "https://photo/second.jpg"),
                ogImageUrl = null,
            )
            every { workspaceRepository.findById(ws.id) } returns Optional.of(ws)
            every { ogCardGenerator.expectedUrl(ws.id, "https://photo/first.jpg") } returns "https://og/first-hash.png"
            every { ogCardGenerator.generate(ws.id, "https://photo/first.jpg") } returns "https://og/first-hash.png"
            every { workspaceRepository.save(ws) } returns ws

            sut.on(WorkspaceUpdatedEvent(ws.id))

            verify { ogCardGenerator.generate(ws.id, "https://photo/first.jpg") }
            verify(exactly = 0) { ogCardGenerator.generate(ws.id, "https://photo/second.jpg") }
        }

        it("does nothing when the workspace cannot be found") {
            every { workspaceRepository.findById(99L) } returns Optional.empty()

            sut.on(WorkspaceUpdatedEvent(99L))

            verify(exactly = 0) { ogCardGenerator.generate(any(), any()) }
            verify(exactly = 0) { workspaceRepository.save(any()) }
        }
    }
})
```

**전제: 기존 `factory/SampleEntity` 활용** — `WorkspaceFacadeTest.kt:38`이 `SampleEntity.workspace`를 사용함을 확인. SampleEntity 위치는 `src/test/kotlin/com/kioschool/kioschoolapi/factory/SampleEntity.kt` 가정. 만약 `images`가 mutable이 아니거나 reflection 접근이 까다로우면 본 테스트에서 `Workspace`를 직접 생성자로 만들어 사용하도록 조정.

- [ ] **Step 2: 테스트 실행 — 실패 확인**

Run:
```bash
./gradlew test --tests "*WorkspaceOgImageListenerTest*" 2>&1 | tail -15
```

Expected: FAIL with `Unresolved reference: WorkspaceOgImageListener`.

- [ ] **Step 3: `WorkspaceOgImageListener.kt` 구현**

```kotlin
package com.kioschool.kioschoolapi.domain.workspace.listener

import com.kioschool.kioschoolapi.domain.workspace.event.WorkspaceUpdatedEvent
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.domain.workspace.service.OgCardGenerator
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class WorkspaceOgImageListener(
    private val workspaceRepository: WorkspaceRepository,
    private val ogCardGenerator: OgCardGenerator,
) {
    private val logger = LoggerFactory.getLogger(WorkspaceOgImageListener::class.java)

    @Async
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: WorkspaceUpdatedEvent) {
        val workspace = workspaceRepository.findById(event.workspaceId).orElse(null) ?: return
        val primaryPhotoUrl = workspace.images.minByOrNull { it.id }?.url

        // Hash precheck: skip when the source photo has not changed.
        // WorkspaceUpdatedEvent fires for non-photo changes too (name/memo/etc.).
        val expectedOgUrl = primaryPhotoUrl?.let { ogCardGenerator.expectedUrl(workspace.id, it) }
        if (workspace.ogImageUrl == expectedOgUrl) return

        val newOgUrl: String? = if (primaryPhotoUrl == null) {
            null
        } else {
            runCatching { ogCardGenerator.generate(workspace.id, primaryPhotoUrl) }
                .getOrElse { ex ->
                    // Preserve the previously-good og card. Surfacing via logger.error keeps
                    // the user-facing flow intact; Sentry can be wired in later.
                    logger.error(
                        "OG card generation failed for workspaceId={}, source={}",
                        workspace.id,
                        primaryPhotoUrl,
                        ex,
                    )
                    return
                }
        }
        workspace.ogImageUrl = newOgUrl
        workspaceRepository.save(workspace)
    }
}
```

- [ ] **Step 4: 테스트 실행 — 통과 확인**

Run:
```bash
./gradlew test --tests "*WorkspaceOgImageListenerTest*" 2>&1 | tail -20
```

Expected: 6개 테스트 모두 PASS.

- [ ] **Step 5: 통합 회귀 — 기존 `WorkspaceCacheEvictListener` 동작이 깨지지 않는지 확인**

Run:
```bash
./gradlew test --tests "*Workspace*" 2>&1 | tail -20
```

Expected: 모든 워크스페이스 관련 테스트 PASS.

- [ ] **Step 6: 커밋**

```bash
git add src/main/kotlin/com/kioschool/kioschoolapi/domain/workspace/listener/WorkspaceOgImageListener.kt \
        src/test/kotlin/com/kioschool/kioschoolapi/workspace/listener/WorkspaceOgImageListenerTest.kt
git commit -m "feat(og): regenerate workspace og image on photo change"
```

---

## Task 7: `application.yml`에 폴백 이미지 키 추가

**Files:**
- Modify: `src/main/resources/application.yml:88` (마지막 라인 다음)

- [ ] **Step 1: yml 파일 끝에 키 추가**

`application.yml`의 가장 마지막에 새 블록 추가:

```yaml
kio:
  og:
    fallback-image-url: "https://kio-school.com/preview.png"
```

설계 결정:
- 환경 변수 미오버라이드 시 prod/dev 모두 동일 글로벌 fallback 사용. 환경별로 다르게 하고 싶으면 `application-prod.yml` / `application-dev.yml`에 동일 키로 오버라이드.

- [ ] **Step 2: 컴파일 + 부트런 검증**

Run:
```bash
./gradlew compileKotlin 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL. (이 단계에선 키를 사용하는 곳이 아직 없어 ConfigurationProperties 검증은 안 됨.)

- [ ] **Step 3: 커밋**

```bash
git add src/main/resources/application.yml
git commit -m "chore(og): add kio.og.fallback-image-url config key"
```

---

## Task 8: `OgController` 작성

**Files:**
- Create: `src/main/kotlin/com/kioschool/kioschoolapi/global/og/controller/OgController.kt`
- Test: `src/test/kotlin/com/kioschool/kioschoolapi/og/controller/OgControllerTest.kt`

설계 결정 (스펙 §8.3):
- `produces = MediaType.TEXT_HTML_VALUE` — 카카오/페북 등 크롤러는 HTML을 기대.
- `Cache-Control: public, max-age=600` — 같은 워크스페이스 빠른 재조회는 nginx/CDN이 흡수. og_image_url은 hash 기반 immutable URL이라 600s 동안 stale도 안전.
- `workspaceId` null/없음/오류 → 폴백 200 응답. 봇에 4xx 주면 미리보기만 깨짐.
- 본문은 빈 `<body></body>` — Amplify rewrite UA 조건을 통과한 봇만 도달하므로 사람 노출 사실상 없음.
- HTML 빌드는 `String.trimIndent()` + simple escape. og:title에 워크스페이스 이름이 들어가는 것을 스펙은 허용(§8.3). HTML escape 필수.

- [ ] **Step 1: 실패하는 테스트 작성 — `OgControllerTest.kt`**

```kotlin
package com.kioschool.kioschoolapi.og.controller

import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.og.controller.OgController
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import java.util.Optional

class OgControllerTest : DescribeSpec({
    val workspaceRepository = mockk<WorkspaceRepository>()
    val sut = OgController(
        workspaceRepository = workspaceRepository,
        fallbackImageUrl = "https://kio-school.com/preview.png",
    )

    describe("ogOrder") {
        it("renders og:image=ogImageUrl and og:title with workspace name when found") {
            val ws: Workspace = SampleEntity.workspace.apply {
                name = "테스트주점"
                ogImageUrl = "https://og/test.png"
            }
            every { workspaceRepository.findById(ws.id) } returns Optional.of(ws)

            val response = sut.ogOrder(ws.id)
            val body = response.body!!

            assert(response.statusCode.value() == 200)
            assert(body.contains("""<meta property="og:image"""))
            assert(body.contains("https://og/test.png"))
            assert(body.contains("테스트주점"))
            val cacheControl = response.headers.cacheControl ?: ""
            assert(cacheControl.contains("max-age=600"))
            assert(cacheControl.contains("public"))
        }

        it("falls back to global preview image when workspaceId is null") {
            val response = sut.ogOrder(null)
            val body = response.body!!

            assert(body.contains("https://kio-school.com/preview.png"))
            assert(!body.contains("og:image\" content=\"\""))
        }

        it("falls back when workspaceId does not match any workspace") {
            every { workspaceRepository.findById(999L) } returns Optional.empty()

            val response = sut.ogOrder(999L)
            val body = response.body!!

            assert(body.contains("https://kio-school.com/preview.png"))
            assert(response.statusCode.value() == 200)
        }

        it("falls back when workspace exists but has no ogImageUrl yet") {
            val ws: Workspace = SampleEntity.workspace.apply { ogImageUrl = null }
            every { workspaceRepository.findById(ws.id) } returns Optional.of(ws)

            val response = sut.ogOrder(ws.id)

            assert(response.body!!.contains("https://kio-school.com/preview.png"))
        }

        it("escapes HTML special characters in workspace name to prevent meta injection") {
            val ws: Workspace = SampleEntity.workspace.apply {
                name = """Tap"House <script>"""
                ogImageUrl = "https://og/x.png"
            }
            every { workspaceRepository.findById(ws.id) } returns Optional.of(ws)

            val body = sut.ogOrder(ws.id).body!!

            assert(!body.contains("<script>"))
            assert(body.contains("&lt;script&gt;") || body.contains("&quot;"))
        }

        it("includes a canonical og:url with the workspaceId query string") {
            val ws: Workspace = SampleEntity.workspace.apply { ogImageUrl = "https://og/x.png" }
            every { workspaceRepository.findById(ws.id) } returns Optional.of(ws)

            val body = sut.ogOrder(ws.id).body!!

            assert(body.contains("https://kio-school.com/order?workspaceId=${ws.id}"))
        }
    }
})
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

Run:
```bash
./gradlew test --tests "*OgControllerTest*" 2>&1 | tail -15
```

Expected: FAIL with `Unresolved reference: OgController`.

- [ ] **Step 3: `OgController.kt` 구현**

```kotlin
package com.kioschool.kioschoolapi.global.og.controller

import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

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

    private fun renderOgHtml(canonical: String, title: String, image: String): String {
        val titleEsc = htmlEscape(title)
        val canonicalEsc = htmlEscape(canonical)
        val imageEsc = htmlEscape(image)
        return """
            <!doctype html>
            <html lang="ko"><head>
              <meta charset="utf-8">
              <meta property="og:url"         content="$canonicalEsc">
              <meta property="og:title"       content="$titleEsc">
              <meta property="og:description" content="대학 주점 테이블 오더 서비스, 키오스쿨입니다!">
              <meta property="og:type"        content="website">
              <meta property="og:image"       content="$imageEsc">
              <meta property="og:site_name"   content="키오스쿨">
              <title>$titleEsc</title>
            </head><body></body></html>
        """.trimIndent()
    }

    private fun htmlEscape(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
}
```

설계 결정:
- HTML escape는 표준 5문자 변환. 워크스페이스 이름에 한글/이모지/일반 기호는 그대로 통과.
- og:description은 글로벌 고정 문구. 향후 워크스페이스별 description 노출이 필요하면 확장.

- [ ] **Step 4: 테스트 실행 — 통과 확인**

Run:
```bash
./gradlew test --tests "*OgControllerTest*" 2>&1 | tail -20
```

Expected: 6개 테스트 모두 PASS.

- [ ] **Step 5: 보안 매처 영향 확인 (변경 없음 검증)**

`SecurityConfiguration.kt:42`에서 `requestMatchers("/**").permitAll()`이 catch-all로 동작하므로 `/og/**` 별도 룰 불필요. 컨트롤러가 등록되었는지 부트런으로 확인:

```bash
./gradlew bootRun --args='--spring.profiles.active=local' 2>&1 | grep -i 'GET .*/og/order' | head -3
```

Expected: `Mapped "{[/og/order],methods=[GET]...}"` 비슷한 라인.

(bootRun이 무거우면 통합 테스트 또는 그냥 `./gradlew compileKotlin`으로 대체.)

- [ ] **Step 6: 커밋**

```bash
git add src/main/kotlin/com/kioschool/kioschoolapi/global/og/controller/OgController.kt \
        src/test/kotlin/com/kioschool/kioschoolapi/og/controller/OgControllerTest.kt
git commit -m "feat(og): add /og/order endpoint with workspace meta tags"
```

---

## Task 9: `V08__BackfillOgCards` — 1회성 백필 스크립트

**Files:**
- Create: `src/main/kotlin/com/kioschool/kioschoolapi/global/schedule/script/V08__BackfillOgCards.kt`
- Test: 백필 스크립트는 V07도 단위 테스트가 없음 (스테이징 검증으로 대체). 필요 시 가벼운 단위 테스트 추가.

설계 결정 (V07 패턴 그대로):
- `Runnable` 인터페이스 구현 + `@Component` → `OneTimeScheduler`가 앱 시작 5초 후 자동 실행.
- 멱등성: `OneTimeScheduler.kt:27`이 `ExecutedOneTimeScript` 테이블로 한 번만 실행 보장. 또한 워크스페이스 단위로도 `ogImageUrl != null`이면 스킵 → 부분 실패 후 재시작 시 재진입 안전.
- 로컬 환경 스킵 (V07 패턴, `V07__ResizeExistingImages.kt:28-32`).
- `@Transactional(propagation = Propagation.NOT_SUPPORTED)` (V07 패턴, `V07__ResizeExistingImages.kt:27`) — 긴 트랜잭션 회피, 워크스페이스별로 짧은 트랜잭션을 감.
- 합성 실패 시 logger.error만 남기고 다음 워크스페이스 진행 — 한 워크스페이스의 깨진 사진이 전체 백필을 막으면 안 됨.

- [ ] **Step 1: `V08__BackfillOgCards.kt` 작성**

```kotlin
package com.kioschool.kioschoolapi.global.schedule.script

import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.domain.workspace.service.OgCardGenerator
import com.kioschool.kioschoolapi.global.schedule.Runnable
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class V08__BackfillOgCards(
    private val workspaceRepository: WorkspaceRepository,
    private val ogCardGenerator: OgCardGenerator,
    private val environment: Environment,
) : Runnable {
    private val logger = LoggerFactory.getLogger(V08__BackfillOgCards::class.java)

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun run() {
        if (environment.activeProfiles.any { it == "local" || it == "default" } || environment.activeProfiles.isEmpty()) {
            logger.info("Skipping V08__BackfillOgCards script in local environment.")
            return
        }

        logger.info("Starting V08__BackfillOgCards script...")

        val workspaces = workspaceRepository.findAll()
        var processed = 0
        var skipped = 0
        var failed = 0

        for (workspace in workspaces) {
            try {
                if (workspace.ogImageUrl != null) {
                    skipped++
                    continue
                }
                val primaryPhotoUrl = workspace.images.minByOrNull { it.id }?.url
                if (primaryPhotoUrl == null) {
                    skipped++
                    continue
                }
                val newUrl = ogCardGenerator.generate(workspace.id, primaryPhotoUrl)
                persistOgUrl(workspace.id, newUrl)
                processed++
                Thread.sleep(50L) // gentle pacing to avoid hammering S3 / generator
            } catch (e: Exception) {
                logger.error("Failed to backfill og card for workspaceId=${workspace.id}", e)
                failed++
            }
        }

        logger.info(
            "V08__BackfillOgCards complete: processed={}, skipped={}, failed={}",
            processed,
            skipped,
            failed,
        )
    }

    @Transactional
    fun persistOgUrl(workspaceId: Long, ogImageUrl: String) {
        val ws = workspaceRepository.findById(workspaceId).orElse(null) ?: return
        ws.ogImageUrl = ogImageUrl
        workspaceRepository.save(ws)
    }
}
```

설계 결정 추가:
- 트랜잭션 분리: 메인 `run()`은 `NOT_SUPPORTED`로 트랜잭션 없음 → 한 번에 모든 워크스페이스를 메모리에 로드하지만 `findAll()`은 lazy 컬렉션을 안 건드리니 OK. **단**, `workspace.images.minByOrNull`은 lazy fetch라 트랜잭션 없이는 `LazyInitializationException`.
- → 따라서 워크스페이스별로 짧은 트랜잭션을 명시적으로 감싸는 패턴이 더 안전. 다음 보강:

다음과 같이 `processOne` 헬퍼를 트랜잭션 내에서 돌리도록 수정:

```kotlin
package com.kioschool.kioschoolapi.global.schedule.script

import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.domain.workspace.service.OgCardGenerator
import com.kioschool.kioschoolapi.global.schedule.Runnable
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class V08__BackfillOgCards(
    private val workspaceRepository: WorkspaceRepository,
    private val ogCardGenerator: OgCardGenerator,
    private val environment: Environment,
) : Runnable {
    private val logger = LoggerFactory.getLogger(V08__BackfillOgCards::class.java)

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun run() {
        if (environment.activeProfiles.any { it == "local" || it == "default" } || environment.activeProfiles.isEmpty()) {
            logger.info("Skipping V08__BackfillOgCards script in local environment.")
            return
        }
        logger.info("Starting V08__BackfillOgCards script...")

        val workspaceIds = workspaceRepository.findAll().map { it.id }
        var processed = 0
        var skipped = 0
        var failed = 0

        for (workspaceId in workspaceIds) {
            try {
                when (processOne(workspaceId)) {
                    Result.PROCESSED -> processed++
                    Result.SKIPPED -> skipped++
                }
                Thread.sleep(50L)
            } catch (e: Exception) {
                logger.error("Failed to backfill og card for workspaceId=$workspaceId", e)
                failed++
            }
        }

        logger.info(
            "V08__BackfillOgCards complete: processed={}, skipped={}, failed={}",
            processed,
            skipped,
            failed,
        )
    }

    @Transactional
    fun processOne(workspaceId: Long): Result {
        val ws = workspaceRepository.findById(workspaceId).orElse(null) ?: return Result.SKIPPED
        if (ws.ogImageUrl != null) return Result.SKIPPED
        val primaryPhotoUrl = ws.images.minByOrNull { it.id }?.url ?: return Result.SKIPPED
        val newUrl = ogCardGenerator.generate(ws.id, primaryPhotoUrl)
        ws.ogImageUrl = newUrl
        workspaceRepository.save(ws)
        return Result.PROCESSED
    }

    enum class Result { PROCESSED, SKIPPED }
}
```

`processOne`이 `@Transactional`로 감싸져 있어 `images.minByOrNull` lazy fetch가 안전. self-invocation은 Spring AOP 프록시를 우회하므로, 이 패턴이 동작하는지 확인 필요. **검증 사항**: V07에서도 동일한 self-invocation을 쓰는지, 또는 별도 빈으로 분리하는지 확인. 만약 self-invocation이 작동 안 하면 별도 `@Service` 빈으로 분리하거나 `applicationContext.getBean(this::class.java).processOne(...)`로 우회.

V07이 같은 클래스 내에서 `@Transactional` 메서드를 호출하지 않고 단일 `run()`만 가지면 self-invocation 이슈 회피됨. **권장 안**: `processOne`을 별도 `@Component` 헬퍼 빈으로 빼서 self-invocation을 피한다. 다음과 같이 보강:

별도 빈 `OgBackfillStep` 추가 (`V08__BackfillOgCards.kt`와 같은 파일에 둠):

```kotlin
@Component
class OgBackfillStep(
    private val workspaceRepository: WorkspaceRepository,
    private val ogCardGenerator: OgCardGenerator,
) {
    @Transactional
    fun processOne(workspaceId: Long): V08__BackfillOgCards.Result {
        val ws = workspaceRepository.findById(workspaceId).orElse(null)
            ?: return V08__BackfillOgCards.Result.SKIPPED
        if (ws.ogImageUrl != null) return V08__BackfillOgCards.Result.SKIPPED
        val primaryPhotoUrl = ws.images.minByOrNull { it.id }?.url
            ?: return V08__BackfillOgCards.Result.SKIPPED
        val newUrl = ogCardGenerator.generate(ws.id, primaryPhotoUrl)
        ws.ogImageUrl = newUrl
        workspaceRepository.save(ws)
        return V08__BackfillOgCards.Result.PROCESSED
    }
}
```

`V08__BackfillOgCards`는 `OgBackfillStep`을 주입받아 호출:

```kotlin
@Component
class V08__BackfillOgCards(
    private val workspaceRepository: WorkspaceRepository,
    private val backfillStep: OgBackfillStep,
    private val environment: Environment,
) : Runnable {
    private val logger = LoggerFactory.getLogger(V08__BackfillOgCards::class.java)
    enum class Result { PROCESSED, SKIPPED }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun run() {
        if (environment.activeProfiles.any { it == "local" || it == "default" } || environment.activeProfiles.isEmpty()) {
            logger.info("Skipping V08__BackfillOgCards script in local environment.")
            return
        }
        logger.info("Starting V08__BackfillOgCards script...")

        val workspaceIds = workspaceRepository.findAll().map { it.id }
        var processed = 0
        var skipped = 0
        var failed = 0

        for (workspaceId in workspaceIds) {
            try {
                when (backfillStep.processOne(workspaceId)) {
                    Result.PROCESSED -> processed++
                    Result.SKIPPED -> skipped++
                }
                Thread.sleep(50L)
            } catch (e: Exception) {
                logger.error("Failed to backfill og card for workspaceId=$workspaceId", e)
                failed++
            }
        }
        logger.info(
            "V08__BackfillOgCards complete: processed={}, skipped={}, failed={}",
            processed,
            skipped,
            failed,
        )
    }
}
```

**최종 결정**: 위의 두 클래스(`V08__BackfillOgCards` + `OgBackfillStep`)를 같은 파일에 둔다. V07이 self-invocation 패턴을 안 쓰는 것과 일관성. `OgBackfillStep`은 V08 전용이라 다른 위치에 둘 필요 없음.

- [ ] **Step 2: 컴파일 확인**

Run:
```bash
./gradlew compileKotlin 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: 가벼운 단위 테스트 추가 (선택)**

V07이 테스트 없이 가는 패턴이라 본 task에서도 테스트는 필수 아님. 다만 `processOne`은 분기가 명확하니 짧게 추가하면 좋다.

`src/test/kotlin/com/kioschool/kioschoolapi/og/script/V08BackfillOgCardsTest.kt`:

```kotlin
package com.kioschool.kioschoolapi.og.script

import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.domain.workspace.service.OgCardGenerator
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.schedule.script.OgBackfillStep
import com.kioschool.kioschoolapi.global.schedule.script.V08__BackfillOgCards.Result
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

class V08BackfillOgCardsTest : DescribeSpec({
    val workspaceRepository = mockk<WorkspaceRepository>()
    val ogCardGenerator = mockk<OgCardGenerator>()
    val sut = OgBackfillStep(workspaceRepository, ogCardGenerator)

    describe("processOne") {
        it("skips when ogImageUrl already populated") {
            val ws = SampleEntity.workspace.apply { ogImageUrl = "existing" }
            every { workspaceRepository.findById(ws.id) } returns Optional.of(ws)

            val result = sut.processOne(ws.id)

            assert(result == Result.SKIPPED)
            verify(exactly = 0) { ogCardGenerator.generate(any(), any()) }
        }

        it("skips when workspace has no images") {
            val ws = SampleEntity.workspace.apply {
                ogImageUrl = null
                images.clear()
            }
            every { workspaceRepository.findById(ws.id) } returns Optional.of(ws)

            assert(sut.processOne(ws.id) == Result.SKIPPED)
        }
    }
})
```

(SampleEntity 의존이 어려우면 본 테스트는 생략. 본 plan에서는 선택 사항으로 둠.)

- [ ] **Step 4: 커밋**

```bash
git add src/main/kotlin/com/kioschool/kioschoolapi/global/schedule/script/V08__BackfillOgCards.kt
# 테스트를 추가했다면:
# git add src/test/kotlin/com/kioschool/kioschoolapi/og/script/V08BackfillOgCardsTest.kt
git commit -m "feat(og): add V08 backfill script for existing workspaces"
```

---

## 배포 후 수동 검증 (스펙 §13.2)

본 plan은 코드 변경까지만 포함. 배포 후 검증은 별도 운영 작업. 참고용으로 절차 명시.

### A. 스테이징 배포 후

```bash
# Spring 직접 호출 — workspaceId 정상
curl -i 'https://api-dev.kio-school.com/og/order?workspaceId=42' | head -40

# Spring 직접 호출 — fallback 동작
curl -i 'https://api-dev.kio-school.com/og/order' | head -40
curl -i 'https://api-dev.kio-school.com/og/order?workspaceId=999999' | head -40
```

응답 본문에 `<meta property="og:image" content="...">`가 박혀있고, fallback이 `https://kio-school.com/preview.png`로 가는지 확인.

### B. Amplify 룰 적용 후 (인프라 작업 — 본 리포 외)

```bash
# 일반 사용자 — SPA index.html이 와야 함
curl -i 'https://kio-school.com/order?workspaceId=42' | head -20

# KAKAOTALK 위장 — Spring 미니 HTML이 와야 함
curl -i -H 'User-Agent: Mozilla/5.0 (compatible; KAKAOTALK 9.0.0)' \
     'https://kio-school.com/order?workspaceId=42' | head -40
```

### C. 카카오/페북 디버거

- 카카오: https://developers.kakao.com/tool/clear/og
- 페북: https://developers.facebook.com/tools/debug/

워크스페이스 사진 + 우하단 키오스쿨 배지 카드가 정상 렌더되는지 확인.

---

## 테스트 일괄 실행 (각 PR 마무리 시)

```bash
./gradlew test 2>&1 | tail -30
```

Expected: 모든 신규 테스트 + 기존 회귀 PASS.

---

## Self-Review

**1. Spec coverage**

- 스펙 §6.1 키 규칙: `$workspacePath/workspace{id}/og/{hash}.png` → Task 5 `OgCardGenerator.pathFor`에서 구현 ✓
- 스펙 §6.2 대표 사진 규칙 `images.minByOrNull { it.id }` → Task 6 listener, Task 9 백필에서 구현 ✓
- 스펙 §6.3 청소(lifecycle 90일) → S3 lifecycle은 인프라 작업이라 코드 변경 없음. plan에서 명시적으로 인지 ✓
- 스펙 §7 마이그레이션 + 엔티티 → Task 1, 2 ✓
- 스펙 §8.1 신규 파일 6개 → Task 1, 4, 5, 6, 8, 9에서 모두 커버 ✓
- 스펙 §8.2 수정 파일 → Workspace.kt(Task 2), S3Service.kt(Task 3), application.yml(Task 7) ✓. SecurityConfig는 불필요로 결정. ✓
- 스펙 §8.3 OgController → Task 8 ✓
- 스펙 §8.4 OgCardGenerator → Task 5 ✓
- 스펙 §8.5 WorkspaceOgImageListener → Task 6 ✓
- 스펙 §8.6 백필 → Task 9 ✓
- 스펙 §9 Amplify 리라이트 → 인프라 작업 (본 리포 스코프 외, plan 도입부에서 명시) ✓
- 스펙 §10 Security → 본 코드베이스에선 불필요로 결정 ✓
- 스펙 §11 사진 변경/삭제 시나리오 → Task 6 listener의 분기로 자연스럽게 처리 (사진 0장 → null, 합성 실패 → 보존) ✓
- 스펙 §13 테스트 → Task 5/6/8에 단위 테스트 포함 ✓
- 스펙 §14 관측 → logger.info/error로 처리, Sentry는 도입 시 마이그레이션 ✓
- 스펙 §15 롤아웃 PR 분할 → 본 plan 도입부에서 4개 PR 분할 명시 ✓
- 스펙의 SSRF/MIME 화이트리스트(§8.4 방어 사항) → 현재 plan은 빠져 있음. 본 단계 입력은 우리 워크스페이스 사진 URL이라 위험 낮음. **후속 보안 강화 티켓**으로 분리.

**2. Placeholder scan** — 본 plan에 "TODO/TBD/적절한 에러 처리 추가/Task N과 유사" 같은 표현 없음 ✓. 모든 step에 실제 코드 또는 구체 명령. og-badge.png는 placeholder PNG 생성 명령까지 포함하여 구체화. ✓

**3. Type consistency**

- `OgCardGenerator.generate(workspaceId: Long, sourcePhotoUrl: String): String` — Task 5 정의, Task 6 listener에서 동일 시그니처로 호출 ✓
- `OgCardGenerator.expectedUrl(workspaceId: Long, sourcePhotoUrl: String): String` — Task 5 정의, Task 6 listener에서 동일 시그니처로 호출 ✓
- `S3Service.uploadBytes(bytes: ByteArray, path: String, contentType: String): String` — Task 3 정의, Task 5에서 동일 시그니처로 호출 ✓
- `S3Service.urlFor(path: String): String` — Task 3 정의, Task 5 `expectedUrl`에서 호출 ✓
- `WorkspaceUpdatedEvent(val workspaceId: Long)` — 기존 코드, Task 6 listener에서 동일 시그니처로 사용 ✓
- `Workspace.ogImageUrl: String?` — Task 2 정의, Task 6/8/9에서 동일 타입으로 read/write ✓
- `V08__BackfillOgCards.Result` enum (PROCESSED/SKIPPED) — Task 9에서 정의, 테스트에서 동일 enum 참조 ✓
- `OgBackfillStep.processOne(workspaceId: Long): V08__BackfillOgCards.Result` — Task 9에서 정의, V08 본체에서 동일 시그니처로 호출 ✓

모든 시그니처 일관 ✓.

**4. 위험/주의 사항 메모**

- `og-badge.png` placeholder 1×1 투명 PNG로는 합성 결과가 사진만 노출 — 디자이너 산출물 도착 전엔 카카오 디버거에서 시각 검증 의미 없음. **PR3 머지 전에 디자이너 산출물 도착 필수**.
- scrimage `PngWriter()`가 4.3.0에서 정상 작동하지 않으면 `WebpWriter.DEFAULT`로 변경 가능 — 다만 og:image는 PNG/JPG가 가장 호환성 좋음 (카카오는 webp 지원 의문). PNG 유지가 안전.
- Task 9 백필이 `Thread.sleep(50L)` 동기 sleep — `OneTimeScheduler`가 `@Scheduled` 별도 스레드에서 도는 것이므로 부트 자체를 막지 않음. 워크스페이스 N개 × 50ms = 가벼운 부하.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-04-og-image-implementation.md`. Two execution options:

**1. Subagent-Driven (recommended)** — fresh subagent per task, review between tasks, fast iteration  
**2. Inline Execution** — execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
