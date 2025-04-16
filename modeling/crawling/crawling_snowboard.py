from datetime import datetime, timedelta
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
from notice_to_firebase import save_to_firebase, get_latest_date
from extract_keywords import extract_keywords
from crawling_everytime import make_clean_text
from datetime import datetime, timedelta, timezone
import traceback
import pytz

# Chrome 옵션 설정
#chrome_options = Options()
#chrome_options.add_argument('--disable-gpu')
#chrome_options.page_load_strategy = 'eager'

# 크롤링 상태 변수: True이면 크롤링 진행, 로그인 실패 또는 오류 발생 시 False로 설정
crawl_status = True
snowboard_posts = []

#스노우보드 계정 가져오기
def get_sb_inform(uid, db):
    # users/{uid} 문서 경로
    user_ref = db.collection('users').document(uid)
    
    # 문서 가져오기
    user_doc = user_ref.get()

    if user_doc.exists:
        # sb_username과 sb_pw 필드 가져오기
        sb_username = user_doc.to_dict().get('step3Id')
        sb_pw = user_doc.to_dict().get('step3Password')
        if sb_username == None or sb_pw == None:
            return None
        return sb_username, sb_pw
    else:
        print(f"No such document for uid: {uid}")
        return None

def snowboard_to_firebase(uid, user_info, driver, kiwi, db, korea_tz):
    global crawl_status, snowboard_posts
    site_name='snowboard'
    userID, userPWD = user_info

    try:
        wait = WebDriverWait(driver, 10)

        # 로그인
        driver.get("https://snowboard.sookmyung.ac.kr/login/index.php")
        username_field = wait.until(EC.presence_of_element_located((By.ID, 'input-username')))
        password_field = wait.until(EC.presence_of_element_located((By.ID, 'input-password')))
        login_button = wait.until(EC.element_to_be_clickable((By.CSS_SELECTOR, ".btn.btn-login")))
        username_field.send_keys(userID)
        password_field.send_keys(userPWD)
        login_button.click()

        # 로그인 성공 여부 확인
        try:
            alert = driver.switch_to.alert
            print("SnowBoard 로그인 실패. Alert 메시지:", alert.text)
            crawl_status = False
            return
        except NoAlertPresentException:
            pass
        i=1
        # 크롤링 작업 수행
        latest_date = get_latest_date(uid, site_name, '', db)
        if latest_date:
            latest_date = latest_date.astimezone(korea_tz)
            
        while crawl_status:    
            driver.get(f'https://snowboard.sookmyung.ac.kr/local/ubnotification/index.php?page={i}')
            flag = get_notice_data(driver, wait, kiwi, korea_tz, latest_date, uid, site_name)
            i+=1
            if not flag:
                crawl_status=False
                break
        
    except Exception as e:
        print(f"SnowBoard 에러 발생: {e}")
        traceback.print_exc()
        crawl_status = False

    finally:
        # 예외 발생 시나 크롤링 완료 후, 데이터를 저장
        if snowboard_posts:
            save_to_firebase(uid, site_name, '', snowboard_posts, db)
        print(snowboard_posts)
        snowboard_posts.clear()
        crawl_status=True
        
def parse_date(date_str):
    if not date_str:
        return None
    
    seoul_tz = pytz.timezone('Asia/Seoul')  # UTC+9 시간대
    
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
                return seoul_tz.localize(dt)
            except OverflowError:
                print(f"Warning: Date value out of range, returning None: {date_str}")
                return None
        except ValueError:
            continue
    
    print(f"Warning: Unable to parse date string, returning None: {date_str}")
    return None

def get_notice_data(driver, wait, kiwi, korea_tz, latest_date, uid, site_name):
    global snowboard_posts
    flag = True
    notices = wait.until(EC.presence_of_all_elements_located((By.CSS_SELECTOR, "div.card.card-body.p-0 a")))

    for notice in notices:
        notice_url = notice.get_attribute('href')
        media_body = notice.find_element(By.CSS_SELECTOR, '.media-body')
        course_name = media_body.find_element(By.CSS_SELECTOR, 'h5.media-heading').text.strip()
        title = media_body.find_elements(By.CSS_SELECTOR, 'p')[-1].text.strip()

        today = datetime.now(korea_tz)
        upload_text = media_body.find_element(By.CSS_SELECTOR, '.timeago').text.strip()
        if "일전" in upload_text:
            # "n일전"인 경우 앞의 숫자를 추출해서 날짜 계산
            days_ago = int(upload_text.replace("일전", "").strip())
            upload_date = today - timedelta(days=days_ago)
        else:
            upload_date = today

        # Firebase에 저장할 수 있는 timestamp로 변환 (UTC 기준으로 변환)
        upload_date = upload_date.astimezone(pytz.utc)


        print(notice_url)
        if 'ubfile' not in notice_url:  # '새 파일 등록'은 제외
            driver.get(notice_url)
            
            try: 
                if 'xncommons' in notice_url:
                    category = '강의'
                    deadline_text = wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, '.card.card-body'))).text
                    deadline_match = re.search(r'(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})(?!.*\d{4}-\d{2}-\d{2})', deadline_text)
                    deadline = deadline_match.group(1)
                    deadline = parse_date(deadline)
                elif 'ubboard' in notice_url:
                    category = '공지사항'
                    deadline = None
                elif 'assign' in notice_url:
                    category = '과제'
                    deadline_text = wait.until(EC.presence_of_element_located((By.XPATH, "//th[contains(text(), '종료 일시')]/following-sibling::td")))
                    deadline = deadline_text.text.strip()
                    deadline = parse_date(deadline)
                    
                elif 'quiz' in notice_url:
                    category = '퀴즈'
                    deadline_text = wait.until(EC.presence_of_element_located((By.XPATH, "//div[contains(@class, 'quizinfo')]//p[contains(text(), '종료일시')]"))).text
                    deadline_match = re.search(r'(\d{4}-\d{2}-\d{2} \d{2}:\d{2})', deadline_text)
                    deadline = deadline_match.group(1)
                    deadline = parse_date(deadline)
                else:
                    category = '기타'
                    deadline = None
                print(deadline)
                print(notice_url)
                snowboard_posts.append({
                    "site": site_name,
                    "category": course_name,
                    "board" : category,
                    "uploadTime" : upload_date,
                    "deadline" : deadline,
                    "title": title,
                    "url": notice_url,
                    "postType": uid
                })    
                driver.back()
            except Exception as e:
                print(e)
                continue

    return flag

