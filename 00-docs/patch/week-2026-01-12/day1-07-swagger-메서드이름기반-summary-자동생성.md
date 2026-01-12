# 패치노트 - 2026-01-12

## Swagger 메서드 이름 기반 Summary 자동 생성

### 목표

Swagger 문서에서 API 엔드포인트의 summary를 자동으로 생성하여, 개발자가 별도의 어노테이션 없이도 기본적인 API 설명을 제공할 수 있도록 합니다.

### 구현 내용

**OpenApiConfig:**
- `methodNameBasedSummaryCustomizer()` Bean 추가
  - `OperationCustomizer`를 구현하여 Swagger Operation의 summary가 비어있을 때 자동 생성
  - 메서드 이름을 기반으로 사람이 읽기 쉬운 형태로 변환
    - 예: `getMyChatRooms` → "Get my chat rooms"
    - 예: `create` → "Create"

**동작 방식:**
1. Swagger 문서 생성 시 각 API 엔드포인트의 summary 확인
2. summary가 비어있으면 메서드 이름을 기반으로 자동 생성
3. CamelCase를 공백으로 구분된 문장으로 변환
4. 첫 글자를 대문자로 변환

### 기술적 고려사항

**초기 시도:**
- `therapi-runtime-javadoc`을 사용하여 KDoc 주석을 읽으려고 시도
- Kotlin의 KDoc은 JavaDoc으로 변환되지 않아 `therapi-runtime-javadoc-scribe` annotation processor가 처리하지 못함
- Dokka 플러그인 추가 시도했으나, 런타임에 KDoc을 읽는 것은 복잡함

**최종 결정:**
- 비침습적 접근을 위해 메서드 이름 기반 summary 생성으로 결정
- 추후 KDoc을 읽는 것을 목표로 한다는 주석 추가

### 코드 예시

```kotlin
/**
 * 메서드 이름 기반으로 Swagger Operation의 summary를 자동 생성하는 Customizer
 * 
 * 현재는 메서드 이름을 기반으로 summary를 생성하지만,
 * 추후 KDoc 주석의 내용을 읽어서 반영하는 것을 목표로 한다.
 */
@Bean
fun methodNameBasedSummaryCustomizer(): OperationCustomizer {
    return OperationCustomizer { operation, handlerMethod ->
        val handlerMethodObj = handlerMethod as? HandlerMethod
        if (handlerMethodObj != null) {
            val method = handlerMethodObj.method
            
            // summary가 비어있으면 메서드 이름 기반으로 생성
            if (operation.summary.isNullOrBlank()) {
                val methodName = method.name
                val summary = methodName
                    .replace(Regex("([A-Z])"), " $1")
                    .trim()
                    .lowercase()
                    .replaceFirstChar { it.uppercase() }
                operation.summary = summary
            }
        }
        operation
    }
}
```

### 효과

**Before:**
- Swagger UI에서 API 엔드포인트의 summary가 비어있음
- 개발자가 `@Operation` 어노테이션을 추가해야 함

**After:**
- 메서드 이름 기반으로 자동으로 summary 생성
- 비침습적 접근으로 기존 코드 수정 불필요
- 향후 KDoc을 읽을 수 있게 되면 자연스럽게 확장 가능

### 향후 계획

- KDoc 주석을 런타임에 읽어서 summary와 description에 반영
- 더 정확한 API 문서 자동 생성
