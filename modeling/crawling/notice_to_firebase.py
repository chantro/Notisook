import firebase_admin
from firebase_admin import credentials, firestore
import re

# Firebase 초기화 (Firebase 서비스 계정 키로 초기화)
def initialize_firebase(credential_path):
    cred = credentials.Certificate(credential_path)
    firebase_admin.initialize_app(cred)
    return firestore.client()

# 데이터 저장 함수
def save_to_firebase(uid, site_name, board_name, posts, db):
    # Firestore에서 저장할 경로 설정
    #collection_ref = db.collection(f'sites/{site_name}/{board_name}')
    print(uid)
    if site_name == 'snowboard':
        collection_ref = db.collection('users').document(uid).collection('posts')
    else:
        collection_ref = db.collection('posts')

    # Firestore 트랜잭션 사용
    with db.transaction() as transaction:
        for post_data in posts:
            if site_name == 'snowboard':
                try:
                    transaction.set(collection_ref.document(extract_url(post_data['url'])), post_data)  # 문서 생성
                    print("데이터가 정상적으로 저장되었습니다.")
                except Exception as e:
                    print(f"Firestore 저장 중 오류 발생: {e}")
            else:
                try:
                    transaction.set(collection_ref.document(), post_data)  # 문서 생성
                    print("데이터가 정상적으로 저장되었습니다.")
                except Exception as e:
                    print(f"Firestore 저장 중 오류 발생: {e}")

def extract_url(text):
    # 정규식을 사용해 ? 다음의 모든 텍스트를 추출
    match = re.search(r'\?(.*)', text)
    if match:
        return match.group(1).strip()  # 결과를 반환
    return None  # ?가 없으면 None 반환

# Firestore에서 가장 최신 게시글의 업로드 날짜 가져오는 함수
def get_latest_date(uid, site_name, board_name, db):
    #collection_path = f'sites/{site_name}/{board_name}'
    if site_name == 'snowboard':
        collection_path = f'users/{uid}/posts'
        posts_ref = db.collection(collection_path)
        query = posts_ref \
            .order_by('uploadTime', direction=firestore.Query.DESCENDING) \
            .limit(1)
    else:
        collection_path = 'posts'
        posts_ref = db.collection(collection_path)
        #site와 board명을 기준으로 필터링, uploadTime을 기준으로 내림차순 정렬
        query = posts_ref.where('site', '==', site_name) \
            .where('board', '==', board_name) \
            .order_by('uploadTime', direction=firestore.Query.DESCENDING) \
            .limit(1)

    #쿼리 실행
    latest_post = query.stream()
    
    
    # 최신 게시글 하나만 가져옴 (upload_date 기준으로 내림차순 정렬)
    #latest_post = posts_ref.order_by('uploadTime', direction=firestore.Query.DESCENDING).limit(1).stream()

    for post in latest_post:
        return post.to_dict()['uploadTime']
    
    # Firestore에 게시글이 없으면 None 반환
    return None


# Firestore posts 컬렉션 초기화
def remove_all(db):
    posts_ref = db.collection('posts')
    docs = posts_ref.stream()

    # 모든 문서 삭제
    for doc in docs:
        doc.reference.delete()
