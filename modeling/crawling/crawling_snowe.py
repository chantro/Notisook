import pandas as pd
#import schedule
import time
import re
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import NoAlertPresentException
from selenium.common.exceptions import TimeoutException
from selenium.common.exceptions import StaleElementReferenceException
from datetime import datetime, timezone
from notice_to_firebase import save_to_firebase, get_latest_date
from extract_keywords import extract_keywords
from crawling_everytime import make_clean_text
import pytz

# Chrome 옵션 설정
#chrome_options = Options()
#chrome_options.add_argument('--disable-gpu')
#chrome_options.page_load_strategy = 'eager'

# 크롤링 상태 변수: True이면 크롤링 진행, 로그인 실패 또는 오류 발생 시 False로 설정
crawl_status = True
snowe_posts = []

def snowe_to_firebase(uid, driver, kiwi, db, korea_tz):
    global crawl_status, snoway_posts
    USERNAME = ''
    PASSWORD = ''
    site_name = 'snowe'
    board_list = {
        'cmtyService': 'cmtyService',
        'internalJob': 'jobopening',
        'jobcareer': 'jobcareer',
        'notice': 'notice',
        'externalJob': 'eoNotice'
    }
    
    try:
        # 웹 드라이버 초기화
        #driver = webdriver.Chrome(options=chrome_options)
        wait = WebDriverWait(driver, 10)

        # 로그인
        driver.get("https://snowe.sookmyung.ac.kr/bbs5/users/login")
        username_field = wait.until(EC.presence_of_element_located((By.NAME, 'userId')))
        password_field = wait.until(EC.presence_of_element_located((By.NAME, 'userPassword')))
        login_button = wait.until(EC.element_to_be_clickable((By.ID, 'loginButton')))
        username_field.send_keys(USERNAME)
        password_field.send_keys(PASSWORD)
        login_button.click()

        # 로그인 성공 여부 확인
        try:
            alert = driver.switch_to.alert
            print("Snowe 로그인 실패. Alert 메시지:", alert.text)
            crawl_status = False
            return
        except NoAlertPresentException:
            pass
        
        # 각 게시판 별 크롤링 작업 수행
        for board_name, board_link in board_list.items():
            board_page = 1
            base_board_url = f'https://snowe.sookmyung.ac.kr/bbs5/boards/{board_link}#'
            latest_date = get_latest_date(uid, site_name, board_name, db).astimezone(korea_tz)
            print(latest_date)
            while crawl_status:    
                driver.get(base_board_url + str(board_page))
                time.sleep(2)
                flag = get_notice_data(driver, wait, kiwi, korea_tz, latest_date, site_name, board_name)
                if not flag: #똑같은 글이 나오면 crawling stop & 저장
                    if snowe_posts:
                        save_to_firebase(uid, site_name, board_name, snowe_posts, db)
                        print('저장 완료: ' + str(len(snowe_posts)))
                    else:
                        print('최근 업데이트 된 게시글이 없습니다.')
                    snowe_posts.clear()
                    break
                board_page += 1

    except Exception as e:
        print(f"Snowe 에러 발생: {e}")
        crawl_status = False

    #finally:
    #    # 예외 발생 시나 크롤링 완료 후, 데이터를 CSV 파일에 저장
    #    if df:
    #        pd.DataFrame(df).to_csv(csv_file_path, index=False)
    #    driver.quit()

def get_notice_data(driver, wait, kiwi, korea_tz, latest_date, site_name, board_name):
    global snowe_posts
    flag = True
    notices = wait.until(EC.presence_of_all_elements_located((By.CSS_SELECTOR, '#messageListBody tr[id^="tr_"] .title a')))
    notice_urls = [notice.get_attribute('href') for notice in notices]

    for notice_url in notice_urls:
        driver.get(notice_url)

        try:
            # 제목과 카테고리 요소 찾기    
            title_element = wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, '.titleWrap .title_head')))
            category_element = title_element.find_elements(By.CSS_SELECTOR, '.title_head')

            # 카테고리 처리
            category = category_element[0].text.strip().strip('[]') if category_element else ''
            full_title = title_element.text.strip()

            if category:
                title = full_title.replace(category_element[0].text, '').strip()
            else:
                title = full_title
            # 작성자, 업로드 시간, 마감일, 본문 내용 대기하여 추출
            writer = wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, '.titleWrap .post_info .writer'))).text.strip()
            upload_time = wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, '.titleWrap .post_info .date'))).text.strip()
            upload_time = modify_timestamp_snowe(upload_time, korea_tz)
            print('upload_time: ' + str(upload_time))
            #새로 업데이트된 글만 가져오기
            if latest_date and upload_time <= latest_date:
                flag=False
                break
            deadline_element = driver.find_elements(By.CSS_SELECTOR, '.titleWrap .period')
            deadline = deadline_element[0].text.strip().split('~')[-1].strip() if deadline_element else None
            deadline = parse_date(deadline, korea_tz)
            print('deadline: ' + str(deadline))
            context = wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, '#viewArticle #_ckeditorContents'))).text.strip()

            #키워드
            keywords= extract_keywords(kiwi, make_clean_text(title+ ' ' + context))
            print(keywords)
            print(category)

            snowe_posts.append({
                "title": title,
                "content": context,
                "writer": writer,
                "uploadTime": upload_time,
                "deadline": deadline,
                "category": category,
                "scraps": 0,
                "views": 0,
                "keywords": keywords,
                "url": notice_url,
                "site": site_name,
                "board": board_name,
                "postType": 'root'
            })

        except TimeoutException:
            print(f"TimeoutException url: {notice_url}")
            continue

    return flag

#업로드 날짜 datetime 타입으로 변환
def modify_timestamp_snowe(upload_time, korea_tz):
    upload_time = upload_time.replace('.', '-')
    upload_time = datetime.strptime(upload_time, "%Y-%m-%d %H:%M:%S")  # 문자열을 datetime으로 변환
    upload_time = korea_tz.localize(upload_time)

    return upload_time


def parse_date(date_str, korea_tz):
    if not date_str:
        return None
    
    #seoul_tz = pytz.timezone('Asia/Seoul')  # UTC+9 시간대
    
    formats = [
        '%Y-%m-%d %H:%M:%S',
        '%Y-%m-%d',
        '%Y.%m.%d %H:%M:%S',
        '%Y.%m.%d'
    ]
    
    for fmt in formats:
        try:
            dt = datetime.strptime(date_str, fmt)
            try:
                return korea_tz.localize(dt)
            except OverflowError:
                print(f"Warning: Date value out of range, returning None: {date_str}")
                return None
        except ValueError:
            continue
    
    print(f"Warning: Unable to parse date string, returning None: {date_str}")
    return None

