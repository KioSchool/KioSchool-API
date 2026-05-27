package com.kioschool.kioschoolapi.global.schedule.script

import com.kioschool.kioschoolapi.domain.account.repository.BankRepository
import com.kioschool.kioschoolapi.global.schedule.Runnable
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class V10__BackfillBankTossName(
    private val bankRepository: BankRepository
) : Runnable {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        /**
         * 실제 계좌 데이터의 toss URL에서 bank 파라미터를 디코딩해 검증된 매핑.
         * 목록에 없는 은행은 tossName을 채우지 않고 null로 유지한다.
         */
        private val VERIFIED_TOSS_NAME_MAP = mapOf(
            "카카오뱅크" to "카카오뱅크",
            "KB국민은행" to "KB국민은행",
            "신한은행" to "신한은행",
            "우리은행" to "우리은행",
            "토스뱅크" to "토스뱅크",
            // name과 tossName이 다른 은행
            "농협" to "NH농협은행",
            "기업은행" to "IBK기업은행",
            "하나은행(서울은행)" to "하나은행",
            "K뱅크" to "케이뱅크"
        )
    }

    override fun run() {
        logger.info("Starting BackfillBankTossName script...")

        val banks = bankRepository.findAll()
        var updated = 0
        var skipped = 0

        banks.forEach { bank ->
            val tossName = VERIFIED_TOSS_NAME_MAP[bank.name]
            if (tossName != null) {
                bank.tossName = tossName
                updated++
            } else {
                logger.warn("BackfillBankTossName: '${bank.name}' is not in verified map. tossName left as null.")
                skipped++
            }
        }

        bankRepository.saveAll(banks)
        logger.info("Completed BackfillBankTossName script. Updated=$updated, Skipped(unverified)=$skipped")
    }
}
