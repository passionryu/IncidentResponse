# Valkey Session Server

이 디렉토리는 고가용성 Valkey Session Server 클러스터를 구현합니다. 이 클러스터는 3개의 Valkey 인스턴스(1개 Primary + 2개 Replica)와 5개의 Sentinel 인스턴스로 구성되어 있습니다.

## 목차

- [아키텍처 개요](#아키텍처-개요)
- [포트 매핑](#포트-매핑)
- [시작하기](#시작하기)
- [연결 방법](#연결-방법)
- [모니터링 및 관리](#모니터링-및-관리)
- [고가용성 기능](#고가용성-기능)
- [보안 설정](#보안-설정)
- [성능 튜닝](#성능-튜닝)
- [문제 해결](#문제-해결)
- [모니터링 및 로그 확인](#모니터링-및-로그-확인)
- [확장성](#확장성)
- [백업 및 복구](#백업-및-복구)
- [참고사항](#참고사항)

## 아키텍처 개요

### Valkey Session Server 구성
- **Primary (valkey-session-primary)**: Read-Write 권한을 가진 마스터 인스턴스
- **Replica 1 (valkey-session-replica1)**: Read-Only 권한을 가진 복제본
- **Replica 2 (valkey-session-replica2)**: Read-Only 권한을 가진 복제본

### Sentinel 그룹 구성
- **5개의 Sentinel 인스턴스**: 자동 장애 대응을 위한 모니터링 및 페일오버 관리
- **Quorum**: 3개 (과반수 기반 의사결정)

## 포트 매핑

### Valkey Session Server
- Primary: `6379:6379`
- Replica 1: `6380:6379`
- Replica 2: `6381:6379`

### Sentinel
- Sentinel 1: `26379:26379`
- Sentinel 2: `26380:26379`
- Sentinel 3: `26381:26379`
- Sentinel 4: `26382:26379`
- Sentinel 5: `26383:26379`

## 시작하기

### 0. 사전 요구사항
이 서비스는 기존 `incident-net` 네트워크를 사용합니다. 먼저 application 서버들을 시작하여 네트워크를 생성해야 합니다:

```bash
cd ../application
docker-compose up -d
```

### 1. 서비스 시작
```bash
cd sessionServer
docker-compose up -d
```

### 2. 서비스 상태 확인
```bash
docker-compose ps
```

### 3. 로그 확인
```bash
# 전체 로그
docker-compose logs

# 특정 서비스 로그
docker-compose logs valkey-session-primary
docker-compose logs valkey-sentinel1
```

### 4. 서비스 중지
```bash
docker-compose down
```

### 5. 데이터와 함께 완전 삭제
```bash
docker-compose down -v
```

## 연결 방법

### Spring Boot 애플리케이션에서 연결

#### 1. Sentinel을 통한 연결 (권장)
```yaml
spring:
  data:
    valkey:
      sentinel:
        master: valkey-session-master
        nodes:
          - valkey-sentinel1:26379
          - valkey-sentinel2:26379
          - valkey-sentinel3:26379
          - valkey-sentinel4:26379
          - valkey-sentinel5:26379
        password: valkey_sentinel_password
      password: valkey_session_password
```

#### 2. 직접 연결 (개발/테스트용)
```yaml
spring:
  data:
    valkey:
      host: valkey-session-primary
      port: 6379
      password: valkey_session_password
```

### Redis CLI를 통한 연결

#### Primary에 직접 연결
```bash
docker exec -it valkey-session-primary valkey-cli -a valkey_session_password
```

#### Replica에 연결
```bash
docker exec -it valkey-session-replica1 valkey-cli -a valkey_session_password
```

#### Sentinel에 연결
```bash
docker exec -it valkey-sentinel1 valkey-cli -p 26379 -a valkey_sentinel_password
```

## 모니터링 및 관리

### Sentinel 정보 확인
```bash
# Sentinel 상태 확인
docker exec -it valkey-sentinel1 valkey-cli -p 26379 -a valkey_sentinel_password sentinel masters

# 마스터 정보 확인
docker exec -it valkey-sentinel1 valkey-cli -p 26379 -a valkey_sentinel_password sentinel master valkey-session-master

# 복제본 정보 확인
docker exec -it valkey-sentinel1 valkey-cli -p 26379 -a valkey_sentinel_password sentinel replicas valkey-session-master
```

### 복제 상태 확인
```bash
# Primary에서 복제 정보 확인
docker exec -it valkey-session-primary valkey-cli -a valkey_session_password info replication

# Replica에서 복제 정보 확인
docker exec -it valkey-session-replica1 valkey-cli -a valkey_session_password info replication
```

## 고가용성 기능

### 자동 페일오버
- Primary 장애 시 Sentinel이 자동으로 새로운 Primary를 선출
- 페일오버 시간: 약 10초 (설정 가능)
- Quorum 기반 의사결정으로 안정성 보장

### 읽기 부하 분산
- Primary: Read-Write 작업 처리
- Replica 1, 2: Read-Only 작업 처리
- 애플리케이션에서 읽기 작업을 Replica로 분산 가능

### 데이터 지속성
- RDB 스냅샷을 통한 데이터 백업
- 자동 저장 주기: 900초(15분), 300초(5분), 60초(1분)

## 보안 설정

### 인증
- Valkey Session Server 비밀번호: `valkey_session_password`
- Sentinel 비밀번호: `valkey_sentinel_password`

### 네트워크 보안
- 기존 `incident-net` 네트워크 사용
- application 서버들과 동일한 네트워크에서 통신

## 성능 튜닝

### 메모리 설정
- 각 인스턴스 최대 메모리: 256MB
- 메모리 정책: `allkeys-lru`

### 연결 설정
- TCP Keepalive: 300초
- 클라이언트 출력 버퍼 제한 설정

## 문제 해결

### 일반적인 문제들

1. **컨테이너 시작 실패**
   ```bash
   docker-compose logs [서비스명]
   ```

2. **복제 연결 실패**
   - 네트워크 연결 확인
   - 비밀번호 설정 확인
   - 방화벽 설정 확인

3. **Sentinel 모니터링 실패**
   - Primary 서비스 상태 확인
   - Sentinel 설정 파일 확인

### Sentinel 재시작 문제 해결

#### 문제 증상
- Sentinel 컨테이너들이 계속 재시작됨
- 로그에 "Can't resolve instance hostname" 오류 발생

#### 원인
- Sentinel이 시작할 때 Valkey 서버들의 호스트명을 해결할 수 없음
- Docker Compose의 `depends_on`은 컨테이너 시작 순서만 보장하고 실제 서비스 준비 상태를 기다리지 않음

#### 해결 방법
1. **대기 스크립트 사용**: `wait-for-valkey.sh`로 Valkey 서버 준비 대기
2. **IP 주소 직접 사용**: Sentinel 설정에서 호스트명 대신 IP 주소 사용
3. **의존성 설정 개선**: 스크립트 기반 시작 명령 사용

#### 적용된 수정사항
- Sentinel 설정 파일에서 `valkey-session-primary` → `172.20.0.6` 변경
- 모든 Sentinel에 대기 스크립트 적용
- Docker Compose에서 스크립트 기반 시작 명령 사용

### 헬스 체크
모든 서비스는 10초마다 헬스 체크를 수행합니다:
```bash
docker-compose ps
```

## 모니터링 및 로그 확인

### 전체 시스템 상태 확인
```bash
# 모든 Valkey 관련 컨테이너 상태 확인
docker ps --filter "name=valkey"

# Docker Compose 서비스 상태 확인
docker-compose ps

# 전체 시스템 리소스 사용량 확인
docker stats --filter "name=valkey"
```

### 로그 확인 명령어

#### 전체 로그
```bash
# 모든 서비스 로그 확인
docker-compose logs

# 실시간 로그 모니터링
docker-compose logs -f

# 특정 서비스 로그만 확인
docker-compose logs valkey-session-primary
docker-compose logs valkey-sentinel1
```

#### 개별 서비스 로그
```bash
# Primary 서버 로그
docker-compose logs valkey-session-primary --tail=50

# Replica 서버 로그
docker-compose logs valkey-session-replica1 --tail=50
docker-compose logs valkey-session-replica2 --tail=50

# Sentinel 로그
docker-compose logs valkey-sentinel1 --tail=50
docker-compose logs valkey-sentinel2 --tail=50
docker-compose logs valkey-sentinel3 --tail=50
docker-compose logs valkey-sentinel4 --tail=50
docker-compose logs valkey-sentinel5 --tail=50
```

### Valkey 서버 정보 확인

#### Primary 서버 정보
```bash
# Primary 서버에 직접 연결
docker exec -it valkey-session-primary valkey-cli -a valkey_session_password

# 복제 정보 확인
docker exec valkey-session-primary valkey-cli -a valkey_session_password info replication

# 메모리 사용량 확인
docker exec valkey-session-primary valkey-cli -a valkey_session_password info memory

# 클라이언트 연결 정보
docker exec valkey-session-primary valkey-cli -a valkey_session_password info clients

# 키스페이스 정보
docker exec valkey-session-primary valkey-cli -a valkey_session_password info keyspace
```

#### Replica 서버 정보
```bash
# Replica 1 복제 상태 확인
docker exec valkey-session-replica1 valkey-cli -a valkey_session_password info replication

# Replica 2 복제 상태 확인
docker exec valkey-session-replica2 valkey-cli -a valkey_session_password info replication

# Replica 서버들에 직접 연결
docker exec -it valkey-session-replica1 valkey-cli -a valkey_session_password
docker exec -it valkey-session-replica2 valkey-cli -a valkey_session_password
```

### Sentinel 모니터링 명령어

#### Sentinel 상태 확인
```bash
# Sentinel에 연결
docker exec -it valkey-sentinel1 valkey-cli -p 26379 -a valkey_sentinel_password

# 모니터링 중인 마스터 정보 확인
docker exec valkey-sentinel1 valkey-cli -p 26379 -a valkey_sentinel_password sentinel masters

# 특정 마스터의 상세 정보
docker exec valkey-sentinel1 valkey-cli -p 26379 -a valkey_sentinel_password sentinel master valkey-session-master

# 복제본 정보 확인
docker exec valkey-sentinel1 valkey-cli -p 26379 -a valkey_sentinel_password sentinel replicas valkey-session-master

# 다른 Sentinel들 정보 확인
docker exec valkey-sentinel1 valkey-cli -p 26379 -a valkey_sentinel_password sentinel sentinels valkey-session-master
```

#### Sentinel 정보 확인
```bash
# Sentinel 1 정보
docker exec valkey-sentinel1 valkey-cli -p 26379 -a valkey_sentinel_password info sentinel

# Sentinel 2 정보
docker exec valkey-sentinel2 valkey-cli -p 26379 -a valkey_sentinel_password info sentinel

# 모든 Sentinel 정보 확인
for i in {1..5}; do
  echo "=== Sentinel $i ==="
  docker exec valkey-sentinel$i valkey-cli -p 26379 -a valkey_sentinel_password info sentinel
done
```

### 네트워크 및 연결 테스트

#### 네트워크 연결 확인
```bash
# Primary 서버에서 다른 서버들 ping 테스트
docker exec valkey-session-primary ping -c 3 valkey-session-replica1
docker exec valkey-session-primary ping -c 3 valkey-sentinel1

# Sentinel에서 Primary 서버 연결 테스트
docker exec valkey-sentinel1 valkey-cli -h valkey-session-primary -p 6379 -a valkey_session_password ping
```

#### 포트 연결 테스트
```bash
# Primary 서버 포트 테스트
telnet localhost 6379

# Replica 서버 포트 테스트
telnet localhost 6380
telnet localhost 6381

# Sentinel 포트 테스트
telnet localhost 26379
telnet localhost 26380
telnet localhost 26381
telnet localhost 26382
telnet localhost 26383
```

### 성능 모니터링

#### 실시간 성능 모니터링
```bash
# 실시간 통계 확인
docker stats --filter "name=valkey" --no-stream

# 지속적인 모니터링 (5초마다 업데이트)
watch -n 5 'docker stats --filter "name=valkey" --no-stream'
```

#### 메모리 및 CPU 사용량
```bash
# 각 서비스별 상세 리소스 사용량
docker exec valkey-session-primary valkey-cli -a valkey_session_password info memory
docker exec valkey-session-primary valkey-cli -a valkey_session_password info stats
```

### 장애 대응 명령어

#### 서비스 재시작
```bash
# 특정 서비스 재시작
docker-compose restart valkey-session-primary
docker-compose restart valkey-sentinel1

# 모든 서비스 재시작
docker-compose restart

# 서비스 중지 후 재시작
docker-compose down && docker-compose up -d
```

#### 로그 레벨 변경
```bash
# 런타임에서 로그 레벨 변경 (Primary)
docker exec valkey-session-primary valkey-cli -a valkey_session_password config set loglevel debug

# 설정 파일에서 로그 레벨 변경 후 재시작
# config/valkey-primary.conf에서 loglevel 수정
docker-compose restart valkey-session-primary
```

## 확장성

### 수평 확장
- 추가 Replica 인스턴스 추가 가능
- Sentinel 인스턴스 추가 가능 (홀수 개 권장)

### 수직 확장
- 각 인스턴스의 메모리 제한 조정
- CPU 리소스 할당 조정

## 백업 및 복구

### 데이터 백업
```bash
# Primary 데이터 백업
docker exec valkey-session-primary valkey-cli -a valkey_session_password --rdb /data/backup.rdb
docker cp valkey-session-primary:/data/backup.rdb ./backup/
```

### 데이터 복구
```bash
# 백업 파일을 컨테이너에 복사
docker cp ./backup/backup.rdb valkey-session-primary:/data/dump.rdb
# 서비스 재시작
docker-compose restart valkey-session-primary
```

## 참고사항

- 이 설정은 개발 및 테스트 환경에 최적화되어 있습니다.
- 프로덕션 환경에서는 추가적인 보안 설정과 모니터링이 필요합니다.
- 로그 레벨과 저장 정책은 환경에 따라 조정할 수 있습니다.
