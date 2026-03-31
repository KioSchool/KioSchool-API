package com.kioschool.kioschoolapi.global.aspect

import com.kioschool.kioschoolapi.domain.workspace.exception.SuperAdminWorkspaceReadOnlyException
import com.kioschool.kioschoolapi.domain.user.facade.UserFacade
import com.kioschool.kioschoolapi.domain.workspace.facade.WorkspaceFacade
import com.kioschool.kioschoolapi.global.common.enums.UserRole
import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware
import com.kioschool.kioschoolapi.global.security.annotation.AdminUsername
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component

@Aspect
@Component
class WorkspaceWriteAccessAspect(
    private val userFacade: UserFacade,
    private val workspaceFacade: WorkspaceFacade
) {

    // Admin 이름이 들어간 컨트롤러들의 CUD(쓰기) 요청만 가로챔
    @Before(
        "execution(* com.kioschool.kioschoolapi..controller.Admin*Controller.*(..)) && " +
        "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
        "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
        "@annotation(org.springframework.web.bind.annotation.PatchMapping) || " +
        "@annotation(org.springframework.web.bind.annotation.DeleteMapping))"
    )
    fun checkSuperAdminWriteAccess(joinPoint: JoinPoint) {
        val signature = joinPoint.signature as MethodSignature
        val args = joinPoint.args
        val parameters = signature.method.parameters

        var username: String? = null
        var workspaceId: Long? = null

        // 1. 파라미터에서 정보 추출
        for (i in args.indices) {
            val arg = args[i] ?: continue
            val param = parameters[i]

            // 컨트롤러 메서드의 @AdminUsername 파라미터에서 유저 ID 추출
            if (param.isAnnotationPresent(AdminUsername::class.java)) {
                username = arg as? String
            }
            
            // workspaceId 추출 (Path Variable, RequestParam 혹은 @RequestBody DTO 타입)
            if (param.name == "workspaceId" && arg is Number) {
                workspaceId = arg.toLong()
            } else if (arg is WorkspaceAware) {
                workspaceId = arg.workspaceId
            }
        }

        // 2. 권한 검증 로직 (Facade 사용으로 캐시 활용)
        if (username != null && workspaceId != null) {
            val userDto = userFacade.getUser(username)

            // 타겟이 Super Admin일 경우에만 체크를 통과시킴
            if (userDto.role == UserRole.SUPER_ADMIN) {
                val workspaceDto = workspaceFacade.getWorkspace(workspaceId)
                
                // Super Admin 본인이 소유(생성)한 워크스페이스가 아닌 경우 예외를 발생 (Read-Only 처리)
                if (workspaceDto.owner.id != userDto.id) {
                    throw SuperAdminWorkspaceReadOnlyException()
                }
            }
        }
    }
}
