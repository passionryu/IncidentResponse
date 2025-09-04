### Project Missions
* 개발자의 개입 없는 자동 장애 대응 시스템 구축 
* 하나의 인스턴스가 장애가 발생해도 유저에게 지장 없는 고가용성 확보 
* 시스템 아키텍처 레이어에서의 효율성 보장
* 어플리케이션에 들어가기 전 DB/Server에 대한 과부하 방어 DB/Server 사전 보호
* 어플레케이션에 들어와서 DB에 들어가기 전 과부하 방어를 통한 DB 보호

### 시스템 아키텍처 
<img width="805" height="599" alt="image" src="https://github.com/user-attachments/assets/a35ea2a1-5f76-45e8-aa14-d86e15b97ca7" />

### 1. Nginx 
* HTTPS : 로컬에서 또한 Https 적용
* API Rate Limit : 어플리케이션 레이어에 들어가기 이전 부터, API에 대한 Rate Limit 정책을 걸어 DB/Server에 대한 과부하 사전 방어 
* Server Health Check : Server 중 장애가 있을 경우, Nginx 레이어에서 인식 
* Load Balance : Least Connection 정책을 따라 Server에 로드 벨런싱을 시도함

### 2. Spring Boot Server
* 동일한 Image 3개로 띄어진 3개의 서버에 대한 트래픽 분산 
* Docker Swarm + Prometheus : Prometheus의 Alert로 인한 Server Auto Scaling

### 3. Main DB 
* Primary-Replica : 고가용성 보장
* Patroni : 장애 자동 대응 시스템 (인스턴스 자체 장애 시, 자동 Restart 로직 추가)
* Read - Write 분리 : Read 부하 분산을 통한 성능 보장

### 4. HAProxy + RabbitMQ(Quorum Queue) 
* HAProxy : 연결 레벨 Failover 보장.
* RabbitMQ Quorum Queue → 데이터 복제 + 메시지 레벨 고가용성 보장.

RabbitMQ는 Quorum Queue관계 (Redis의 Primary-Replica와는 약간 다른)
-> 인스턴스끼리 자동 합의 알고리즘 내장 
* 큐 시스템을 통한 트레픽 안정성 보장 

### 5. Valkey Session Server 
* Primary - Replica : 고가용성 보장
* Read-Write 분리 : Read 부하 분산을 통한 성능 보장
* Sentinel: 장애 자동 대응 시스템 (Sentinel 인스턴스 자체 장애 시, 자동 Restart 로직 추가) 
  
### 6. Valkey Cache Server 
 * Read-Write 분리 : Read 부하 분산을 통한 성능 보장
 * Primary - Replica : 고가용성 보장
 * Sarding : 캐싱 서버 성능 향상
 * HAProxy + Valkey Router : Valkey 인스턴스에 대한 라우팅을 담당하는 인스턴스(여기에 장애가 일어나면 캐싱 서버에 라우티이 안되니 HaProxy로 고가용성 보장)
