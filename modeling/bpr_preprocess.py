# preprocess.py
import pandas as pd
from scipy.sparse import csr_matrix
import firebase_admin
from firebase_admin import credentials, firestore
import pandas as pd

# Firebase Admin SDK 초기화
cred = credentials.Certificate('notisook-24488-firebase-adminsdk-2anqm-cc4480df38.json')
firebase_admin.initialize_app(cred)

# Firestore 클라이언트 가져오기
db = firestore.client()

def get_user_data():
    users_ref = db.collection('users')
    users = users_ref.stream()

    user_data_list = []

    for user in users:
        user_id = user.id
        print(f"Processing user: {user_id}")  # 사용자 ID 디버깅 출력

        # recommendPosts 하위 컬렉션에서 user_similar 문서의 items 필드 가져오기
        try:
            recommend_posts_ref = users_ref.document(user_id).collection('recommendPosts').document('user_similar')
            recommend_posts_doc = recommend_posts_ref.get()
            recommend_posts = recommend_posts_doc.to_dict().get('items', []) if recommend_posts_doc.exists else []
        except Exception as e:
            print(f"Error fetching recommendPosts for user {user_id}: {e}")
            recommend_posts = []

        # viewedPosts 하위 컬렉션에서 postID 가져오기 (없는 경우 빈 리스트로 처리)
        viewed_posts = []
        try:
            viewed_posts_ref = users_ref.document(user_id).collection('viewed')
            viewed_posts_docs = viewed_posts_ref.stream()
            for doc in viewed_posts_docs:
                post_id = doc.id  # 문서 ID가 postID인 경우
                viewed_posts.append(post_id)
        except Exception as e:
            print(f"Error fetching viewedPosts for user {user_id}: {e}")
            viewed_posts = []

        # scrappedPosts 하위 컬렉션에서 postID 가져오기
        scrapped_posts = []
        try:
            scrapped_posts_ref = users_ref.document(user_id).collection('scrappedPosts')
            scrapped_posts_docs = scrapped_posts_ref.stream()
            for doc in scrapped_posts_docs:
                post_id = doc.id  # 문서 ID가 postID인 경우
                scrapped_posts.append(post_id)
            print(f"scrappedPosts for user {user_id}: {scrapped_posts}")  # scrappedPosts 디버깅 출력
        except Exception as e:
            print(f"Error fetching scrappedPosts for user {user_id}: {e}")
            scrapped_posts = []

        # scrappedPosts에 데이터가 있는 사용자만 추가
        if scrapped_posts:  # scrappedPosts가 비어 있지 않으면 추가
            print(f"Adding user {user_id} with scrappedPosts: {scrapped_posts}")  # 사용자 추가 디버깅 출력
            user_data_list.append({
                'userID': user_id,
                'recommendPosts': ','.join(recommend_posts) if isinstance(recommend_posts, list) else recommend_posts,
                'viewedPosts': ','.join(viewed_posts) if isinstance(viewed_posts, list) else '',
                'scrappedPosts': ','.join(scrapped_posts)  # scrappedPosts가 반드시 존재
            })
        else:
            print(f"No scrappedPosts for user {user_id}, skipping...")  # scrappedPosts가 없는 경우

        # 사용자 처리 후 확인
        print(f"Finished processing user: {user_id}")

    print("All users processed. Creating CSV file...")
    
    # Pandas DataFrame 생성
    user_data_df = pd.DataFrame(user_data_list)

    # CSV 파일로 저장
    user_data_df.to_csv('userData.csv', index=False)
    print("CSV file created successfully.")
    
    return user_data_df

def get_post_data():
    users_ref = db.collection('users')
    posts_ref = db.collection('posts')

    # 모든 사용자 문서에서 postID를 중복 없이 수집
    post_ids = set()

    users = users_ref.stream()
    for user in users:
        user_id = user.id
        user_doc = user.to_dict()

        # 사용자의 recommendPosts 하위 컬렉션에서 postID 가져오기
        try:
            recommend_posts_ref = users_ref.document(user_id).collection('recommendPosts').document('user_similar')
            recommend_posts_doc = recommend_posts_ref.get()
            recommend_posts = recommend_posts_doc.to_dict().get('items', [])
            post_ids.update(recommend_posts)
        except:
            pass

        # 하위 컬렉션인 scrappedPosts에서 postID 가져오기
        try:
            scrapped_posts_ref = users_ref.document(user_id).collection('scrappedPosts')
            scrapped_posts_docs = scrapped_posts_ref.stream()
            for doc in scrapped_posts_docs:
                post_id = doc.id  # 하위 컬렉션 문서 ID가 postID
                post_ids.add(post_id)  # 중복 제거를 위해 Set에 추가
        except Exception as e:
            print(f"Error fetching scrappedPosts for user {user_id}: {e}")

        # viewedPosts 하위 컬렉션에서도 postID 가져오기
        try:
            viewed_posts_ref = users_ref.document(user_id).collection('viewedPosts')
            viewed_posts_docs = viewed_posts_ref.stream()
            for doc in viewed_posts_docs:
                post_id = doc.id  # 하위 컬렉션 문서 ID가 postID
                post_ids.add(post_id)
        except Exception as e:
            print(f"Error fetching viewedPosts for user {user_id}: {e}")


    # postID로 /posts 컬렉션에서 데이터 가져오기
    post_data_list = []

    for post_id in post_ids:
        post_doc = posts_ref.document(post_id).get()
        if post_doc.exists:
            post_data = post_doc.to_dict()
            upload_time = post_data.get('uploadTime', '')
            post_data_list.append({
                'postID': post_id,
                'uploadTime': upload_time
            })

    # Pandas DataFrame 생성
    post_data_df = pd.DataFrame(post_data_list)

    # CSV 파일로 저장
    post_data_df.to_csv('postData.csv', index=False)
    return post_data_df


def preprocess():
    # CSV 파일 불러오기
    user_data = pd.read_csv('G:/SchoolService/api/userData.csv')
    post_data = pd.read_csv('G:/SchoolService/api/postData.csv')

    # scrappedPosts와 viewedPosts를 리스트로 변환
    user_data['scrappedPosts'] = user_data['scrappedPosts'].apply(lambda x: set(x.split(',')))
    user_data['viewedPosts'] = user_data['viewedPosts'].apply(lambda x: set(x.split(',')) if isinstance(x, str) else set())

    # postData에서 uploadTime을 datetime 형태로 강제 변환
    post_data['uploadTime'] = pd.to_datetime(post_data['uploadTime'].str.replace(' KST', ''), errors='coerce')

    # datetime으로 변환되지 않은 값 처리 (예: NaT 값 제거)
    if post_data['uploadTime'].isnull().any():
        print("Some uploadTime values could not be converted to datetime and will be dropped.")
        post_data = post_data.dropna(subset=['uploadTime'])

    # 이미 시간대 정보가 있는 경우에는 tz_convert를 사용하고, 없는 경우 tz_localize 사용
    if post_data['uploadTime'].dt.tz is None:
        post_data['uploadTime'] = post_data['uploadTime'].dt.tz_localize('Asia/Seoul')
    else:
        post_data['uploadTime'] = post_data['uploadTime'].dt.tz_convert('Asia/Seoul')

    # 사용자와 게시글 ID 추출
    user_ids = user_data['userID'].tolist()
    post_ids = post_data['postID'].tolist()

    # 사용자와 게시글을 인덱스 맵핑
    user_map = {user_id: i for i, user_id in enumerate(user_ids)}
    post_map = {post_id: i for i, post_id in enumerate(post_ids)}

    rows, cols, data = [], [], []
    for i, row in user_data.iterrows():
        for post in row['scrappedPosts']:
            if post in post_map:
                rows.append(user_map[row['userID']])
                cols.append(post_map[post])
                data.append(1)

    # 희소 행렬 생성
    interaction_matrix = csr_matrix((data, (rows, cols)), shape=(len(user_ids), len(post_ids)))
    
    return user_data, post_data, interaction_matrix, user_map, post_map


