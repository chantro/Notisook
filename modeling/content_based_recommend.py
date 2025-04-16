import firebase_admin
from firebase_admin import credentials, firestore
from kiwipiepy import Kiwi
import re
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

# Firebase 초기화
cred = credentials.Certificate("notisook-24488-firebase-adminsdk-2anqm-cc4480df38.json")
firebase_admin.initialize_app(cred)
db = firestore.client()

# 전처리 함수
def preprocess(text):
    # 한글만 남기기
    hangul_text = re.sub(r'[^ㄱ-ㅎㅏ-ㅣ가-힣\s]', '', text)
    return hangul_text.strip()

# 형태소 분석기 초기화
kiwi = Kiwi()

def tokenize(text):
    # 형태소 분석 및 명사 추출 (NNG: 일반 명사, NNP: 고유 명사)
    tokens = kiwi.tokenize(text)  # 문장을 형태소 단위로 토크나이징
    nouns = [token.form for token in tokens if token.tag.startswith('NNG') or token.tag.startswith('NNP')]
    
    # 토큰이 없을 경우 빈 문자열 반환
    return ' '.join(nouns) if nouns else ''

# /users/{uid}/scrappedPosts에서 문서 ID 가져오기
def get_scrapped_posts_ids(uid):
    user_ref = db.collection('users').document(uid).collection('scrappedPosts')
    scrapped_docs = user_ref.stream()
    scrapped_ids = [doc.id for doc in scrapped_docs]
    return scrapped_ids

# /posts에서 문서 데이터 가져오기 (특정 문서들)
def get_posts_by_ids(post_ids):
    posts_ref = db.collection('posts')
    posts_data = {}
    for post_id in post_ids:
        doc = posts_ref.document(post_id).get()
        if doc.exists:
            posts_data[post_id] = doc.to_dict()
    return posts_data

# /posts에서 전체 문서 데이터 가져오기
def get_all_posts():
    posts_ref = db.collection('posts')
    all_posts = posts_ref.stream()
    posts_data = {}
    for doc in all_posts:
        posts_data[doc.id] = doc.to_dict()
    return posts_data

# 가장 최근 문서 찾기
def find_latest_post(posts):
    return max(posts.items(), key=lambda x: x[1]['uploadTime'])

# 유사한 문서 4개 찾기 (TF-IDF 기반, 자기 자신 제외)
def find_similar_posts_tfidf(target_post_id, target_post, all_posts, original_posts):
    # TF-IDF 벡터화 (정지어 필터링 해제)
    vectorizer = TfidfVectorizer(stop_words=None)
    corpus = [target_post] + list(all_posts.values())  # 비교할 모든 포스트의 내용을 포함
    
    # 비어 있는 문서 확인 및 제거
    corpus = [doc for doc in corpus if doc.strip()]
    if len(corpus) <= 1:
        raise ValueError("Not enough valid documents to compare.")
    
    # TF-IDF 행렬 계산
    tfidf_matrix = vectorizer.fit_transform(corpus)
    
    # 코사인 유사도 계산 (첫 번째 문서와 나머지 문서들 간의 유사도)
    cosine_similarities = cosine_similarity(tfidf_matrix[0:1], tfidf_matrix[1:]).flatten()
    
    # 유사도가 높은 순으로 정렬 후 자기 자신을 제외한 상위 4개 추출
    all_post_ids = list(all_posts.keys())
    
    # 유사도가 높은 순으로 정렬 (자기 자신을 제외)
    similar_indices = cosine_similarities.argsort()[::-1]
    
    # 자기 자신을 배제하고 유사한 상위 4개의 문서 추천 (ID와 원본 문서 데이터를 함께 반환)
    similar_posts = [(all_post_ids[i], original_posts[all_post_ids[i]]) for i in similar_indices if all_post_ids[i] != target_post_id][:4]
    
    return similar_posts

# 상위 4개의 post_id를 Firestore에 저장하는 함수
def save_recommended_posts(uid, post_ids):
    # Firestore의 해당 경로 지정 (예: /users/recommendPosts/CF)
    doc_ref = db.collection('users').document(uid).collection('recommendPosts').document('content_similar')

    # 배열을 필드에 추가
    doc_ref.set({
        'items': post_ids
    })


# 메인 함수
def recommend(uid):
    # 사용자의 스크랩된 문서 ID 가져오기
    scrapped_post_ids = get_scrapped_posts_ids(uid)
    
    # 스크랩된 문서 데이터 가져오기
    scrapped_posts_data = get_posts_by_ids(scrapped_post_ids)
    
    # 전체 posts 컬렉션의 모든 문서 가져오기
    all_posts_data = get_all_posts()

    # 모든 포스트에 대해 전처리 및 토큰화
    tokenized_posts = {}
    for post_id, post in all_posts_data.items():
        content = post['title'] + ' ' + post['content']
        preprocessed_content = preprocess(content)
        tokenized_posts[post_id] = tokenize(preprocessed_content)
    
    # 스크랩된 문서 중 가장 최근 문서 찾기
    latest_post_id, latest_post_data = find_latest_post(scrapped_posts_data)
    latest_post_content = tokenize(preprocess(latest_post_data['title'] + ' ' + latest_post_data['content']))
    
    # 가장 최근 문서가 비어있지 않으면 전체 posts 컬렉션에서 유사한 4개의 문서 찾기
    if latest_post_content.strip():  # 최근 문서가 비어있지 않으면 처리
        similar_posts = find_similar_posts_tfidf(latest_post_id, latest_post_content, tokenized_posts, all_posts_data)
        post_ids = [post_id for post_id, _ in similar_posts]  # 상위 4개의 post_id만 추출
        save_recommended_posts(uid, post_ids)  # 추천 문서 ID 저장
        print(f"추천 문서 IDs: {post_ids}")
    else:
        print("가장 최근 문서가 비어 있습니다.")

if __name__=='__main__':
    uid = 'mO6CjKpGLJNVbZmhnIJlLrtemY33'
    recommend(uid)
