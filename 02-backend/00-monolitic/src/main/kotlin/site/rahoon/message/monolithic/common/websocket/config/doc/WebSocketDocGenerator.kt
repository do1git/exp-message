package site.rahoon.message.monolithic.common.websocket.config.doc

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.victools.jsonschema.generator.Option
import com.github.victools.jsonschema.generator.OptionPreset
import com.github.victools.jsonschema.generator.SchemaGenerator
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
import com.github.victools.jsonschema.generator.SchemaVersion
import com.github.victools.jsonschema.module.jackson.JacksonModule
import com.github.victools.jsonschema.module.jackson.JacksonOption
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import site.rahoon.message.monolithic.common.websocket.WebsocketSend
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable

@Suppress("TooManyFunctions")
@Component
class WebSocketDocGenerator(
    private val objectMapper: ObjectMapper,
) {
    // 1. 특수문자 치환 함수 (내부 식별자용)
    private fun sanitize(key: String): String =
        key
            .replace(Regex("[^a-zA-Z0-9]"), "_")
            .replace(Regex("_+"), "_")
            .removeSurrounding("_")

    fun generate(basePackage: String): String {
        val (metadataList, domainClasses) = collectMetadata(basePackage)
        val schemas = mapToSchemas(domainClasses, basePackage)
        val typeSchemas = metadataList.associate { meta ->
            meta.payloadClassName to generateSchemaForType(meta.payloadType)
        }
        val doc = buildAsyncApiDocument(metadataList, schemas, typeSchemas)
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc)
    }

    private fun collectMetadata(basePackage: String): Pair<List<StompMetadata>, MutableSet<Class<*>>> {
        val config = ConfigurationBuilder()
            .forPackages(basePackage)
            .addScanners(Scanners.MethodsAnnotated, Scanners.TypesAnnotated, Scanners.SubTypes)
        val reflections = Reflections(config)

        val allMethods = reflections.getMethodsAnnotatedWith(MessageMapping::class.java) +
            reflections.getMethodsAnnotatedWith(SendTo::class.java) +
            reflections.getMethodsAnnotatedWith(WebsocketSend::class.java)

        val targetMethods = allMethods
            .filter {
                it.declaringClass.isAnnotationPresent(Controller::class.java) ||
                    it.declaringClass.isAnnotationPresent(Component::class.java)
            }.toSet()

        val domainClasses = mutableSetOf<Class<*>>()
        val metadataList = mutableListOf<StompMetadata>()

        targetMethods.forEach { method ->
            val simplifiedClass = method.declaringClass.name.replace("$basePackage.", "")
            processMessageMapping(method, simplifiedClass, basePackage, metadataList, domainClasses)
            processSendTo(method, simplifiedClass, basePackage, metadataList, domainClasses)
            processWebsocketSend(method, simplifiedClass, basePackage, metadataList, domainClasses)
        }

        return Pair(metadataList, domainClasses)
    }

    private fun processMessageMapping(
        method: Method,
        simplifiedClass: String,
        basePackage: String,
        metadataList: MutableList<StompMetadata>,
        domainClasses: MutableSet<Class<*>>,
    ) {
        method.getAnnotation(MessageMapping::class.java)?.let { anno ->
            val type = method.genericParameterTypes.firstOrNull() ?: Any::class.java
            val rawKey = "$simplifiedClass.${method.name}.SEND"
            val pathParameters = extractPathParameters(anno.value.first())
            metadataList.add(
                StompMetadata(
                    key = sanitize(rawKey),
                    action = "SEND",
                    address = anno.value.firstOrNull() ?: "/unknown",
                    payloadType = type,
                    payloadClassName = sanitize(getTypeName(type, basePackage)),
                    method = method,
                    parameters = pathParameters,
                ),
            )
            collectDomainClasses(type, domainClasses)
        }
    }

    private fun processSendTo(
        method: Method,
        simplifiedClass: String,
        basePackage: String,
        metadataList: MutableList<StompMetadata>,
        domainClasses: MutableSet<Class<*>>,
    ) {
        val receiveAddr = method.getAnnotation(SendTo::class.java)?.value
        receiveAddr?.let { addr ->
            val type = method.genericReturnType
            if (type != Void.TYPE && type != Unit::class.java) {
                val rawKey = "$simplifiedClass.${method.name}.RECEIVE"
                val pathParameters = extractPathParameters(addr.first())
                metadataList.add(
                    StompMetadata(
                        key = sanitize(rawKey),
                        action = "RECEIVE",
                        address = addr.firstOrNull() ?: "/unknown",
                        payloadType = type,
                        payloadClassName = sanitize(getTypeName(type, basePackage)),
                        method = method,
                        parameters = pathParameters,
                    ),
                )
                collectDomainClasses(type, domainClasses)
            }
        }
    }

    private fun processWebsocketSend(
        method: Method,
        simplifiedClass: String,
        basePackage: String,
        metadataList: MutableList<StompMetadata>,
        domainClasses: MutableSet<Class<*>>,
    ) {
        val websocketSendAddr = method.getAnnotation(WebsocketSend::class.java)?.value
        websocketSendAddr?.let { addr ->
            val type = method.genericReturnType
            if (type != Void.TYPE && type != Unit::class.java) {
                val rawKey = "$simplifiedClass.${method.name}.RECEIVE"
                val pathParameters = extractPathParameters(addr)
                metadataList.add(
                    StompMetadata(
                        key = sanitize(rawKey),
                        action = "RECEIVE",
                        address = addr,
                        payloadType = type,
                        payloadClassName = sanitize(getTypeName(type, basePackage)),
                        method = method,
                        parameters = pathParameters,
                    ),
                )
                collectDomainClasses(type, domainClasses)
            }
        }
    }

    private fun extractPathParameters(address: String): List<String> =
        Regex("\\{([^}]+)\\}")
            .findAll(address)
            .map { it.groupValues[1] }
            .toList()

    private fun buildAsyncApiDocument(
        metadataList: List<StompMetadata>,
        schemas: Map<String, Any>,
        typeSchemas: Map<String, Map<String, Any>>,
    ): Map<String, Any> =
        mapOf(
            "asyncapi" to "3.0.0",
            "info" to mapOf("title" to "STOMP API Specification", "version" to "1.0.0"),
            "channels" to buildChannels(metadataList),
            "operations" to buildOperations(metadataList),
            "components" to mapOf(
                "schemas" to (schemas + typeSchemas),
                "messages" to buildMessages(metadataList),
            ),
        )

    private fun buildChannels(metadataList: List<StompMetadata>): Map<String, Any> =
        metadataList.associate { meta ->
            val channelId = sanitize(meta.address)
            val channelContent = mutableMapOf(
                "address" to meta.address,
                "messages" to mapOf(meta.payloadClassName to mapOf("\$ref" to "#/components/messages/${meta.key}")),
            )
            if (meta.parameters.isNotEmpty()) {
                channelContent["parameters"] = meta.parameters.associateWith { paramName ->
                    mapOf("description" to "$paramName parameter")
                }
            }
            channelId to channelContent
        }

    private fun buildOperations(metadataList: List<StompMetadata>): Map<String, Any> =
        metadataList.associate { meta ->
            val channelId = sanitize(meta.address)
            val messageId = meta.payloadClassName
            meta.key to mapOf(
                "action" to if (meta.action == "SEND") "receive" else "send",
                "channel" to mapOf("\$ref" to "#/channels/$channelId"),
                "messages" to listOf(
                    mapOf("\$ref" to "#/channels/$channelId/messages/$messageId"),
                ),
            )
        }

    private fun buildMessages(metadataList: List<StompMetadata>): Map<String, Any> =
        metadataList.associate { meta ->
            meta.key to mapOf("payload" to mapOf("\$ref" to "#/components/schemas/${meta.payloadClassName}"))
        }

    // --- 나머지 헬퍼 함수 (기존과 동일) ---
    private fun collectDomainClasses(
        type: Type,
        accumulator: MutableSet<Class<*>>,
    ) {
        when (type) {
            is Class<*> -> {
                if (type.isArray) {
                    collectDomainClasses(type.componentType, accumulator)
                } else if (isDtoCandidate(type) && accumulator.add(type)) {
                    type.declaredFields.forEach { collectDomainClasses(it.genericType, accumulator) }
                }
            }
            is ParameterizedType -> {
                // rawType도 수집 (Generic 클래스 자체)
                val rawType = type.rawType as? Class<*>
                rawType?.let {
                    if (isDtoCandidate(it)) {
                        accumulator.add(it)
                        it.declaredFields.forEach { field -> collectDomainClasses(field.genericType, accumulator) }
                    }
                }
                // 타입 인자들도 수집
                type.actualTypeArguments.forEach { collectDomainClasses(it, accumulator) }
            }
        }
    }

    private fun isDtoCandidate(clazz: Class<*>): Boolean {
        val name = clazz.name
        return !clazz.isPrimitive && !name.startsWith("java.") && !name.startsWith("kotlin.") && !name.endsWith("\$Companion")
    }

    private fun unwrapType(type: Type): Class<*> =
        when (type) {
            is Class<*> -> if (type.isArray) type.componentType else type
            is ParameterizedType -> unwrapType(type.actualTypeArguments.first())
            else -> Any::class.java
        }

    /**
     * 패키지 이름에서 basePackage와 표준 라이브러리 패키지 제거
     */
    private fun simplifyPackageName(
        canonicalName: String,
        basePackage: String,
    ): String {
        var simplified = canonicalName

        // basePackage 제거
        if (simplified.startsWith(basePackage)) {
            simplified = simplified.removePrefix(basePackage).removePrefix(".")
        }

        // 표준 라이브러리 패키지 제거 (java., javax., kotlin.)
        val standardPrefixes = listOf("java.", "javax.", "kotlin.")
        for (prefix in standardPrefixes) {
            if (simplified.startsWith(prefix)) {
                simplified = simplified.removePrefix(prefix)
                // 패키지 경로가 남아있으면 마지막 부분만 사용
                val parts = simplified.split(".")
                simplified = if (parts.size > 1) parts.last() else simplified
            }
        }

        return simplified
    }

    /**
     * Type을 문자열로 변환 (Generic 타입과 List 타입 포함, canonical name 사용)
     * basePackage와 표준 라이브러리 패키지는 제거됨
     * 예: List<TestSampleClass> -> "List_TestSampleClass" (basePackage가 site.rahoon.message인 경우)
     *     GenericClass<TestSampleClass> -> "GenericClass_TestSampleClass"
     */
    private fun getTypeName(
        type: Type,
        basePackage: String,
    ): String =
        when (type) {
            is Class<*> -> {
                if (type.isArray) {
                    "${getTypeName(type.componentType, basePackage)}Array"
                } else {
                    val canonicalName = type.canonicalName ?: type.name
                    simplifyPackageName(canonicalName, basePackage)
                }
            }
            is ParameterizedType -> {
                val rawType = type.rawType as? Class<*> ?: return "Unknown"
                val rawTypeCanonical = rawType.canonicalName ?: rawType.name
                val rawTypeName = simplifyPackageName(rawTypeCanonical, basePackage)
                val typeArgs = type.actualTypeArguments.joinToString("_") { getTypeName(it, basePackage) }
                "${rawTypeName}_$typeArgs"
            }
            else -> "Any"
        }

    /**
     * TypeVariable을 실제 타입 인자로 resolve
     */
    private fun resolveType(
        type: Type,
        typeVariableMap: Map<String, Type>,
    ): Type =
        when (type) {
            is TypeVariable<*> -> typeVariableMap[type.name] ?: type
            is ParameterizedType -> {
                val rawType = type.rawType
                val resolvedArgs = type.actualTypeArguments.map { resolveType(it, typeVariableMap) }
                object : ParameterizedType {
                    override fun getRawType() = rawType

                    override fun getOwnerType() = type.ownerType

                    override fun getActualTypeArguments() = resolvedArgs.toTypedArray()
                }
            }
            else -> type
        }

    /**
     * ParameterizedType에서 타입 변수 매핑 생성
     */
    private fun createTypeVariableMap(parameterizedType: ParameterizedType): Map<String, Type> {
        val rawType = parameterizedType.rawType as? Class<*> ?: return emptyMap()
        val typeParameters = rawType.typeParameters
        val actualTypeArguments = parameterizedType.actualTypeArguments

        return typeParameters
            .mapIndexedNotNull { index, typeParam ->
                if (index < actualTypeArguments.size) {
                    typeParam.name to actualTypeArguments[index]
                } else {
                    null
                }
            }.toMap()
    }

    /**
     * Type에 대한 JSON Schema를 생성 (Generic 타입과 List 타입 처리)
     */
    private fun generateSchemaForType(type: Type): Map<String, Any> = generateSchemaForType(type, createSchemaGenerator())

    /**
     * SchemaGenerator를 생성하는 헬퍼 함수
     */
    private fun createSchemaGenerator(): SchemaGenerator {
        val config = SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
            .with(JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_ORDER))
            .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
            .with(Option.INLINE_ALL_SCHEMAS)
            .build()
        return SchemaGenerator(config)
    }

    /**
     * Type에 대한 JSON Schema를 생성 (Generic 타입과 List 타입 처리)
     * @param generator 재사용할 SchemaGenerator 인스턴스
     */
    private fun generateSchemaForType(
        type: Type,
        generator: SchemaGenerator,
    ): Map<String, Any> =
        when (type) {
            is Class<*> -> {
                if (type.isArray) {
                    val itemSchema = generateSchemaForType(type.componentType, generator)
                    mapOf(
                        "type" to "array",
                        "items" to itemSchema,
                    )
                } else {
                    val schema = generator.generateSchema(type)
                    objectMapper.convertValue(schema, Map::class.java) as Map<String, Any>
                }
            }
            is ParameterizedType -> {
                val rawType = type.rawType as? Class<*>
                when {
                    rawType == List::class.java || rawType == java.util.List::class.java -> {
                        val itemType = type.actualTypeArguments.firstOrNull() ?: Any::class.java
                        val itemSchema = generateSchemaForType(itemType, generator)
                        mapOf(
                            "type" to "array",
                            "items" to itemSchema,
                        )
                    }
                    else -> {
                        // Generic 클래스인 경우 - 필드 타입을 resolve하여 스키마 생성
                        generateSchemaForGenericClass(type, rawType, generator)
                    }
                }
            }
            else -> mapOf("type" to "object")
        }

    /**
     * Generic 클래스의 스키마를 생성 (타입 인자를 필드에 적용)
     */
    private fun generateSchemaForGenericClass(
        parameterizedType: ParameterizedType,
        rawType: Class<*>?,
        generator: SchemaGenerator,
    ): Map<String, Any> {
        if (rawType == null) {
            return mapOf("type" to "object")
        }

        val typeVariableMap = createTypeVariableMap(parameterizedType)
        val baseSchema = objectMapper.convertValue(generator.generateSchema(rawType), Map::class.java) as MutableMap<String, Any>
        val properties = baseSchema["properties"] as? Map<*, *>

        if (properties != null) {
            val resolvedProperties = resolveProperties(properties, rawType, typeVariableMap)
            baseSchema["properties"] = resolvedProperties
        }

        return baseSchema
    }

    private fun resolveProperties(
        properties: Map<*, *>,
        rawType: Class<*>,
        typeVariableMap: Map<String, Type>,
    ): Map<String, Any> {
        val resolvedProperties = mutableMapOf<String, Any>()
        val fieldMap = rawType.declaredFields.associateBy { it.name }

        properties.forEach { (propertyName, originalFieldSchema) ->
            val propertyKey = propertyName.toString()
            val originalSchema = originalFieldSchema as? Map<*, *>

            if (originalSchema != null) {
                val field = findField(propertyKey, fieldMap)
                val resolvedSchema = if (field != null) {
                    resolveFieldSchema(field, typeVariableMap, originalSchema)
                } else {
                    originalSchema
                }
                resolvedProperties[propertyKey] = resolvedSchema
            } else {
                resolvedProperties[propertyKey] = originalFieldSchema!!
            }
        }

        return resolvedProperties
    }

    private fun findField(
        propertyKey: String,
        fieldMap: Map<String, java.lang.reflect.Field>,
    ): java.lang.reflect.Field? =
        fieldMap[propertyKey] ?: fieldMap.values.firstOrNull {
            it.name.equals(propertyKey, ignoreCase = true) ||
                it.name.decapitalize() == propertyKey ||
                propertyKey.equals(it.name, ignoreCase = true)
        }

    private fun resolveFieldSchema(
        field: java.lang.reflect.Field,
        typeVariableMap: Map<String, Type>,
        originalSchema: Map<*, *>,
    ): Map<String, Any> {
        val resolvedFieldType = resolveType(field.genericType, typeVariableMap)
        val resolvedSchema = generateSchemaForType(resolvedFieldType)
        return (originalSchema.toMutableMap() as MutableMap<String, Any>).apply {
            putAll(resolvedSchema)
        }
    }

    private fun mapToSchemas(
        classes: Set<Class<*>>,
        basePackage: String,
    ): Map<String, Any> {
        val config = SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
            .with(JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_ORDER))
            .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
            .with(Option.INLINE_ALL_SCHEMAS) // 모든 중첩 구조를 인라인화
            .build()
        val generator = SchemaGenerator(config)
        return classes.associate {
            // canonical name 사용하여 패키지 경로 포함 (중복 방지)
            // basePackage와 표준 라이브러리 패키지는 제거
            val canonicalName = it.canonicalName ?: it.name
            val simplifiedName = simplifyPackageName(canonicalName, basePackage)
            sanitize(simplifiedName) to objectMapper.convertValue(generator.generateSchema(it), Map::class.java)
        }
    }

    data class StompMetadata(
        val key: String,
        val action: String,
        val address: String,
        val parameters: List<String>,
        val payloadType: Type,
        val payloadClassName: String,
        val method: Method,
    )
}
