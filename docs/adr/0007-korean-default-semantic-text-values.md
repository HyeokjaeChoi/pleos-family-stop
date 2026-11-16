# 다국어는 한국어 기본 리소스 + 비 UI 코드는 의미 값 반환

한국어·영어를 지원하되 기본 리소스(`values/`)를 한국어로 두고 영어는 `values-en/`에 둔다. Android 관례(기본 = 영어)와 다르지만, 시연 에뮬레이터가 `ko-KR`이고 리소스에 없는 로케일에서도 한국어가 나와야 지금 시연 흐름이 그대로 유지되기 때문이다. 언어 전환은 시스템 언어와 앱별 언어(`localeConfig`, `cmd locale set-app-locales`)로만 하고 앱 안 언어 선택 UI는 두지 않는다.

Controller·enum·formatter 등 Compose 밖 코드는 완성된 문장 대신 의미 값(`sealed class`/enum + 인자)을 반환하고, 문장은 Compose 쪽에서 `stringResource`/`pluralStringResource`로 만든다. `@StringRes` 래퍼 반환이나 `Context` 주입도 가능했지만, `:core:model`·`:core:domain`을 Android 무의존으로 유지하고(ADR-0006) 단위 테스트가 문장이 아니라 값을 비교해 언어와 무관해지도록 이 방식을 골랐다.

## Consequences

- 문자열 리소스는 각 `:feature:<x>`의 `res/values*/`에 두고, 여러 모듈이 쓰는 단위·시각 문구만 `:core:designsystem`에 둔다.
- `:impl:*`가 만들던 사용자 표시 문구(예: 위치 오류)는 `:core:domain`/`:core:model`의 오류 값으로 바꾼다.
- 조사 병기(`을(를)`)는 조사 헬퍼 대신 조사가 필요 없는 문장 구조로 바꾼다. 한국어 화면 문구가 바뀌므로 2차 PT 이후에 한다.
- lint의 기본 로케일 관련 경고는 끈다.
