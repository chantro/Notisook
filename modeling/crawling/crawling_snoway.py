import pandas as pd
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

# Chrome 옵션 설정
#chrome_options = Options()
#chrome_options.add_argument('--disable-gpu')
#chrome_options.page_load_strategy = 'eager'

# 크롤링 상태 변수: True이면 크롤링 진행, 로그인 실패 또는 오류 발생 시 False로 설정
crawl_status = True
snoway_posts = []


def snoway_to_firebase(uid, driver, kiwi, db, korea_tz):
    global crawl_status, snoway_posts
    USERNAME = ''
    PASSWORD = ''
    site_name = 'snoway'
    board_name = 'notice'

    try:
        # 웹 드라이버 초기화
        #driver = webdriver.Chrome(options=chrome_options)
        wait = WebDriverWait(driver, 10)

        # 로그인
        driver.get("https://snoway.sookmyung.ac.kr/login.jsp")
        username_field = wait.until(EC.presence_of_element_located((By.NAME, 'userid')))
        password_field = wait.until(EC.presence_of_element_located((By.NAME, 'userpw')))
        login_button = wait.until(EC.element_to_be_clickable((By.CSS_SELECTOR, "a.sp_login.btn")))
        username_field.send_keys(USERNAME)
        password_field.send_keys(PASSWORD)
        login_button.click()

        # 로그인 성공 여부 확인
        try:
            alert = driver.switch_to.alert
            print("Snoway 로그인 실패. Alert 메시지:", alert.text)
            crawl_status = False
            return
        except NoAlertPresentException:
            pass
        
        i=1
        ### 크롤링 작업 수행 ###
        #가장 최근 업로드 날짜
        latest_date = get_latest_date(uid, site_name, board_name, db).astimezone(korea_tz)
        while crawl_status:    
            driver.get(f'https://snoway.sookmyung.ac.kr/site/program/board/basicboard/list?boardtypeid=2&menuid=001007001&pagesize=10&currentpage={i}')
            flag = crawl_snoway(driver, wait, kiwi, korea_tz, latest_date, site_name, board_name)
            i+=1
            if not flag:
                break

    except Exception as e:
        print(f"Snoway 에러 발생: {e}")
        crawl_status = False

    finally:
        # 예외 발생 시나 크롤링 완료 후, 데이터를 CSV 파일에 저장
        if snoway_posts:
            save_to_firebase(uid, site_name, board_name, snoway_posts, db)
            print('저장 완료: ' + str(len(snoway_posts)))
        else:
            print('최근 업데이트 된 게시글이 없습니다.')

def crawl_snoway(driver, wait, kiwi, korea_tz, latest_date, site_name, board_name):
    global crawl_status
    global snoway_posts
    flag = True
    notices = wait.until(EC.presence_of_all_elements_located((By.CSS_SELECTOR, '.table_style1 tbody tr')))
    notice_info = []

    # url, category 저장
    for notice in notices:
        notice_link_element = notice.find_element(By.CSS_SELECTOR, 'td.left a')
        notice_url = notice_link_element.get_attribute('href')
        
        # 공지 카테고리 확인 (공지일 경우 "공지", 그 외는 "")
        if notice.find_elements(By.CSS_SELECTOR, 'span.noti'):
            category = "공지"
        else:
            category = ""
        
        notice_info.append((notice_url, category))

    # 각 공지사항 페이지에 접속하여 정보 추출
    for notice_url, category in notice_info:
        driver.get(notice_url)

        try:
            # 제목, 작성자, 등록일, 본문 추출
            title = wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, '.table_style2 thead .tit'))).text.strip()
            writer = wait.until(EC.presence_of_element_located((By.XPATH, "//span[contains(text(), '작성자')]/a"))).text.strip()
            date_element = wait.until(EC.presence_of_element_located((By.XPATH, "//span[contains(text(), '등록일')]")))
            upload_time = date_element.text.strip()

            if "등록일" in upload_time:
                upload_time= modify_timestamp_snoway(upload_time, korea_tz)

            #새로 업데이트된 글만 가져오기
            if latest_date and upload_time <= latest_date:
                flag=False
                break

            context = wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, '.table_style2 tbody'))).text.strip()

            #키워드
            keywords= extract_keywords(kiwi, make_clean_text(title+ ' ' + context))
            snoway_posts.append({
                "title": title,
                "content": context,
                "writer": writer,
                "uploadTime": upload_time,
                "deadline": None,
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
def modify_timestamp_snoway(date_text, korea_tz):
    date_str = date_text.split("등록일")[-1].strip()
    upload_time_str = f"{date_str} 00:00:00"  # 기본 시간을 '00:00:00'으로 설정
    upload_time = datetime.strptime(upload_time_str, "%Y-%m-%d %H:%M:%S")  # 문자열을 datetime으로 변환
    upload_time = korea_tz.localize(upload_time)

    return upload_time
