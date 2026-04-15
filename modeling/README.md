# 📌 Notisook: 통합 공지사항 추천 시스템 (Portfolio + Research Version)

---

## 1. 프로젝트 개요
분산된 공지사항 환경에서 사용자 관심 기반으로 정보를 추천하는 시스템입니다.  
서비스 관점의 UX 개선과 추천 시스템 연구 관점을 동시에 반영했습니다.

---

## 2. 문제 정의

<img width="894" height="487" alt="image" src="https://github.com/user-attachments/assets/9995db7c-87a0-47ac-b4be-6704a3080eb6" />

<img width="1063" height="566" alt="image" src="https://github.com/user-attachments/assets/8641f37d-4aaa-47fa-87c2-92b820046c28" />


- 공지사항 분산 → 탐색 비용 증가  
- 관심 없는 정보 과다 → 중요 정보 누락  
- 평균 2~3개 사이트 방문 필요  

---

## 3. 접근 방법

### ✔ Hybrid-like 구조 (병렬 추천)
- 협업 필터링 (BPR)
- 콘텐츠 기반 (TF-IDF)

→ 서로 보완하는 구조

---

## 4. 데이터 처리 및 전처리

<img width="1050" height="445" alt="image" src="https://github.com/user-attachments/assets/78be4c80-20a5-4a6b-aa84-bdc442b3e0ee" />

- 형태소 분석 (Kiwi)
- 명사 추출
- TF-IDF 벡터화
- 사용자-아이템 매핑

---

## 5. 모델링

### 5.1 협업 필터링
- implicit feedback 기반
- ranking optimization

### 5.2 콘텐츠 기반
<img width="656" height="362" alt="image" src="https://github.com/user-attachments/assets/19745295-3297-4f57-90c8-d7feb173b190" />

- cosine similarity
- 텍스트 의미 기반 유사도

---

## 6. 추천 전략

- 병렬 추천 수행
- 기존 조회 데이터 제거
- 최신성 기반 정렬

---

## 7. 실험 결과

### 📊 주요 성능

| Metric | Value |
|------|------|
| Precision@5 | ≈ 0.6 |
| 방식 | Top-N 추천 |

---

## 8. 결과 해석

- 높은 recall → 다양한 후보 확보
- precision 향상 → 추천 품질 개선
- 최신성 반영 → 실제 UX 개선
