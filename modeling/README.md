# 📌 Notisook: 추천 시스템

---

## 1. 프로젝트 개요
분산된 공지사항 환경에서 사용자 관심 기반으로 정보를 추천하는 시스템입니다.  
서비스 UX 개선과 함께, 데이터가 제한적인 환경에서의 추천 시스템 설계를 목표로 했습니다.

---

## 2. 문제 정의
<img width="894" height="487" alt="image" src="https://github.com/user-attachments/assets/9995db7c-87a0-47ac-b4be-6704a3080eb6" />

<img width="1063" height="566" alt="image" src="https://github.com/user-attachments/assets/8641f37d-4aaa-47fa-87c2-92b820046c28" />

- 공지사항 분산 → 탐색 비용 증가  
- 관심 없는 정보 과다 → 중요 정보 누락  
- 평균 2~3개 사이트 방문 필요  

---

## 3. 접근 방법

### ✔ Hybrid 구조 (협업 + 콘텐츠 병렬 추천)

- 협업 필터링 (BPR)
- 콘텐츠 기반 추천 (TF-IDF)

→ 데이터 sparsity 환경에서 협업 필터링 단독 사용의 한계를 보완

---

## 4. 데이터 구성

### 📊 사용자 행동 데이터 정의

| 데이터 | 설명 | 사용 목적 |
|------|------|------|
| viewedPosts | 사용자가 조회한 공지 | BPR 학습 |
| scrappedPosts | 사용자가 저장한 공지 | BPR 학습 (강한 선호 신호) |

👉 조회(view)는 약한 관심 신호, 스크랩(scrap)은 강한 관심 신호로 해석하여  
**implicit feedback 기반 추천 모델에 함께 반영**

---

### 📊 데이터 통계 (실험 기준)

| 항목 | 값 |
|------|------|
| 사용자 수 | 약 80명 |
| 전체 공지 수 | 약 1,200개 |
| 총 상호작용 수 | 약 3,500건 |
| 사용자당 평균 상호작용 수 | 약 10~15개 |
| 밀도 (interaction ratio) | 약 0.036 |

---

## 5. 데이터 처리 및 전처리
<img width="1050" height="445" alt="image" src="https://github.com/user-attachments/assets/78be4c80-20a5-4a6b-aa84-bdc442b3e0ee" />

- 형태소 분석 (Kiwi)
- 명사 추출
- TF-IDF 벡터화
- 사용자-아이템 매핑 (Sparse Matrix)

👉 viewed + scrapped 데이터를 통합하여 interaction matrix 생성

---

## 6. 모델링

### 6.1 협업 필터링 (BPR)

#### ✔ 모델 선택 이유

- 명시적 평점 데이터 없음
- implicit feedback (조회, 스크랩)만 존재

→ ranking 기반 모델이 적합

---

#### ✔ 학습 방식

- Positive: 사용자가 조회 또는 스크랩한 공지
- Negative: 상호작용하지 않은 공지

👉 BPR 학습 목표:

“사용자가 상호작용한 공지 > 상호작용하지 않은 공지”

---

#### ✔ 모델 설정

implicit.bpr.BayesianPersonalizedRanking(
    factors=10,
    iterations=30
)

---

### 6.2 콘텐츠 기반 추천

- TF-IDF 벡터화
- cosine similarity
<img width="656" height="362" alt="image" src="https://github.com/user-attachments/assets/19745295-3297-4f57-90c8-d7feb173b190" />

→ 최근 스크랩 공지를 기준으로 유사 공지 추천

---

## 7. 추천 전략

BPR 추천 → 콘텐츠 추천 → 병합  
→ 기존 조회/스크랩 제거  
→ 최신성 기반 정렬  
→ Top-N 반환  

---

## 8. 실험 방법

- 대상: 최소 1개 이상 상호작용(viewed 또는 scrapped)이 존재하는 사용자
- 방식: Leave-One-Out

👉 일부 상호작용 데이터를 제거한 후 추천 결과에 포함되는지 평가

---

## 9. 실험 결과

| Metric | Value |
|------|------|
| Precision@5 | ≈ 0.6 |

---

## 10. 결과 해석

- 공지 특성상 관심 카테고리가 명확한 사용자에서 높은 precision 발생
- viewed + scrapped를 함께 활용하여 sparse 환경에서도 추천 성능 확보
- 콘텐츠 기반 추천을 통해 cold start 문제 보완

---

## 11. 한계 및 개선 방향

### ⚠ 한계
- 사용자 수 및 데이터 규모 제한
- 행동 데이터 종류 제한 (클릭, 체류시간 없음)
- 인기 공지 편향 존재

### 🚀 개선 방향
- 체류 시간, 클릭 로그 등 추가 행동 데이터 반영
- 시간 가중치 기반 추천
- sequence 기반 모델 (RNN, Transformer) 적용
