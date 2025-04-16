from selenium import webdriver
from selenium.webdriver.chrome.service import Service as Service
from webdriver_manager.chrome import ChromeDriverManager as DriverManager
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException
from bs4 import BeautifulSoup
import requests
import time
from datetime import datetime, timedelta, timezone
import random
import re
from notice_to_firebase import save_to_firebase, get_latest_date
from extract_keywords import extract_keywords

#본문 전처리 - 순수 한글만 남기기
def make_clean_text(text):
    #한글, 공백을 제외한 모든 것을 빈칸으로 치환
    text_only_korean = re.sub(r'[^가-힣\s]', ' ', text)

    #이모지 제거
    emoji_pattern = re.compile("["
        u"\U0001F600-\U0001F64F"  # 이모티콘
        u"\U0001F300-\U0001F5FF"  # 기호 및 아이콘
        u"\U0001F680-\U0001F6FF"  # 운송 및 지도 기호
        u"\U0001F1E0-\U0001F1FF"  # flags (iOS)
        "]+", flags=re.UNICODE)
    cleaned_text = emoji_pattern.sub(r' ', text_only_korean)

    #한 글자 단어 제거 및 공백 정리
    cleaned_text = re.sub(r'\b[가-힣]\b', '', cleaned_text)
    cleaned_text = re.sub(r'\s+', ' ', cleaned_text).strip()
    return cleaned_text

#업로드 날짜 datetime 타입으로 변환
#time_format = '%Y년 %m월 %d일 %p %I시 %M분 %S초 UTC+9'
def modify_timestamp(timestamp_str, korea_tz):
    today = datetime.now(korea_tz)
    if timestamp_str == '방금':
        timestamp = today
    elif re.match(r'^\d+분 전$', timestamp_str):
        minutes_ago = int(re.search(r'(\d+)분 전', timestamp_str).group(1))
        timestamp = today - timedelta(minutes=minutes_ago)
        timestamp = timestamp.replace(second=0, microsecond=0)
    elif re.match(r'^\d{2}/\d{2} \d{2}:\d{2}$', timestamp_str):
        month, day, time_part = re.split(r'[ /]', timestamp_str)
        hour, minute = map(int, time_part.split(':'))
        timestamp = datetime(today.year, int(month), int(day), hour, minute, tzinfo=korea_tz)
    elif re.match(r'^\d{2}/\d{2}/\d{2} \d{2}:\d{2}$', timestamp_str):
        timestamp = datetime.strptime(timestamp_str, '%y/%m/%d %H:%M')
        timestamp = korea_tz.localize(timestamp)

    # 변환된 날짜를 '%Y년 %m월 %d일 %p %I시 %M분 %S초 UTC+9' 형태로 반환
    return timestamp


#사이트 접속
def login_everytime(driver):
    # 접속
    rand_value = random.uniform(2,4)
    time.sleep(rand_value)
    driver.get('https://account.everytime.kr/login')

    # 로그인
    rand_value = random.uniform(2,4)
    time.sleep(rand_value)
    driver.find_element(By.NAME, 'id').send_keys('')
    time.sleep(5)
    driver.find_element(By.NAME, 'password').send_keys('')
    time.sleep(20)
    driver.find_element(By.XPATH, '/html/body/div[1]/div/form/input').click()

#게시글 정보 크롤링
def crawl_everytime(base_url, board_name, board_url,
                    driver, latest_date, kiwi, korea_tz, site_name):
    posts = []
    flag = True #이미 저장한 공지인지 여부를 위한 flag
    board_page = 1
    post_num = 0
    while flag:
        #각 게시글 가져오기
        WebDriverWait(driver, 10).until(EC.presence_of_all_elements_located((By.TAG_NAME, 'article')))
        html = driver.page_source
        soup = BeautifulSoup(html, 'html.parser')
        contents = soup.find_all('article')

        #각 게시글 페이지로 이동
        for content in contents:
            if post_num > 10:  #for test
                flag = False
                break
            link = content.find('a').get('href')
            rand_value = random.uniform(1, 2)
            time.sleep(rand_value)
            notice_url = base_url + link
            driver.get(notice_url)
            #WebDriverWait(driver, 10).until(lambda d: d.execute_script('return document.readyState') == 'complete')
            WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.CSS_SELECTOR, 'p.large')))

            #각 게시글 정보 가져오기
            sub_html = driver.page_source
            sub_soup = BeautifulSoup(sub_html, 'html.parser')

            #제목
            title = sub_soup.find('h2', class_='large').get_text()
            clean_title = make_clean_text(title)
            #본문
            context = sub_soup.find('p', class_='large').get_text()
            #context = context.replace('\n', ' ').replace('\r', ' ')
            context = make_clean_text(context)
            #키워드
            keywords= extract_keywords(kiwi, clean_title+context)
            print(keywords)
            #업로드 날짜
            upload_time_str = sub_soup.find('time', class_='large').get_text().strip()
            upload_time = modify_timestamp(upload_time_str, korea_tz)
            #마감날짜
            #deadline = ?
            #글쓴이
            author = sub_soup.find('h3', class_='large').get_text()
            #공감 수
            #status = sub_soup.find('ul', class_='status left')
            #likes = status.find('li', class_='vote').get_text()
            #스크랩 수
            #scraps = status.find('li', class_='scrap').get_text()
            #댓글 수
            #comments = status.find('li', class_='comment').get_text()
            #첨부 이미지
            #images = [figure.find('img')['src'] for figure in sub_soup.find_all('figure', class_='attach')]

            if latest_date and upload_time <= latest_date:
                flag = False
                break

            #게시글 객체 추가
            posts.append({
                'title': title,
                'content': context,
                'writer': author,
                'uploadTime': upload_time,
                'deadline': None,
                'category':'',
                'scraps': 0,
                'views': 0,
                'keywords': keywords,
                'url': notice_url,
                'site': site_name,
                'board': board_name,
                "postType": 'root'
            })
            post_num+=1

        #다음 페이지로 넘어가기 (게시판)
        if flag:
            board_page += 1
            driver.get(board_url + '/p/' + str(board_page))
        else:
            break
        
    return posts

#메인 함수 -> 파이어베이스에 저장
def everytime_to_firebase(uid, driver, kiwi, db, korea_tz):
    #사이트명
    site_name = 'everytime'
    #크롤링할 목록
    base_url = 'https://everytime.kr'
    board_url_list = {'free':'129540',
             #'비밀':'255673',
             #'질문':'255793',
             #'졸업':'385948',
             #'새내기':'369473',
             'info':'258594',
             'promo':'367421',
             'club':'418763'
    }
    #로그인
    login_everytime(driver)

    #각 게시판 별 크롤링
    for board_name, board_num in board_url_list.items():
        #가장 최근 업로드 날짜
        latest_date = get_latest_date(uid, site_name, board_name, db).astimezone(korea_tz)
        print('everytime_latest_date1: ' + (str(latest_date) if latest_date is not None else 'None'))
        #latest_date =  datetime.fromtimestamp(latest_date.timestamp(), tz=timezone.utc)
        #print('everytime_latest_date2: ' + (str(latest_date) if latest_date is not None else 'None'))
        
        #각 게시판별 페이지로 이동
        board_url = base_url + '/' + board_num
        driver.get(board_url)
        posts = crawl_everytime(base_url, board_name
                        , board_url, driver
                        , latest_date, kiwi
                        , korea_tz, site_name)

        #파이어베이스에 저장
        if posts:
            save_to_firebase(uid, site_name, board_name, posts, db)
            print('저장 완료: ' + str(len(posts)))
        else:
            print('최근 업데이트 된 게시글이 없습니다.')


    
    
