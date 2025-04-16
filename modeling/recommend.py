# recommend.py
import joblib
from bpr_preprocess import preprocess
import sys
import firebase_admin
from firebase_admin import credentials, firestore
from bpr_train import train_model

def recommend_posts(user_id, model, interaction_matrix, user_data, post_data, user_map, post_map, n=5):
    # 사용자 인덱스 찾기
    user_idx = user_map[user_id]

    # 사용자가 스크랩하거나 본 게시글 가져오기
    excluded_posts = user_data.loc[user_data['userID'] == user_id, 'scrappedPosts'].values[0].union(
                      user_data.loc[user_data['userID'] == user_id, 'viewedPosts'].values[0])

    #print(f"Excluded posts: {excluded_posts}")

    # 모든 게시글에 대한 점수 예측
    post_ids, scores = model.recommend(user_idx, interaction_matrix[user_idx], N=len(post_map), filter_already_liked_items=False)
    
    # post_map에서 post_ids를 실제 postID로 매핑하고, 제외된 게시글은 제외
    recommended_posts = []
    for post_idx in post_ids:
        actual_post_id = list(post_map.keys())[post_idx]
        if actual_post_id not in excluded_posts:
            recommended_posts.append(actual_post_id)

    #print(f"Recommended post IDs: {recommended_posts}")

    # 먼저 추천 게시글에서 상위 n개 추출
    top_n_posts = recommended_posts[:n]

    # 상위 n개의 추천 게시글을 uploadTime 기준으로 정렬
    if top_n_posts:
        top_n_posts_df = post_data[post_data['postID'].isin(top_n_posts)]
        # uploadTime 기준으로 정렬
        top_n_posts_df = top_n_posts_df.sort_values(by='uploadTime', ascending=False)
        return top_n_posts_df['postID'].tolist()
    else:
        return []  # 빈 리스트 반환

def get_db():
    # Firebase 앱이 이미 초기화되어 있는지 확인
    if not firebase_admin._apps:
        # Firebase 서비스 계정 키 파일 경로 설정
        cred = credentials.Certificate('notisook-24488-firebase-adminsdk-2anqm-cc4480df38.json')
        firebase_admin.initialize_app(cred)

    # Firestore 클라이언트 초기화
    db = firestore.client()

    return db

def save_to_firebase(filtered_posts, user_id, db):
    # Firestore의 해당 경로 지정 (예: /users/recommendPosts/CF)
    doc_ref = db.collection('users').document(user_id).collection('recommendPosts').document('user_similar')

    # 배열을 필드에 추가
    doc_ref.set({
    'items': filtered_posts
    }, merge=True)
    
    
if __name__ == '__main__':
    db = get_db()
    #훈련
    train_model()
    
    # 전처리된 데이터 및 저장된 모델 로드
    user_data, post_data, interaction_matrix, user_map, post_map = preprocess()
    model = joblib.load('G:/SchoolService/api/bpr_model.pkl')

    # 특정 사용자에게 추천 생성
    user_id = ''  # 추천을 원하는 사용자 ID
    #user_id = sys.argv[1]
    filtered_posts = recommend_posts(user_id, model, interaction_matrix, user_data, post_data, user_map, post_map)
    save_to_firebase(filtered_posts, user_id, db)
    print(filtered_posts)
