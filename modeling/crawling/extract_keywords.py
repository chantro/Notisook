from kiwipiepy import Kiwi
from sklearn.feature_extraction.text import TfidfVectorizer

def extract_keywords(kiwi, text, custom_dict=None, top_n=5):
    # 사용자 사전 추가
    #if custom_dict:
    #    for word, tag in custom_dict:
    #        kiwi.add_user_word(word, tag)
    
    # 형태소 분석 및 명사 추출(NNG: 일반 명사, NNP: 고유 명사)
    tokens = kiwi.tokenize(text)
    nouns = [token.form for token in tokens if token.tag.startswith('NNG') or token.tag.startswith('NNP')]
    
    # 명사를 하나의 문자열로 합침
    if not nouns:
        return []  # 명사가 없으면 빈 리스트 반환
    
    # 명사를 하나의 문자열로 합침
    processed_text = ' '.join(nouns)
    
    # TF-IDF 벡터라이저 설정
    vectorizer = TfidfVectorizer()
    
    try:
        tfidf_matrix = vectorizer.fit_transform([processed_text])
    except ValueError:
        return []  # 단어가 없을 경우 빈 리스트 반환
    
    # TF-IDF 점수를 기반으로 상위 키워드 추출
    scores = tfidf_matrix.toarray().flatten()
    feature_names = vectorizer.get_feature_names_out()
    
    sorted_indices = scores.argsort()[::-1]
    top_keywords = [feature_names[idx] for idx in sorted_indices[:top_n]]
    
    return top_keywords

