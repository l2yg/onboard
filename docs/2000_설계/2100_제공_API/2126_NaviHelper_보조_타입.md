---
type: Guide
title: NaviHelper 보조 타입
description: NaviHelper SDK의 공개 보조 타입 47개.
status: draft
tags: [외부연동, API, Pleos]
relations:
  - type: refines
    target: ../2001_API_연동_참조.md
  - type: required_by
    target: 2121_내비게이션_API.md
---

# NaviHelper 보조 타입

## 문서 정보

- 상태: 초안.
- 적용 범위: NaviHelper SDK의 공개 보조 타입 47개.
- 근거: 2026-10-27 조사한 공식 API 문서·릴리스 노트·배포 선언. 개별 출처는 본문에 명시한다.
- 관련 요구사항: REQ-CONTEST-001, REQ-CONTEST-009. 제공 기능의 조사이며 제품의 채택·구현 완료를 뜻하지 않는다.

## 목적과 범위

공개 타입 경로와 배포 선언이 대응하는 데이터 속성·리스너 콜백·enum·화면 요청 변형을 기록한다. 메서드 전체 선언은 관련 API 문서에서 관리한다. 자동 추출에서 확인되지 않은 계산 속성·기본값·생성자 상세는 원본 참조가 필요하다.

## 타입 정의

### NaviHelperEventListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper.listener/navi-helper-event-listener/entry)

```text
fun onRouteStarted(routeStartInfo: RouteStartInfo)
```

```text
fun onRouteEnded()
```
폐기 예정.

```text
fun onRoutingCompleted()
```

```text
fun onReRouteEnded()
```

```text
fun onRouteOptionChanged(routeOption: RouteOption)
```

```text
fun onRouteCancelled()
```

```text
fun onWaypointChanged(waypointChangedInfo: WaypointChangedInfo)
```

```text
fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo)
```

```text
fun onWayPointArrived(waypointArrivedInfo: WaypointArrivedInfo)
```

```text
fun onBookMarkChanged()
```

```text
fun onDestinationChanged()
```

```text
fun onError(errorCode: NaviErrorCode)
```

```text
fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo)
```

```text
fun onRouteStateInfo(routeStateInfo: RouteStateInfo)
```

```text
fun onDestinationInfo(destinationInfo: DestinationInfo)
```

```text
fun onWayPointInfo(waypointInfoList: List<WaypointInfo>)
```

```text
fun onRecentDestinationInfo(recentDestinationInfoList: List<RecentDestinationInfo>)
```

```text
fun onBookmarkInfo(bookmarkInfoList: List<BookmarkInfo>)
```

```text
fun onCurrentRoadSpeedLimit(speedLimit: Int)
```

```text
fun onCameraAlert(cameraAlertList: List<CameraAlert>)
```

```text
fun onDrivingInfo(drivingInfo: DrivingInfo)
```

```text
fun onNavigationStatus(status: NavigationStatus)
```

```text
fun onTBTInfo(tbtInfoList: List<TBTInfo>)
```

```text
fun onChargerOperatorInfo(chargerOperatorList: List<ChargerOperator>)
```

```text
fun onCautionPointInfo(cautionPointList: List<CautionPoint>)
```

```text
fun onTripInfo(tripInfo: TripInfo)
```

### BookmarkType

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/bookmark-type/entry)

상수: `HOME`, `OFFICE`, `EV_CHARGING_STATION`, `DEFAULT`.

### CameraType

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/camera-type/entry)

상수: `SPEED_CAMERA_FIX`, `SPEED_CAMERA_MOV`, `SPEED_CAMERA_SECTION_START`, `SECTION_SPEED_ALERT`, `SPEED_CAMERA_SECTION_END`, `SPEED_CAMERA_TRAFFIC_LIGHT`, `SPEED_CAMERA_REAR`, `VARIABLE_SPEED_CAMERA`, `SPEED_CAMERA_SCHOOL_ZONE`, `SPEED_CAMERA_SCHOOL_ZONE_TRAFFIC_LIGHT`.

### CautionPointType

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/caution-point-type/entry)

상수: `SPEED_BUMP`, `SCHOOL_ZONE`, `ACCIDENT_PRONE_AREA`, `RAILROAD_CROSSING`, `FOG_AREA`, `FALLING_ROCK_AREA`, `DOWNHILL_CAUTION_AREA`, `WILDLIFE_CROSSING`, `NARROW_ROAD`, `HIGHWAY_CONSTRUCTION`, `TRAFFIC_INFO_COLLECTION`, `NO_PARKING_ZONE`, `TRAFFIC_LIGHT_VIOLATION_ENFORCEMENT`, `BUS_LANE_VIOLATION_ENFORCEMENT`, `CUT_IN_VIOLATION_ENFORCEMENT`, `BLOCK_INTERSECTION_ENFORCEMENT`, `LANE_CHANGE_CAMERA_ALERT`, `SLIPPERY_ROAD_ALERT`, `ROAD_DAMAGE_ALERT`, `WRONG_WAY_DRIVING_ALERT`, `BUS_LANE_UNAVAILABLE_NOTICE`, `BUS_LANE_AVAILABLE_NOTICE`, `SIGN_MALFUNCION`.

### NaviErrorCode

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/navi-error-code/entry)

상수: `CAUSE_LOW_BATTERY`, `CAUSE_SEARCH_ROUTE_OVER_DISTANCE`, `CAUSE_SEARCH_ROUTE`, `CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE`, `CAUSE_SEARCH_TRIP`.

### NaviMode

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/navi-mode/entry)

상수: `NOT_WORKING`, `SAFE_GUIDING`, `ROUTE_GUIDING`, `SIMULATION_GUIDING`.

### NavigationStatus

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/navigation-status/entry)

상수: `INACTIVE`, `INITIALIZING`, `FREE_DRIVE`, `ROUTE_DRIVE`.

### RouteOption

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/route-option/entry)

상수: `RECOMMENDED`, `SHORT_DISTANCE`, `SHORT_TIME`, `FREE`, `HIGHWAY`, `COMFORTABLE`, `SCHOOL_ZONE`.

### RoutePointPoiType

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/route-point-poi-type/entry)

상수: `NORMAL`, `EV_CHARGING`.

### RoutePointSettingType

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/route-point-setting-type/entry)

상수: `AUTO`, `MANUAL`.

### RoutePointType

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/route-point-type/entry)

상수: `ORIGIN`, `WAYPOINT`, `DESTINATION`.

### TrafficCongestionType

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/traffic-congestion-type/entry)

상수: `UNKNOWN`, `FAST`, `MODERATE`, `SLOW`, `HEAVY`.

### TrafficEventType

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/traffic-event-type/entry)

상수: `ETC`, `ACCIDENT`, `ROAD_DAMAGE`, `REPAIRING`, `EVENT`, `DISASTER`.

### TurnType

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/turn-type/entry)

| 속성 | 타입 |
|---|---|
| `value` | `Long` |

상수: `TURN_UNKNOWN`, `TURN_STRAIGHT`, `TURN_RIGHT`, `TURN_LEFT`, `TURN_KEEP_R`, `TURN_KEEP_L`, `TURN_SHARP_R`, `TURN_SHARP_L`, `TURN_UTURN`, `TURN_ENTER_IC_R`, `TURN_ENTER_IC_L`, `TURN_ENTER_IC_C`, `TURN_EXIT_IC_R`, `TURN_EXIT_IC_L`, `TURN_EXIT_IC_C`, `TURN_ENTER_UNDER_R`, `TURN_ENTER_UNDER_L`, `TURN_ENTER_UNDER_C`, `TURN_AVOID_UNDER_R`, `TURN_AVOID_UNDER_L`, `TURN_AVOID_UNDER_C`, `TURN_ENTER_OVER_R`, `TURN_ENTER_OVER_L`, `TURN_ENTER_OVER_C`, `TURN_AVOID_OVER_R`, `TURN_AVOID_OVER_L`, `TURN_AVOID_OVER_C`, `TURN_ENTER_ROUNDABOUT_02`, `TURN_ENTER_ROUNDABOUT_03`, `TURN_ENTER_ROUNDABOUT_04`, `TURN_ENTER_ROUNDABOUT_06`, `TURN_ENTER_ROUNDABOUT_08`, `TURN_ENTER_ROUNDABOUT_09`, `TURN_ENTER_ROUNDABOUT_10`, `TURN_ENTER_ROUNDABOUT_12`, `TURN_EXIT_ROUNDABOUT`, `TURN_WAY`, `TURN_ARRIVE`, `TURN_TUNNEL`, `TURN_DEVIDE_MERGE`, `TURN_TOLL`, `TURN_ENTER_SERVICEAREA`, `TURN_RESTAREA`.

### WaypointCmdType

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/waypoint-cmd-type/entry)

상수: `ADD`, `REMOVE`.

### WaypointIndex

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/waypoint-index/entry)

상수: `FIRST`, `SECOND`, `ALL`.

### BookmarkInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/bookmark-info/entry)

| 속성 | 타입 |
|---|---|
| `latitude` | `Double` |
| `longitude` | `Double` |
| `displayLatitude` | `Double?` |
| `displayLongitude` | `Double?` |
| `name` | `String` |
| `poiId` | `String` |
| `poiSubId` | `String?` |
| `address` | `String` |
| `category` | `BookmarkType` |
| `poiProvider` | `String` |

### CameraAlert

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/camera-alert/entry)

| 속성 | 타입 |
|---|---|
| `id` | `String` |
| `type` | `CameraType` |
| `status` | `Boolean` |
| `speedLimit` | `Int` |
| `remainDistance` | `Int` |
| `sectionAvrSpeed` | `Int` |
| `isOverSpeed` | `Boolean` |
| `isOverSectionAvrSpeed` | `Boolean` |

### CautionPoint

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/caution-point/entry)

| 속성 | 타입 |
|---|---|
| `id` | `String` |
| `type` | `CautionPointType` |
| `remainDistance` | `Int` |

### ChargerOperator

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/charger-operator/entry)

| 속성 | 타입 |
|---|---|
| `id` | `String` |
| `name` | `String` |

### ChargingPlan

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/charging-plan/entry)

| 속성 | 타입 |
|---|---|
| `duration` | `Int` |
| `startSoc` | `Int` |
| `endSoc` | `Int` |

### CurrentLocationInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/current-location-info/entry)

| 속성 | 타입 |
|---|---|
| `latitude` | `Double` |
| `longitude` | `Double` |
| `heading` | `Double` |
| `address` | `String` |
| `routeId` | `String` |
| `currentRouteIndex` | `Int?` |

### DestinationArrivedInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/destination-arrived-info/entry)

| 속성 | 타입 |
|---|---|
| `duration` | `Int` |
| `distance` | `Int` |
| `name` | `String` |
| `address` | `String` |

### DestinationInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/destination-info/entry)

| 속성 | 타입 |
|---|---|
| `latitude` | `Double` |
| `longitude` | `Double` |
| `displayLatitude` | `Double?` |
| `displayLongitude` | `Double?` |
| `name` | `String` |
| `address` | `String` |
| `remainDistance` | `Int` |
| `remainTime` | `Int` |
| `tollFare` | `Int` |
| `poiId` | `String` |
| `poiSubId` | `String?` |
| `poiProvider` | `String` |

### DrivingInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/driving-info/entry)

| 속성 | 타입 |
|---|---|
| `destination` | `DrivingState` |
| `waypoints` | `List<DrivingState>` |

### DrivingState

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/driving-state/entry)

| 속성 | 타입 |
|---|---|
| `name` | `String` |
| `duration` | `Int` |
| `distance` | `Int` |
| `remainDestinationChargingValue` | `Int` |
| `isChargingStation` | `Boolean` |
| `isRecommendedChargingStation` | `Boolean` |
| `maxChargingPower` | `Int` |
| `targetSoc` | `Int` |
| `chargerCount` | `Int` |
| `chargingPlan` | `ChargingPlan?` |

### NavigationScreenRequest

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/navigation-screen-request/entry)

각 필드는 기반 타입의 공통 속성이 아니라 아래 요청 변형의 생성자 입력이다.

| 요청 변형 | 입력 | 의미·기본값 |
| --- | --- | --- |
| `OverviewRoute` | `info: RouteOverviewShowInfo` | 전체 경로 표시 설정 |
| `SearchPlace` | `keyword: String` | 장소 검색어 |
| `POIDetailPopup` | `poiId: String`, `poiProvider: String = ""` | POI 식별자와 제공자. 제공자 기본값은 빈 문자열 |

변형: Foreground, FavoriteMain, AddHomeFavorite, AddWorkFavorite, NearbyChargingStations, Settings, MoveToCurrentLocationOnMap, OverviewRoute, SearchPlace, POIDetailPopup, RecentDestinations, RouteOptionList, EditRoute.

### RecentDestinationInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/recent-destination-info/entry)

| 속성 | 타입 |
|---|---|
| `latitude` | `Double` |
| `longitude` | `Double` |
| `displayLatitude` | `Double?` |
| `displayLongitude` | `Double?` |
| `name` | `String` |
| `address` | `String` |
| `poiId` | `String` |
| `poiSubId` | `String?` |
| `poiProvider` | `String` |

### RequestWaypointInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/request-waypoint-info/entry)

| 속성 | 타입 |
|---|---|
| `latitude` | `Double` |
| `longitude` | `Double` |
| `poiName` | `String?` |
| `waypointIndex` | `WaypointIndex` |
| `poiId` | `String` |
| `address` | `String` |
| `poiSubId` | `String` |
| `poiProvider` | `String` |

### RouteInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/route-info/entry)

| 속성 | 타입 |
|---|---|
| `latitude` | `Double` |
| `longitude` | `Double` |
| `displayLatitude` | `Double?` |
| `displayLongitude` | `Double?` |
| `poiName` | `String` |
| `poiId` | `String` |
| `address` | `String` |
| `poiSubId` | `String` |
| `routeOption` | `RouteOption` |
| `poiProvider` | `String` |
| `isMobileRequest` | `Boolean` |

### RouteOverviewShowInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/route-overview-show-info/entry)

| 속성 | 타입 |
|---|---|
| `durationMs` | `Int` |

### RoutePointInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/route-point-info/entry)

| 속성 | 타입 |
|---|---|
| `latitude` | `Double` |
| `longitude` | `Double` |
| `poiName` | `String` |
| `poiId` | `String` |
| `ratioOnRoute` | `Double` |
| `poiType` | `RoutePointPoiType` |
| `pointType` | `RoutePointType` |
| `settingType` | `RoutePointSettingType` |

### RouteStartInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/route-start-info/entry)

| 속성 | 타입 |
|---|---|
| `duration` | `Int` |
| `distance` | `Int` |
| `tollFare` | `Int` |
| `name` | `String` |
| `remainDestinationChargingValue` | `Int` |

### RouteStateInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/route-state-info/entry)

| 속성 | 타입 |
|---|---|
| `termsAgreement` | `Boolean` |
| `naviMode` | `NaviMode` |
| `isRouting` | `Boolean` |
| `routeOption` | `RouteOption` |
| `remainDestinationChargingInt` | `Int` |
| `remainWaypointChargingInt` | `List<Int>` |
| `trafficStatus` | `TrafficStatus?` |

### RouteWithWaypointsRequest

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/route-with-waypoints-request/entry)

| 속성 | 타입 |
|---|---|
| `destination` | `RouteInfo` |
| `waypoints` | `List<RequestWaypointInfo>` |

### TBTInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/t-b-t-info/entry)

| 속성 | 타입 |
|---|---|
| `type` | `TurnType` |
| `distance` | `Double` |
| `description` | `String` |

### TrafficCongestionInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/traffic-congestion-info/entry)

| 속성 | 타입 |
|---|---|
| `offset` | `Double` |
| `distance` | `Double` |
| `trafficCongestionTime` | `Long` |
| `trafficCongestionType` | `TrafficCongestionType` |
| `avrSpeed` | `Double` |
| `roadName` | `String` |
| `startCrossName` | `String` |
| `endCrossName` | `String` |

### TrafficCongestionSummary

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/traffic-congestion-summary/entry)

| 속성 | 타입 |
|---|---|
| `totalCongestionDist` | `Double` |
| `totalCongestionTime` | `Long` |
| `totalCongestionAvrSpeed` | `Double` |

### TrafficEventInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/traffic-event-info/entry)

| 속성 | 타입 |
|---|---|
| `latitude` | `Double` |
| `longitude` | `Double` |
| `eventType` | `TrafficEventType` |
| `startTime` | `String` |
| `endTime` | `String` |
| `description` | `String` |

### TrafficInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/traffic-info/entry)

| 속성 | 타입 |
|---|---|
| `congestionType` | `TrafficCongestionType` |
| `startRatio` | `Int` |
| `endRatio` | `Int` |

### TrafficStatus

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/traffic-status/entry)

| 속성 | 타입 |
|---|---|
| `summary` | `TrafficCongestionSummary` |
| `congestionItemList` | `List<TrafficCongestionInfo>` |
| `trafficEventList` | `List<TrafficEventInfo>` |

### TripInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/trip-info/entry)

| 속성 | 타입 |
|---|---|
| `distance` | `Int` |
| `eta` | `Int` |
| `estimatedSocAtDest` | `Int` |
| `energyConsumption` | `Double` |
| `summary` | `TripSummary` |

### TripRequest

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/trip-request/entry)

| 속성 | 타입 |
|---|---|
| `latitude` | `Double` |
| `longitude` | `Double` |
| `poiId` | `String` |
| `poiName` | `String` |
| `poiSubId` | `String?` |

### TripSummary

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/trip-summary/entry)

| 속성 | 타입 |
|---|---|
| `trafficConditions` | `List<TrafficInfo>` |
| `routePoints` | `List<RoutePointInfo>` |

### WaypointArrivedInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/waypoint-arrived-info/entry)

| 속성 | 타입 |
|---|---|
| `duration` | `Int` |
| `distance` | `Int` |
| `index` | `WaypointIndex` |
| `name` | `String` |
| `address` | `String` |

### WaypointChangedInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/waypoint-changed-info/entry)

| 속성 | 타입 |
|---|---|
| `index` | `WaypointIndex` |
| `cmdType` | `WaypointCmdType` |
| `routeStartInfo` | `RouteStartInfo` |

### WaypointInfo

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/waypoint-info/entry)

| 속성 | 타입 |
|---|---|
| `name` | `String` |
| `address` | `String` |
| `latitude` | `Double` |
| `longitude` | `Double` |
| `displayLatitude` | `Double?` |
| `displayLongitude` | `Double?` |
| `remainDistance` | `Int` |
| `remainTime` | `Int` |
| `poiId` | `String` |
| `poiSubId` | `String?` |
| `poiProvider` | `String` |

## 미정 사항과 확인 필요 사항

실제 계정 승인, 차종별 지원 영역·허용 값·주행 중 제한과 실차 동작은 미검증이다. 공개 문서·배포 선언에서 확인하지 못한 입력 의미·기본값·동작 보장은 추정하지 않는다. SDK 선택과 구체적인 제품 기능은 미정이며 개요 문서의 제공 상태·제약을 함께 확인한다.

## 관련 문서

| 문서 역할 | 문서 | 참조 목적 및 적용 범위 | 관련 요구사항 ID |
| --- | --- | --- | --- |
| 상위 개요 | [제공 API](../2001_API_연동_참조.md) | 버전·사용 가능 상태·공통 계약·해석 주의 사항 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [내비게이션 API](2121_내비게이션_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
