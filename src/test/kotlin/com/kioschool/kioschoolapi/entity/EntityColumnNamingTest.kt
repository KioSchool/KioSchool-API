package com.kioschool.kioschoolapi.entity

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToMany
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.OneToMany
import jakarta.persistence.Transient
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

/**
 * Hibernate's CamelCaseToUnderscoresNamingStrategy inserts an underscore before an uppercase
 * letter only when that letter is both preceded AND followed by a lowercase letter:
 *
 *   for (int i = 1; i < builder.length() - 1; i++) {
 *       if (isUnderscoreRequired(builder.charAt(i - 1), builder.charAt(i), builder.charAt(i + 1))) { ... }
 *   }
 *   isUnderscoreRequired(before, current, after) =
 *       Character.isLowerCase(before) && Character.isUpperCase(current) && Character.isLowerCase(after)
 *
 * The loop never examines the last character as "current" (i stops at length - 2), and it
 * requires a lowercase character *after* the uppercase one. So an uppercase letter at the very
 * end of a property name (positionX), or one immediately followed by a non-lowercase character
 * (positionX2), never gets its underscore -- "positionX" implicitly maps to "positionx", not
 * "position_x". That silently broke every WorkspaceTable query once the changelog created a
 * position_x column (see commit dce73a8).
 *
 * This test asserts no entity can carry a property name in that blind spot without an explicit
 * @Column(name = ...) (or @JoinColumn(name = ...) for the owning side of a relation) pinning the
 * real column name. Without it, deleting position_x/position_y's @Column annotations passes every
 * other test in the suite -- every repository is mocked and there is no Spring-context or database
 * test -- and only fails in production.
 */

private const val BASE_PACKAGE = "com.kioschool.kioschoolapi"

/**
 * True when Hibernate's default strategy would fail to underscore-separate this name, i.e. it
 * contains an uppercase letter that is either the last character or not followed by a lowercase
 * letter. This is a simpler, equivalent restatement of the blind spot above: we don't need to
 * reimplement the sliding window, just find any uppercase letter the window can never fire on.
 */
private fun isColumnNameAtRisk(name: String): Boolean {
    for (i in name.indices) {
        if (name[i].isUpperCase()) {
            val next = name.getOrNull(i + 1)
            if (next == null || !next.isLowerCase()) return true
        }
    }
    return false
}

/**
 * Faithful re-implementation of Hibernate's CamelCaseToUnderscoresNamingStrategy#toSnakeCase, used
 * only to make failure messages concrete ("Hibernate would map this to X") rather than to decide
 * risk (isColumnNameAtRisk above is the simpler, equivalent check actually used for that).
 */
private fun hibernateImplicitColumnName(identifier: String): String {
    val buf = StringBuilder(identifier)
    var i = 1
    while (i < buf.length - 1) {
        val before = buf[i - 1]
        val current = buf[i]
        val after = buf[i + 1]
        if (before.isLowerCase() && current.isUpperCase() && after.isLowerCase()) {
            buf.insert(i, '_')
            i++
        }
        i++
    }
    return buf.toString().lowercase()
}

/**
 * Scans the compiled classpath (no Spring context, no database) for every @Entity and
 * @MappedSuperclass class in the project. A hardcoded list was considered instead, but it would
 * silently go stale the moment someone adds a new entity and forgets to update it -- exactly the
 * kind of "looks safe, isn't" gap this test exists to close. Spring's own bytecode-based scanner
 * (already on the test classpath via spring-boot-starter-test) finds them without needing an
 * ApplicationContext, so scanning costs nothing extra and never needs maintenance.
 */
private fun scanPersistenceClasses(): List<Class<*>> {
    val scanner = ClassPathScanningCandidateComponentProvider(false)
    scanner.addIncludeFilter(AnnotationTypeFilter(Entity::class.java))
    scanner.addIncludeFilter(AnnotationTypeFilter(MappedSuperclass::class.java))
    return scanner.findCandidateComponents(BASE_PACKAGE)
        .map { Class.forName(it.beanClassName) }
}

class EntityColumnNamingTest : DescribeSpec({
    describe("reading @Column off the Java backing field") {
        it("sees the explicit column names already pinned on WorkspaceTable.positionX/positionY (sanity check)") {
            val workspaceTable =
                Class.forName("com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceTable").kotlin

            val positionX = workspaceTable.declaredMemberProperties.first { it.name == "positionX" }
            val positionY = workspaceTable.declaredMemberProperties.first { it.name == "positionY" }

            val xColumn = positionX.javaField?.getAnnotation(Column::class.java)
            val yColumn = positionY.javaField?.getAnnotation(Column::class.java)

            xColumn.shouldNotBeNull()
            yColumn.shouldNotBeNull()
            xColumn.name shouldBe "position_x"
            yColumn.name shouldBe "position_y"
        }
    }

    describe("entity column naming") {
        it("gives every mangle-prone property an explicit column name") {
            val violations = mutableListOf<String>()

            scanPersistenceClasses().forEach classLoop@{ clazz ->
                clazz.kotlin.declaredMemberProperties.forEach { property ->
                    // No backing field: a computed property or delegate, not a persistent column.
                    val field = property.javaField ?: return@forEach

                    if (field.isAnnotationPresent(Transient::class.java)) return@forEach
                    // Collection sides of a relationship own no column of their own.
                    if (field.isAnnotationPresent(OneToMany::class.java)) return@forEach
                    if (field.isAnnotationPresent(ManyToMany::class.java)) return@forEach

                    if (!isColumnNameAtRisk(property.name)) return@forEach

                    val explicitColumnName = field.getAnnotation(Column::class.java)
                        ?.name?.takeIf { it.isNotBlank() }
                    val explicitJoinColumnName = field.getAnnotation(JoinColumn::class.java)
                        ?.name?.takeIf { it.isNotBlank() }

                    if (explicitColumnName == null && explicitJoinColumnName == null) {
                        violations += "${clazz.simpleName}.${property.name} -- Hibernate's default naming " +
                            "strategy would implicitly map this to column \"${hibernateImplicitColumnName(property.name)}\", " +
                            "silently dropping the case boundary at the end of the name. " +
                            "Add @Column(name = \"...\") (or @JoinColumn(name = \"...\") if this is a relation) " +
                            "with the intended snake_case name."
                    }
                }
            }

            violations shouldBe emptyList()
        }
    }
})
