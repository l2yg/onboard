---
type: Guide
title: Vehicle 보조 타입
description: Vehicle SDK의 공개 보조 타입 116개.
status: draft
tags: [외부연동, API, Pleos]
relations:
  - type: refines
    target: ../2001_API_연동_참조.md
  - type: required_by
    target: 2101_차량_서비스_연결_API.md
  - type: required_by
    target: 2102_브레이크_API.md
  - type: required_by
    target: 2103_차량_식별_API.md
  - type: required_by
    target: 2104_표시_단위_API.md
  - type: required_by
    target: 2105_도어_API.md
  - type: required_by
    target: 2106_주행_모드_API.md
  - type: required_by
    target: 2107_전기차_배터리와_충전_API.md
  - type: required_by
    target: 2108_연료와_연비_API.md
  - type: required_by
    target: 2109_공조_API.md
  - type: required_by
    target: 2110_조명_API.md
  - type: required_by
    target: 2111_주행거리와_시간_API.md
  - type: required_by
    target: 2112_안전과_경고_API.md
  - type: required_by
    target: 2113_시트_API.md
  - type: required_by
    target: 2114_사이드미러_API.md
  - type: required_by
    target: 2115_스티어링_API.md
  - type: required_by
    target: 2116_타이어_API.md
  - type: required_by
    target: 2117_하이패스_API.md
  - type: required_by
    target: 2118_방향지시등_API.md
  - type: required_by
    target: 2119_창문과_스마트글라스_API.md
  - type: required_by
    target: 2120_와이퍼_API.md
---

# Vehicle 보조 타입

## 문서 정보

- 상태: 초안.
- 적용 범위: Vehicle SDK의 공개 보조 타입 116개.
- 근거: 2026-10-27 조사한 공식 API 문서·릴리스 노트·배포 선언. 개별 출처는 본문에 명시한다.
- 관련 요구사항: REQ-CONTEST-001, REQ-CONTEST-009. 제공 기능의 조사이며 제품의 채택·구현 완료를 뜻하지 않는다.

## 목적과 범위

공개 타입 경로와 배포 선언이 대응하는 데이터 속성·리스너 콜백·enum·화면 요청 변형을 기록한다. 메서드 전체 선언은 관련 API 문서에서 관리한다. 자동 추출에서 확인되지 않은 계산 속성·기본값·생성자 상세는 원본 참조가 필요하다.

## 타입 정의

### Area

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api.model.area/area/entry)

차량 영역의 기반 타입이다. Vehicle 2.3.2 소스 선언은 `sealed class Area`이며 자체 공개 속성은 없다.

### DoorArea

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api.model.area/door-area/entry)

`class DoorArea(vararg val values: DoorValue) : Area()` — 선택한 도어 영역을 `values`에 보관한다. 실제 지원 영역은 해당 capability 응답으로 확인한다.

### MirrorArea

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api.model.area/mirror-area/entry)

`class MirrorArea(vararg val values: MirrorValue) : Area()` — 선택한 미러 영역을 `values`에 보관한다. 실제 지원 영역은 해당 capability 응답으로 확인한다.

### SeatArea

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api.model.area/seat-area/entry)

`class SeatArea(vararg val values: SeatValue) : Area()` — 선택한 좌석 영역을 `values`에 보관한다. 실제 지원 영역은 해당 capability 응답으로 확인한다.

### TurnSignalArea

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api.model.area/turn-signal-area/entry)

`class TurnSignalArea(vararg val values: TurnSignalValue) : Area()` — 선택한 방향지시등 영역을 `values`에 보관한다. 실제 지원 영역은 해당 capability 응답으로 확인한다.

### WheelArea

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api.model.area/wheel-area/entry)

`class WheelArea(vararg val values: WheelValue) : Area()` — 선택한 바퀴 영역을 `values`에 보관한다. 실제 지원 영역은 해당 capability 응답으로 확인한다.

### WindowArea

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api.model.area/window-area/entry)

`class WindowArea(vararg val values: WindowValue) : Area()` — 선택한 창문 영역을 `values`에 보관한다. 실제 지원 영역은 해당 capability 응답으로 확인한다.

### AccessType

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api.model.capability/access-type/entry)

상수: `NONE`, `READ`, `WRITE`, `READ_WRITE`.

### AreaBounds

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api.model.capability/area-bounds/entry)

| 속성 | 타입 |
|---|---|
| `minValue` | `T` |
| `maxValue` | `T` |

### CapabilityConfig

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api.model.capability/capability-config/entry)

| 속성 | 타입 |
|---|---|
| `accessType` | `AccessType ` |

### HvacTemperatureConfigArray

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api.model.capability/hvac-temperature-config-array/entry)

온도·증가 간격 속성은 해당 단위의 실제 `Float` 값이다. `raw`는 변환 전 정수 6개이며 각각 온도 또는 증가 간격에 10을 곱한 값이다.

| 속성 | 타입 |
|---|---|
| `minCelsius` | `Float` |
| `maxCelsius` | `Float` |
| `incrementCelsius` | `Float` |
| `minFahrenheit` | `Float` |
| `maxFahrenheit` | `Float` |
| `incrementFahrenheit` | `Float` |
| `raw` | `List<Int>` |

### SmartGlassConfig

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api.model.capability/smart-glass-config/entry)

| 속성 | 타입 |
|---|---|
| `zoneCount` | `Int` |
| `levelCount` | `Int` |

### TripStatistics

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api.model.common/trip-statistics/entry)

| 속성 | 타입 |
|---|---|
| `current` | `T?` |
| `instantaneous` | `T?` |
| `cumulative` | `T?` |
| `sinceRefill` | `T?` |
| `raw` | `List<T?>` |

`current`·`instantaneous`·`cumulative`·`sinceRefill`의 기본값은 `null`, `raw`의 기본값은 빈 목록이다. 아래 메서드는 지정한 통계 종류에 대응하는 속성을 반환하며 새로 차량 정보를 조회하지 않는다.

```text
fun get(type: TripStatisticType): T?
```

### TelltaleData

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api.model.telltale/telltale-data/entry)

| 속성 | 타입 |
|---|---|
| `type` | `TelltaleType` |
| `state` | `TelltaleState` |
| `color` | `Int` |
| `priority` | `Int` |
| `warningCount` | `Int` |

### Telltales

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api.model.telltale/telltales/entry)

| 속성 | 타입 |
|---|---|
| `telltales` | `List<TelltaleData> ` |

### VehicleException

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.callback.exception/vehicle-exception/entry)

**배포물 불일치:** 아래는 공식 소스·문서의 정의다. 확인한 Vehicle 2.3.2 AAR에는 이 원래 클래스명이 없고 동일 패키지에 난독화된 예외 클래스가 있다. 직접 import하는 Java 컴파일 검증이 실패했다. 소스에 정의된 `errorCode` 접근이 현재 바이너리에서 그대로 가능하다고 가정하지 않으며 난독화된 이름에 의존하지 않는다. 상세 근거는 상위 문서의 SDK 검증 결과를 참조한다.

| 속성 | 타입 |
|---|---|
| `errorCode` | `ErrorCode` |
| `message` | `String` |

### PleosPermission

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant/pleos-permission/entry)

| 속성 | 타입 |
|---|---|
| `CONTROL_CAR_CLIMATE` | `String` |
| `CONTROL_CAR_DOORS` | `String` |
| `CONTROL_CAR_ENERGY` | `String` |
| `CONTROL_CAR_MIRRORS` | `String` |
| `CONTROL_CAR_SEATS` | `String` |
| `CONTROL_CAR_WINDOWS` | `String` |
| `CONTROL_CAR_INFO` | `String` |
| `CONTROL_CAR_TOLL` | `String` |
| `CONTROL_CAR_DRIVE` | `String` |
| `CONTROL_CAR_LIGHTS` | `String` |
| `CONTROL_CAR_SAFETY` | `String` |
| `CAR_ENERGY` | `String` |
| `CAR_INFO` | `String` |
| `CAR_TOLL` | `String` |
| `CAR_DRIVE` | `String` |
| `CAR_LIGHTS` | `String` |
| `CAR_SAFETY` | `String` |
| `CAR_SEATS` | `String` |
| `CAR_CLIMATE` | `String` |
| `CAR_WINDOWS` | `String` |
| `CAR_DOORS` | `String` |
| `CAR_MIRRORS` | `String` |

### AreaValue

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.area/area-value/entry)

| 속성 | 타입 |
|---|---|
| `id` | `Int` |

### DoorValue

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.area/door-value/entry)

상수: `ROW_1_LEFT`, `ROW_2_LEFT`, `ROW_3_LEFT`, `HOOD`.

### MirrorValue

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.area/mirror-value/entry)

상수: `DRIVER_LEFT`, `DRIVER_CENTER`.

### SeatValue

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.area/seat-value/entry)

상수: `ROW_1_LEFT`, `ROW_2_LEFT`, `ROW_3_LEFT`.

### TurnSignalValue

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.area/turn-signal-value/entry)

상수: `OFF`, `RIGHT`, `LEFT`.

### WheelValue

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.area/wheel-value/entry)

상수: `UNKNOWN`, `LEFT_FRONT`, `LEFT_REAR`.

### WindowValue

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.area/window-value/entry)

상수: `FRONT_WINDSHIELD`, `ROW_1_LEFT`, `ROW_2_LEFT`, `ROOF_TOP_1`.

### CruiseControl

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.control/cruise-control/entry)

상수: `OTHER`, `ENABLED`, `ACTIVATED`, `USER_OVERRIDE`, `SUSPENDED`, `FORCED_DEACTIVATION_WARNING`.

### DrivingModeType

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.control/driving-mode-type/entry)

상수: `NORMAL`, `ECO`, `SPORT`.

### EvChargeState

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.control/ev-charge-state/entry)

상수: `UNKNOWN`, `CHARGING`, `FULLY_CHARGED`, `NOT_CHARGING`, `ERROR`.

### EvConnectorType

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.control/ev-connector-type/entry)

상수: `UNKNOWN`, `IEC_TYPE_1_AC`, `IEC_TYPE_2_AC`, `IEC_TYPE_3_AC`, `IEC_TYPE_4_DC`, `IEC_TYPE_1_CCS_DC`, `IEC_TYPE_2_CCS_DC`, `TESLA_ROADSTER`, `TESLA_HPWC`, `TESLA_SUPERCHARGER`, `GBT_AC`, `GBT_DC`, `OTHER`.

### EvRegenerativeBrakingState

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.control/ev-regenerative-braking-state/entry)

상수: `UNKNOWN`, `DISABLED`, `PARTIALLY_ENABLED`, `FULLY_ENABLED`.

### FanDirectionType

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.control/fan-direction-type/entry)

상수: `UNKNOWN`, `FACE`, `FLOOR`, `FACE_AND_FLOOR`, `DEFROST`, `FACE_AND_DEFROST`, `DEFROST_AND_FLOOR`, `FACE_AND_FLOOR_AND_DEFROST`.

### FuelType

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.control/fuel-type/entry)

상수: `BIODIESEL`, `CNG`, `DIESEL_1`, `DIESEL_2`, `E85`, `ELECTRIC`, `HYDROGEN`, `LEADED`, `LNG`, `LPG`, `OTHER`, `UNKNOWN`, `UNLEADED`.

### GsrComplianceType

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.control/gsr-compliance-type/entry)

상수: `GSR_COMPLIANCE_TYPE_NOT_REQUIRED`, `GSR_COMPLIANCE_TYPE_REQUIRED_V1`.

### LightState

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.control/light-state/entry)

상수: `OFF`, `ON`, `DAYTIME_RUNNING`.

### LightSwitch

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.control/light-switch/entry)

상수: `OFF`, `ON`, `DAYTIME_RUNNING`, `AUTOMATIC`.

### MoveControl

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.control/move-control/entry)

상수: `BACKWARD`, `STOP`, `FORWARD`.

### OilLevelState

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.control/oil-level-state/entry)

상수: `LOW`, `HIGH`, `NORMAL`, `ERROR`, `CRITICALLY_LOW`.

### PortLocationType

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.control/port-location-type/entry)

상수: `UNKNOWN`, `FRONT_LEFT`, `FRONT_RIGHT`, `REAR_RIGHT`, `REAR_LEFT`, `FRONT`, `REAR`.

### SmartGlassZoneCommand

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.control/smart-glass-zone-command/entry)

상수: `SKIP`, `OFF`, `ON`.

### SteeringControl

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.control/steering-control/entry)

상수: `OFF`, `READY`, `ACTIVE`.

### TripStatisticType

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.control/trip-statistic-type/entry)

| 속성 | 타입 |
|---|---|
| `id` | `Int` |

상수: `CURRENT`, `INSTANTANEOUS`, `CUMULATIVE`, `SINCE_REFILL`.

### WiperState

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.control/wiper-state/entry)

상수: `OTHER`, `OFF`, `ON`, `SERVICE`.

### WiperSwitch

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.control/wiper-switch/entry)

상수: `OTHER`, `OFF`, `MIST`, `INTERMITTENT_LEVEL_1`, `INTERMITTENT_LEVEL_2`, `INTERMITTENT_LEVEL_3`, `INTERMITTENT_LEVEL_4`, `INTERMITTENT_LEVEL_5`, `CONTINUOUS_LEVEL_1`, `CONTINUOUS_LEVEL_2`, `CONTINUOUS_LEVEL_3`, `CONTINUOUS_LEVEL_4`, `CONTINUOUS_LEVEL_5`, `AUTO`, `SERVICE`.

### TelltaleColor

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.telltale/telltale-color/entry)

상수: `WHITE`, `GREEN`, `BLUE`, `YELLOW`, `RED`, `GRAY`.

### TelltaleState

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.telltale/telltale-state/entry)

상수: `OFF`, `ON`, `BLINK`.

### TelltaleType

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.telltale/telltale-type/entry)

상수: `PARKING_BRAKE`, `ELECTRONIC_PARKING_BRAKE`, `SEAT_BELT`, `DOOR_OPEN`, `ABS`, `MDPS`, `TPMS`, `LOW_VOLT_BATTERY`, `ESC`, `ESC_OFF`, `AIRBAG`, `AFS`, `MASTER_WARNING`, `AUTO_HOLD`, `AUTO_HIGH_BEAM`, `HEADLAMP_LOW`, `HEADLAMP_HIGH`, `POSITION_LAMP`, `FRONT_FOG_LAMP`, `REAR_FOG_LAMP`, `ENGINE_OIL_PRESSURE`, `ENGINE_TEMPERATURE`, `EMERGENCY_CALL`, `EV_SYSTEM`, `POWER_DOWN`, `RBS`, `IMMOBILIZER`, `ENGINE`, `LED_HEADLAMP`, `PREHEAT`, `PARTICLE_FILTER`, `H2_SENSOR`, `H2_LEAK`, `PASSENGER_AIRBAG_OFF`, `PASSENGER_AIRBAG_ON`, `TURN_SIGNAL_LEFT`, `TURN_SIGNAL_RIGHT`, `ICY_ROAD`, `OVER_SPEED`, `WATER_SEPARATOR`, `SCR`, `LOW_EV_BATTERY`, `HEV_SYSTEM`, `FCEV_SYSTEM`, `EREV_SYSTEM`, `LPI`, `EV_CHARGING_CONNECTOR`, `AWD_LOCK`, `HEV_MODE`, `ISG`, `DBC`, `REV`, `XEV_READY`, `I_PEDAL`, `LOW_FUEL`, `LOW_FUEL_FCEV`, `EV_BATTERY_CONDITIONING`, `TRUNK_OPEN`, `CHILD_PRESENCE_DETECTION_OFF`, `DRIVE_MODE`, `DRIVE_MODE_SPORT`, `DRIVE_MODE_ECO`, `DRIVE_MODE_COMFORT`, `DRIVE_MODE_TERRAIN`, `FORCED_ALL_WHEEL_DRIVE`, `FOUR_WHEEL_DRIVE_MODE_2H`, `FOUR_WHEEL_DRIVE_MODE_4A`, `FOUR_WHEEL_DRIVE_MODE_4H`, `FOUR_WHEEL_DRIVE_MODE_4L`, `TOW_MODE`, `ESC_SPORT`, `CREEP_OFF`, `DRIFT_MODE`, `TRACK_MODE`, `UTILITY_MODE`, `GREEN_ZONE`, `PHEV_MODE`, `PHEV_MODE_HEV`, `PHEV_MODE_AUTO`, `PHEV_MODE_EV_PLUS`, `EREV_MODE`, `LAUNCH_MODE`, `ACCEL_RESPONSE_DASH`, `ACCEL_RESPONSE_ACCEL`, `N_BATTERY_DRAG`, `N_BATTERY_SPRINT`, `N_BATTERY_ENDURANCE`, `FCA`, `LKA`, `LFA`, `LCA_LEFT`, `LCA_RIGHT`, `ELK`, `ICC`, `DRIVER_ATTENTION`.

### DistanceDisplayUnit

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.unit/distance-display-unit/entry)

상수: `KILOMETER`, `MILE`.

### EvBatteryDisplayUnit

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.unit/ev-battery-display-unit/entry)

상수: `WATT_HOUR`, `AMPERE_HOURS`, `KILOWATT_HOUR`.

### SpeedDisplayUnit

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.unit/speed-display-unit/entry)

상수: `METER_PER_SEC`, `MILES_PER_HOUR`, `KILOMETERS_PER_HOUR`.

### TemperatureUnit

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.unit/temperature-unit/entry)

상수: `CELSIUS`, `FAHRENHEIT`.

### AmbientColorAutoByDriveModeEnabledListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/ambient-color-auto-by-drive-mode-enabled-listener/entry)

```text
fun onAmbientColorAutoByDriveModeEnabledUpdated(enabled: Boolean?)
```

### AmbientLightBrightnessColorListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/ambient-light-brightness-color-listener/entry)

```text
fun onBrightnessColorUpdated(argbColor: Int?)
```

### AmbientLightBrightnessListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/ambient-light-brightness-listener/entry)

```text
fun onBrightnessUpdated(brightness: Int?)
```

### BrakePedalStatusListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/brake-pedal-status-listener/entry)

```text
fun onBrakePedalStatusUpdated(status: Boolean?)
```

### CruiseControlStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/cruise-control-state-listener/entry)

```text
fun onCruiseControlStateUpdated(state: CruiseControl?)
```

### CruiseControlTargetSpeedListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/cruise-control-target-speed-listener/entry)

```text
fun onCruiseControlTargetSpeedUpdated(speed: Float?)
```

### DistanceDrivenListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/distance-driven-listener/entry)

```text
fun onDistanceDrivenUpdated(value: Float?)
```

### DoorLockStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/door-lock-state-listener/entry)

```text
fun onDoorLockStateUpdated(state: Boolean?)
```

### DoorPositionListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/door-position-listener/entry)

```text
fun onPositionUpdated(pos: Int?)
```

### DrivingModeStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/driving-mode-state-listener/entry)

```text
fun onDrivingModeStateUpdated(state: DrivingModeType?)
```

### DrivingTimeListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/driving-time-listener/entry)

```text
fun onDrivingTimeUpdated(value: Int?)
```

### EtcsVoiceGuidanceVolumeListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/etcs-voice-guidance-volume-listener/entry)

```text
fun onEtcsVoiceGuidanceVolumeUpdated(volume: Int?)
```

### EvBatteryEfficiencyListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/ev-battery-efficiency-listener/entry)

```text
fun onEvBatteryEfficiencyUpdated(efficiency: Float?)
```

### EvBatteryStateOfChargeListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/ev-battery-state-of-charge-listener/entry)

```text
fun onEvBatteryStateOfChargeUpdated(value: Int?)
```

### FuelEconomyListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/fuel-economy-listener/entry)

```text
fun onFuelEconomyUpdated(efficiency: Float?)
```

### FuelPercentListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/fuel-percent-listener/entry)

```text
fun onFuelPercentUpdated(value: Int?)
```

### GloveBoxLockStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/glove-box-lock-state-listener/entry)

```text
fun onGloveBoxLockStateUpdated(state: Boolean?)
```

### HvacAcStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/hvac-ac-state-listener/entry)

```text
fun onHvacAcStateUpdated(state: Boolean?)
```

### HvacAirCleanerStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/hvac-air-cleaner-state-listener/entry)

```text
fun onHvacAirCleanerStateUpdated(state: Boolean?)
```

### HvacAutoStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/hvac-auto-state-listener/entry)

```text
fun onHvacAutoStateUpdated(state: Boolean?)
```

### HvacDefrosterStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/hvac-defroster-state-listener/entry)

```text
fun onHvacDefrosterStateUpdated(state: Boolean?)
```

### HvacDualStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/hvac-dual-state-listener/entry)

```text
fun onHvacDualStateUpdated(state: Boolean?)
```

### HvacFanSpeedListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/hvac-fan-speed-listener/entry)

```text
fun onHvacFanSpeedUpdated(speed: Int?)
```

### HvacMaxAcStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/hvac-max-ac-state-listener/entry)

```text
fun onHvacMaxAcStateUpdated(state: Boolean?)
```

### HvacMaxDefrostStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/hvac-max-defrost-state-listener/entry)

```text
fun onHvacMaxDefrostStateUpdated(state: Boolean?)
```

### HvacPowerStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/hvac-power-state-listener/entry)

```text
fun onHvacPowerStateUpdated(state: Boolean?)
```

### HvacRecirculationStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/hvac-recirculation-state-listener/entry)

```text
fun onHvacAutoRecirculationStateUpdated(state: Boolean?)
```

### HvacSeatTemperatureListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/hvac-seat-temperature-listener/entry)

```text
fun onHvacSeatTemperatureUpdated(temperature: Int?)
```

### HvacSeatVentilationListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/hvac-seat-ventilation-listener/entry)

```text
fun onHvacSeatVentilationUpdated(ventilation: Int?)
```

### HvacSideMirrorHeatStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/hvac-side-mirror-heat-state-listener/entry)

```text
fun onHvacSideMirrorHeatStateUpdated(state: Int?)
```

### HvacSteeringWheelHeatStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/hvac-steering-wheel-heat-state-listener/entry)

```text
fun onHvacSteeringWheelHeatStateUpdated(state: Int?)
```

### HvacTemperatureListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/hvac-temperature-listener/entry)

```text
fun onHvacTemperatureUpdated(temperature: Float?)
```

### HvacTemperatureValueListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/hvac-temperature-value-listener/entry)

```text
fun onHvacTemperatureUpdated(celsius: Float?, fahrenheit: Float?)
```

### LightStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/light-state-listener/entry)

```text
fun onLightStateUpdated(state: LightState?)
```

### LowWasherFluidWaringListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/low-washer-fluid-waring-listener/entry)

```text
fun onLowWasherFluidWaringUpdated(state: Boolean?)
```

### OilLevelStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/oil-level-state-listener/entry)

```text
fun onOilLevelUpdated(level: OilLevelState?)
```

### PowerDownWarningIndicatorListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/power-down-warning-indicator-listener/entry)

```text
fun onPowerDownWarningIndicatorUpdated(state: Boolean?)
```

### SeatAirbagDeployedListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/seat-airbag-deployed-listener/entry)

```text
fun onSeatAirbagDeployUpdated(deployed: Boolean?)
```

### SeatAirbagEnabledListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/seat-airbag-enabled-listener/entry)

```text
fun onSeatAirbagEnabledUpdated(enabled: Boolean?)
```

### SeatBackrestAnglePositionListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/seat-backrest-angle-position-listener/entry)

```text
fun onSeatBackrestAnglePositionUpdated(position: Int?)
```

### SeatBeltBuckledStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/seat-belt-buckled-state-listener/entry)

```text
fun onSeatBeltBuckledStateUpdated(state: Boolean?)
```

### SeatBeltHeightPositionListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/seat-belt-height-position-listener/entry)

```text
fun onSeatBeltHeightPositionUpdated(position: Int?)
```

### SeatBeltNotBuckledWarningListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/seat-belt-not-buckled-warning-listener/entry)

```text
fun onSeatBeltNotBuckledWarningUpdated(state: Boolean?)
```

### SeatDepthPositionListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/seat-depth-position-listener/entry)

```text
fun onSeatDepthPositionUpdated(position: Int?)
```

### SeatForeAftPositionListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/seat-fore-aft-position-listener/entry)

```text
fun onSeatForeAftPositionUpdated(position: Int?)
```

### SeatHeadrestAnglePositionListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/seat-headrest-angle-position-listener/entry)

```text
fun onSeatHeadrestAnglePositionUpdated(position: Int?)
```

### SeatHeadrestForeAftPositionListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/seat-headrest-fore-aft-position-listener/entry)

```text
fun onSeatHeadrestForeAftPositionUpdated(position: Int?)
```

### SeatHeadrestHeightAftPositionListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/seat-headrest-height-aft-position-listener/entry)

```text
fun onSeatHeadrestHeightAftPositionUpdated(position: Int?)
```

### SeatHeightPositionListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/seat-height-position-listener/entry)

```text
fun onSeatHeightPositionUpdated(position: Int?)
```

### SeatLumbarForeAftPositionListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/seat-lumbar-fore-aft-position-listener/entry)

```text
fun onSeatLumbarForeAftPositionUpdated(position: Int?)
```

### SeatLumbarSideSupportPositionListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/seat-lumbar-side-support-position-listener/entry)

```text
fun onSeatLumbarSideSupportPositionUpdated(position: Int?)
```

### SeatOccupancyListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/seat-occupancy-listener/entry)

```text
fun onSeatOccupancyUpdated(occupancy: Boolean?)
```

### SeatTiltPositionListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/seat-tilt-position-listener/entry)

```text
fun onSeatTiltPositionUpdated(position: Int?)
```

### SmartGlassLevelListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/smart-glass-level-listener/entry)

```text
fun onSmartGlassLevelUpdated(level: Int?)
```

### SmartGlassStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/smart-glass-state-listener/entry)

```text
fun onSmartGlassStateUpdated(state: Boolean?)
```

### SteeringWheelAngleListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/steering-wheel-angle-listener/entry)

```text
fun onSteeringWheelAngleUpdated(angle: Float?)
```

### TelltaleInfoListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/telltale-info-listener/entry)

```text
fun onTelltaleInfoUpdated(telltales: Telltales?)
```

### TemperatureDisplayUnitListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/temperature-display-unit-listener/entry)

```text
fun onTemperatureDisplayUnitUpdated(unit: TemperatureUnit?)
```

### TripDrivingDistanceListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/trip-driving-distance-listener/entry)

```text
fun onTripDrivingDistanceUpdated(statistics: TripStatistics<Float>?)
```

### TripDrivingTimeListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/trip-driving-time-listener/entry)

```text
fun onTripDrivingTimeUpdated(statistics: TripStatistics<Int>?)
```

### TripEvEfficiencyListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/trip-ev-efficiency-listener/entry)

```text
fun onTripEvEfficiencyUpdated(statistics: TripStatistics<Float>?)
```

### TripFuelEconomyListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/trip-fuel-economy-listener/entry)

```text
fun onTripFuelEconomyUpdated(statistics: TripStatistics<Float>?)
```

### TurnSignalStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/turn-signal-state-listener/entry)

```text
fun onTurnSignalStateUpdated(state: TurnSignalValue?)
```

### VehicleListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/vehicle-listener/entry)

```text
fun onFailed(e: Exception)
```

### WindowLockStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/window-lock-state-listener/entry)

```text
fun onWindowLockStateUpdated(state: Boolean?)
```

### WindowPositionListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/window-position-listener/entry)

```text
fun onWindowPositionUpdated(position: Int?)
```

### WiperStateListener

[공식 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/wiper-state-listener/entry)

```text
fun onWiperStateUpdated(state: WiperState?)
```

## 미정 사항과 확인 필요 사항

실제 계정 승인, 차종별 지원 영역·허용 값·주행 중 제한과 실차 동작은 미검증이다. 공개 문서·배포 선언에서 확인하지 못한 입력 의미·기본값·동작 보장은 추정하지 않는다. SDK 선택과 구체적인 제품 기능은 미정이며 개요 문서의 제공 상태·제약을 함께 확인한다.

## 관련 문서

| 문서 역할 | 문서 | 참조 목적 및 적용 범위 | 관련 요구사항 ID |
| --- | --- | --- | --- |
| 상위 개요 | [제공 API](../2001_API_연동_참조.md) | 버전·사용 가능 상태·공통 계약·해석 주의 사항 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [차량 서비스 연결 API](2101_차량_서비스_연결_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [브레이크 API](2102_브레이크_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [차량 식별 API](2103_차량_식별_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [표시 단위 API](2104_표시_단위_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [도어 API](2105_도어_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [주행 모드 API](2106_주행_모드_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [전기차 배터리와 충전 API](2107_전기차_배터리와_충전_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [연료와 연비 API](2108_연료와_연비_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [공조 API](2109_공조_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [조명 API](2110_조명_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [주행거리와 시간 API](2111_주행거리와_시간_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [안전과 경고 API](2112_안전과_경고_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [시트 API](2113_시트_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [사이드미러 API](2114_사이드미러_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [스티어링 API](2115_스티어링_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [타이어 API](2116_타이어_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [하이패스 API](2117_하이패스_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [방향지시등 API](2118_방향지시등_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [창문과 스마트글라스 API](2119_창문과_스마트글라스_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
| API 선언 | [와이퍼 API](2120_와이퍼_API.md) | 이 타입을 사용하는 메서드·입출력 확인 | REQ-CONTEST-001, REQ-CONTEST-009 |
