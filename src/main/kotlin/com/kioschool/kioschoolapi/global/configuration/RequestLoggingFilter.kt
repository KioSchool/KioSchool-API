import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*

@Component
class RequestLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // 요청 정보를 담을 문자열 빌더 생성
        val requestDetails = StringBuilder()
        requestDetails.append("\n--- INCOMING REQUEST ---\n")
        requestDetails.append("URI    : ${request.requestURI}\n")
        requestDetails.append("Method : ${request.method}\n")
        requestDetails.append("Headers:\n")

        // 모든 헤더 정보를 순회하며 기록
        val headerNames = request.headerNames ?: Collections.emptyEnumeration()
        headerNames.asSequence().forEach { headerName ->
            requestDetails.append("  $headerName : ${request.getHeader(headerName)}\n")
        }
        requestDetails.append("----------------------\n")

        // 완성된 요청 정보를 로그로 출력
        log.info(requestDetails.toString())

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response)
    }
}