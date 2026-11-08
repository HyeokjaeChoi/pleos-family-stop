# 이동 상태 미확인은 주행 중으로 취급하고, 재추천은 요청 시에만

Pleos SDK에서 주행/정차를 직접 주는 값을 찾지 못했다. 에뮬레이터에서 `DrivingModeStateListener`와 NaviHelper 주행 정보를 실측해 판별 근거를 확정하기 전까지는 '미확인 = 주행 중과 같은 제한'(음성 입력·확인·취소만)을 유지하고, 위치 변화로 속도를 직접 계산해 판별하지 않는다. 같은 주의 분산 이유로 이동 중 자동 재추천은 하지 않고 사용자가 요청할 때만(음성 또는 정차 후 화면) 재추천한다.

실측(#2, `docs/agents/emulator.md` §6): `DrivingModeStateListener`는 NORMAL/ECO/SPORT 모드라 이동 판별에 쓸 수 없다. 판별 근거는 Vehicle SDK `GearSelectionListener`(`CAR_DRIVE`)로 확정한다: `GEAR_PARK` = 정차, 그 외 기어와 미확인(`GEAR_UNKNOWN`, 콜백 없음, 오류) = 주행 중이다. 이는 '미확인 = 주행 중' 결정과 같은 방향이다. 속도는 Vehicle SDK에 없고 AAOS `CarDrivingStateService`(0 정차/1 공회전/2 이동)에서만 확인했다.
