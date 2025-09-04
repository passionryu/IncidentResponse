# 장애 대응+ 고가용성 확보 + 효율성 확보 + 과부하 방지를 고려한 서버 아키텍처에 대한 연구

# 시스템 아키텍처 
<img width="805" height="599" alt="image" src="https://github.com/user-attachments/assets/a35ea2a1-5f76-45e8-aa14-d86e15b97ca7" />

### 1. Nginx 
* HTTPS
* API Rate Limit
* Server Health Check
* Load Balance

### 2. Spring Boot Server (Docker + Docker Swarm + Prometheus)
* 부하 분산 
* Prometheus의 Alert로 인한 Server Auto Scaling


### 3. Main DB (Primary-Replica,Read-Write 분리, Patroni) : 고가용성 + 성능 보장 

### 4. HAProxy + RabbitMQ(Quorum Queue) : 고가용성 + 안정성 보장 
* HAProxy → 연결 레벨 Failover 보장.
* RabbitMQ Quorum Queue → 데이터 복제 + 메시지 레벨 고가용성 보장.

RabbitMQ는 Quorum Queue관계 (Redis의 Primary-Replica와는 약간 다른)
-> 인스턴스끼리 자동 합의 알고리즘 내장 

### 5. Valkey Session Server (Primary - Replica,Read-Write 분리, Sentinel) : 고가용성 + 성능 보장 

### 6. Valkey Cache Server (Primary - Replica,Read-Write 분리,Sarding,HAProxy + Valkey Router): 고가용성 + 부하 분산 + 성능 보장  
