/*
 * Copyright 2015-2026 Den Haag, Ritense, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.nlportal.architecture

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.type.filter.TypeFilter
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/** Discovers backend entry points by classpath scanning; no Spring context is started. */
object EntryPointScanner {
    private const val BASE_PACKAGE = "nl.nlportal"

    fun scan(): Set<EntryPoint> = scanClasses().flatMap { entryPointsOf(it) }.toSet()

    private fun scanClasses(): List<Class<*>> {
        val provider =
            object : ClassPathScanningCandidateComponentProvider(false) {
                override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition) = true
            }
        provider.addIncludeFilter(TypeFilter { _, _ -> true })
        return provider
            .findCandidateComponents(BASE_PACKAGE)
            .mapNotNull { it.beanClassName }
            .mapNotNull { loadOrNull(it) }
            // Abstract types are covered through their concrete subclasses, which inherit the mappings.
            .filterNot { it.isInterface || Modifier.isAbstract(it.modifiers) || it.isAnonymousClass }
    }

    // Classes with optional dependencies missing from the classpath cannot contain active entry points.
    private fun loadOrNull(name: String): Class<*>? =
        try {
            Class.forName(name, false, javaClass.classLoader).also { it.declaredMethods }
        } catch (_: LinkageError) {
            null
        }

    private fun methodsOf(clazz: Class<*>): List<Method> =
        generateSequence(clazz) { it.superclass }
            .takeWhile { it != Any::class.java }
            .flatMap { it.declaredMethods.asSequence() }
            .filterNot { it.isSynthetic || it.isBridge }
            .toList()

    private fun entryPointsOf(clazz: Class<*>): List<EntryPoint> {
        val methods =
            try {
                methodsOf(clazz)
            } catch (_: LinkageError) {
                return emptyList()
            }
        val isListener =
            ApplicationListener::class.java.isAssignableFrom(clazz) ||
                methods.any { AnnotatedElementUtils.hasAnnotation(it, EventListener::class.java) }
        return methods.flatMap { graphQlOperations(it) + restEndpoints(clazz, it) } +
            listOfNotNull(EntryPoint(EntryPointType.EVENT_LISTENER, clazz.simpleName).takeIf { isListener })
    }

    private fun graphQlOperations(method: Method): List<EntryPoint> {
        val query = AnnotatedElementUtils.findMergedAnnotation(method, QueryMapping::class.java)
        val mutation = AnnotatedElementUtils.findMergedAnnotation(method, MutationMapping::class.java)
        return listOfNotNull(
            query?.let { EntryPoint(EntryPointType.GRAPHQL_QUERY, it.name.ifEmpty { method.name }) },
            mutation?.let { EntryPoint(EntryPointType.GRAPHQL_MUTATION, it.name.ifEmpty { method.name }) },
        )
    }

    private fun restEndpoints(
        clazz: Class<*>,
        method: Method,
    ): List<EntryPoint> {
        val mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping::class.java) ?: return emptyList()
        val prefixes =
            AnnotatedElementUtils
                .findMergedAnnotation(clazz, RequestMapping::class.java)
                ?.path
                .orEmpty()
                .toList()
                .ifEmpty { listOf("") }
        val suffixes = mapping.path.toList().ifEmpty { listOf("") }
        val verbs = mapping.method.map { it.name }.ifEmpty { listOf("ANY") }
        return verbs.flatMap { verb ->
            prefixes.flatMap { prefix -> suffixes.map { EntryPoint(EntryPointType.REST, "$verb ${joinPath(prefix, it)}") } }
        }
    }

    private fun joinPath(
        prefix: String,
        suffix: String,
    ): String {
        val joined = ("/" + prefix.trim('/') + "/" + suffix.trim('/')).replace(Regex("/{2,}"), "/")
        return if (joined.length > 1) joined.trimEnd('/') else joined
    }
}