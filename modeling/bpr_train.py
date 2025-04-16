import implicit
import joblib
from bpr_preprocess import preprocess, get_user_data, get_post_data
import logging

def train_model():
    # 데이터 수집
    get_user_data()
    get_post_data()
    
    # 전처리된 데이터 로드
    user_data, post_data, interaction_matrix, user_map, post_map = preprocess()

    # logging 설정을 통해 출력 수준을 WARNING으로 설정하여 진행 상황 출력 비활성화
    logging.getLogger("implicit").setLevel(logging.WARNING)

    # BPR 모델 생성 및 학습
    model = implicit.bpr.BayesianPersonalizedRanking(factors=10, iterations=30)
    model.fit(interaction_matrix)

    # 모델 저장
    joblib.dump(model, 'G:/SchoolService/api/bpr_model.pkl')
    print("모델이 성공적으로 저장되었습니다.")

# 모델을 학습하고 저장
#if __name__ == '__main__':
#    train_model()

