package com.kioschool.kioschoolapi.architecture

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.stereotype.Service

private const val BASE_PACKAGE = "com.kioschool.kioschoolapi"

// 규칙을 들이기 전부터 있던 의존성. 고치면 목록에서 빼고, 새 항목은 넣지 않는다.
private val LEGACY_SERVICE_DEPENDENCIES = setOf(
    "DailyInsightCardGenerationService -> DiscordService",
    "DailyInsightCardGenerationService -> InsightScoringService",
    "InquiryPurgeService -> S3Service",
    "InquiryService -> EmailService",
    "InquiryService -> S3Service",
    "InquiryService -> UserService",
    "MailAudienceService -> EmailService",
    "OrderService -> CustomWebSocketService",
    "ProductService -> S3Service",
    "UserService -> EmailService",
    "WorkspaceService -> S3Service",
    "WorkspaceService -> UserService",
)

private fun Class<*>.isServiceLayer() = isAnnotationPresent(Service::class.java) && simpleName.endsWith("Service")

private fun scanServiceClasses(): List<Class<*>> {
    val scanner = ClassPathScanningCandidateComponentProvider(false)
    scanner.addIncludeFilter(AnnotationTypeFilter(Service::class.java))
    return scanner.findCandidateComponents(BASE_PACKAGE)
        .map { Class.forName(it.beanClassName) }
        .filter { it.isServiceLayer() }
}

private fun serviceToServiceDependencies(): Set<String> =
    scanServiceClasses().flatMap { service ->
        service.declaredConstructors
            .flatMap { it.parameterTypes.toList() }
            .filter { it != service && it.isServiceLayer() }
            .map { "${service.simpleName} -> ${it.simpleName}" }
    }.toSet()

class ServiceLayerDependencyTest : DescribeSpec({
    describe("Service 계층 의존성") {
        it("@Service가 붙은 *Service 클래스를 찾는다") {
            scanServiceClasses().map { it.simpleName } shouldContain "OrderService"
        }

        it("Service는 다른 Service를 생성자로 받지 않는다") {
            val newDependencies = serviceToServiceDependencies() - LEGACY_SERVICE_DEPENDENCIES

            withClue(
                "Service끼리 직접 부르지 않는다. 두 Service를 함께 쓰는 로직은 Facade로 옮긴다 " +
                    "(.agent/agent.md 코딩 컨벤션 1번). LEGACY_SERVICE_DEPENDENCIES에 새 항목을 넣어 우회하지 않는다.",
            ) {
                newDependencies shouldBe emptySet()
            }
        }

        it("기존 위반 목록에는 아직 남아 있는 의존성만 둔다") {
            val resolved = LEGACY_SERVICE_DEPENDENCIES - serviceToServiceDependencies()

            withClue("고쳐진 의존성은 LEGACY_SERVICE_DEPENDENCIES에서 뺀다.") {
                resolved shouldBe emptySet()
            }
        }
    }
})
