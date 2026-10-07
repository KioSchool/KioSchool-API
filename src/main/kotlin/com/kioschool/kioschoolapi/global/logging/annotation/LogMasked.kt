package com.kioschool.kioschoolapi.global.logging.annotation

/**
 * 로그에서만 가리는 속성. HTTP 응답에는 그대로 나간다(응답까지 가리는 것은 [Masked]).
 *
 * 속성 이름만으로는 개인정보인지 알 수 없을 때 붙인다. 예: 운영진 실명 `name` — 같은 이름을 상품·주점도 쓴다.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogMasked
