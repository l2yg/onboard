---
type: Guide
title: FusedLocation 보조 타입
description: FusedLocation SDK의 공개 보조 타입 6개.
status: draft
tags: [외부연동, API, Pleos]
relations:
  - type: refines
    target: ../2001_API_연동_참조.md
  - type: required_by
    target: 2124_위치_API.md
---

# FusedLocation 보조 타입

## 문서 정보

- 상태: 초안.
- 적용 범위: FusedLocation SDK의 공개 보조 타입 6개.
- 근거: 2026-10-27 조사한 공식 API 문서·릴리스 노트·배포 선언. 개별 출처는 본문에 명시한다.
- 관련 요구사항: REQ-CONTEST-001, REQ-CONTEST-009. 제공 기능의 조사이며 제품의 채택·구현 완료를 뜻하지 않는다.

## 목적과 범위

공개 타입 경로와 배포 선언이 대응하는 데이터 속성·리스너 콜백·enum·화면 요청 변형을 기록한다. 메서드 전체 선언은 관련 API 문서에서 관리한다. 자동 추출에서 확인되지 않은 계산 속성·기본값·생성자 상세는 원본 참조가 필요하다.

## 타입 정의

### FusedLocationListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused.listener/fused-location-listener/entry)

```text
fun onChanged(location: LocationData?)
```

```text
fun onError(exception: Exception)
```

### ProximityAlertListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused.listener/proximity-alert-listener/entry)

```text
fun onRegistered(alertId: String)
```

```text
fun onTransition(event: ProximityAlertEvent)
```

```text
fun onError(exception: Exception)
```

### LocationData

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused.model/location-data/entry)

| 속성 | 타입 |
|---|---|
| `latitude` | `Double` |
| `longitude` | `Double` |
| `altitude` | `Double` |
| `heading` | `Float` |
| `confidence` | `Float` |
| `timestampMillis` | `Long` |

### ProximityAlertEvent

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused.model/proximity-alert-event/entry)

| 속성 | 타입 |
|---|---|
| `alertId` | `String` |
| `transition` | `ProximityTransition` |

### ProximityAlertRequest

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused.model/proximity-alert-request/entry)

| 속성 | 타입 |
|---|---|
| `latitude` | `Double` |
| `longitude` | `Double` |
| `radiusMeters` | `Float` |
| `transitionTypes` | `Set<ProximityTransition>` |

### ProximityTransition

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused.model/proximity-transition/entry)

상수: `ENTER`, `EXIT`.

## 미정 사항과 확인 필요 사항

실제 계정 승인, 차종별 지원 영역·허용 값·주행 중 제한과 실차 동작은 미검증이다. 공개 문서·배포 선언에서 확인하지 못한 입력 의미·기본값·동작 보장은 추정하지 않는다. SDK 선택과 구체적인 제품 기능은 미정이며 개요 문서의 제공 상태·제약을 함께 확인한다.

## 관련 문서

| 문서 역할 | 문서 | 참조 목적 및 적용 범위 | 관련 요구사항 ID |
| --- | --- | --- | --- |
| 상위 개요 | [제공 API](../2001_API_연동_참조.md) | 버전·사용 가능 상태·공통 계약·해석 주의 사항 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [위치 API](2124_위치_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
