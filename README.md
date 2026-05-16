# AutoAgents Android App

`autoagents_researcher`(Flask 기반 AI 블로그/리서치 서비스)와 동일한 기능을 제공하는 안드로이드 클라이언트입니다.

## 기능

- **홈**: 기본 RSS 피드(GeekNews, TechCrunch, The Verge, Wired, …) 12개에서 최신 기사를 수집하고 LLM이 한국어 요약/태그를 자동 생성하여 카드 형태로 표시합니다.
- **리서치 채팅**: 관심 키워드 시스템 프롬프트가 적용된 LLM과 대화합니다.
- **유튜브 요약**: 유튜브 URL을 붙여넣으면 `smart-subtitles` API에서 자막을 받아 LLM이 한국어 마크다운 요약을 생성하고 홈 피드에도 저장합니다.
- **메모**: 마크다운 기반 로컬 메모 (Room).
- **데일리 인사이트**: 최근 기사 묶음을 바탕으로 한국어 트렌드 브리핑을 생성합니다.
- **설정**: LLM 엔드포인트/토큰/모델/유튜브 모델/관심 키워드를 런타임에 변경 가능. 기본값 복원 버튼 지원.

## 기본 LLM 설정 (`autoagents_researcher/.env`에서 반영)

| 항목 | 값 |
| --- | --- |
| Endpoint | `https://vllmapi.tor-ai.com/v1` |
| Token | `dgx_UCM-1apva9cTxr4L9bJfAx2p1eLdVBp6TgfCIMk84eg` |
| Model | `google/gemma-4-26B-A4B-it` |
| YouTube model | `google/gemma-4-26B-A4B-it` |
| YouTube API | `https://tubeapi.tor-ai.com/smart-subtitles` |

이 값들은 빌드 시 `BuildConfig`에 주입되며, 첫 실행 시 DataStore에 기본값으로 채워집니다. 사용자는 설정 화면에서 언제든지 변경할 수 있습니다.

## 기술 스택

- Kotlin 2.0.21, Android Gradle Plugin 8.5.2
- Jetpack Compose (Material3), Navigation Compose
- Room 2.6.1, DataStore Preferences 1.1.1
- OkHttp 4.12.0, kotlinx-serialization-json 1.7.3
- Jsoup 1.18.1 (RSS 파서), Coil 2.7.0 (이미지), compose-markdown 0.5.4

## 빌드

이 저장소는 **GitHub Actions로만 빌드**됩니다 (로컬 환경에 JDK가 없음). `main` 브랜치 push 시 다음이 자동 실행됩니다.

1. JDK 17, Gradle 8.9 셋업
2. `:app:assembleDebug`
3. `:app:assembleRelease` (unsigned)
4. Lint
5. Debug/Release APK 아티팩트 업로드

빌드 결과는 Actions 탭의 "Android Build" 워크플로 실행 페이지에서 다운로드합니다.

## 로컬 푸시 스크립트

`.env`(gitignored)에 GitHub PAT을 저장하고 `scripts/push.sh`를 사용해 푸시합니다:

```
GITHUB_USER=ULTRAKKK1004
GITHUB_TOKEN=ghp_xxx
GITHUB_REPO=ULTRAKKK1004/autoagents_app
```

## 권한

- `INTERNET` — LLM/RSS/유튜브 자막 API
- `ACCESS_NETWORK_STATE` — 네트워크 상태 확인

네트워크 외 권한은 사용하지 않습니다. 영구 저장은 앱 전용 디렉터리의 Room/DataStore만 사용합니다.
