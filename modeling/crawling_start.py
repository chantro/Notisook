from selenium import webdriver
from selenium.webdriver.chrome.service import Service as Service
from webdriver_manager.chrome import ChromeDriverManager as DriverManager
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException
from notice_to_firebase import initialize_firebase
from kiwipiepy import Kiwi
from crawling_everytime import everytime_to_firebase
from crawling_snoway import snoway_to_firebase
from crawling_snowe import snowe_to_firebase
from crawling_snowboard import snowboard_to_firebase, get_sb_inform
import pytz
import warnings
from datetime import datetime, timezone
import sys

# UserWarning 경고 무시
warnings.filterwarnings('ignore', category=UserWarning)

def get_driver():
    # 가상 브라우저 사용
    driver = webdriver.Chrome(service=Service(DriverManager().install()))
    driver.implicitly_wait(10)

    return driver

def start_crawling():
    #파이어베이스 객체 가져오기
    db = initialize_firebase('notisook-24488-firebase-adminsdk-2anqm-cc4480df38.json')

    #키워드 추출을 위한 kiwi 객체 가져오기
    kiwi = Kiwi()

    #한국 시간대로 설정
    korea_tz = pytz.timezone('Asia/Seoul')

    #각 사이트별 크롤링
    sites = ['snoway','snowe', 'snowboard']
    base_func_name = '_to_firebase'
    for site in sites:
        func_name = site + base_func_name
        if site == 'snowboard':
            users_ref = db.collection('users')
            user_docs = users_ref.stream()
            for user in user_docs:
                uid = user.id
                # 사용자 정보 가져오기
                user_info = get_sb_inform(uid, db)

                # user_info가 None인 경우 예외 처리
                if user_info is None:
                    print(f"User information for uid {uid} not found or incomplete.")
                    continue
                
                driver = get_driver()
                globals()[func_name](uid, user_info, driver, kiwi, db, korea_tz)
                driver.quit()
        else:
            driver = get_driver()
            globals()[func_name]('', driver, kiwi, db, korea_tz)
            driver.quit()

    #크롤링 완료 시각 저장
    log_ref = db.collection('server').document('log')
    log_ref.set({
       'latest_crawling': datetime.now(korea_tz)
    })

if __name__ == '__main__':
    start_crawling()
