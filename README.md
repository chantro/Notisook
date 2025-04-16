# 📢 Notisook - Integrated & Personalized University Announcements

**Notisook** is a unified platform that gathers scattered university announcements from multiple sources and delivers them in one place, enriched with a personalized recommendation system to save students' time and help them never miss important information.

---

## 🔍 Key Features

### 1️⃣ All-in-One Announcement Viewer
- Aggregates posts from Snowy, Snoway, Snowboard, and Everytime.
- Displays all announcements in a unified and intuitive interface.

### 2️⃣ Personalized Recommendation Engine
- Recommends announcements based on user interests and activity.
- Utilizes TF-IDF and BPR algorithms with real user interaction data.
- Sends push notifications via FCM for newly recommended posts.

### 3️⃣ Save & Calendar Integration
- Users can save ("scrap") important announcements.
- Integrate saved announcements with their Google Calendar.

### 4️⃣ Keyword-Based Search & Exploration
- Extracts keywords using NLP (KIWIPY) and TF-IDF.
- Allows users to explore related posts by clicking on keywords.
- Full-text search across all announcement data.

### 5️⃣ Campus Benefits Organizer
- Centralizes easy-to-miss campus benefits like locker applications, gifticon claims, and more.

---

## 🛠️ Tech Stack

| Category        | Technology                          |
|-----------------|--------------------------------------|
| Frontend        | Kotlin (Android)                    |
| Backend         | Java, Python                        |
| Cloud Platform  | Google Cloud Platform (GCP)         |
| Database        | Firebase Firestore                  |

---

## 🔐 Authentication & Security

- Firebase Authentication for secure login.
- JWT & custom token issuance with user role binding.
- Role-based access control (RBAC) at the client level.

### 🔐 Flow
![image](https://github.com/user-attachments/assets/05e0504f-8f17-454a-ba47-f064ff67a4a9)
1. Client logs in via Firebase Auth.
2. JWT is sent to the GCP server.
3. Server assigns a role based on email and generates a custom token.
4. Client updates Firebase Auth with the custom token.
5. Role is used for secure access control.

---

## 🧠 Recommendation System Details
![image-2](https://github.com/user-attachments/assets/45c8e025-4d37-4d19-b9d9-f18976bc5d56)
### 📌 Data Collection & Preprocessing
- Crawling announcements using **BS4** and **Selenium** every 20 minutes.
- Extracting nouns via **KIWIPY** and generating TF-IDF keyword vectors.
- All data is stored in **Firestore**.

### 📌 Recommendation Algorithms

#### (1) User-Based Collaborative Filtering (BPR)
- Builds a user-post matrix based on scrap/click behavior.
- Applies **Bayesian Personalized Ranking (BPR)** to generate top 5 personalized recommendations.

#### (2) Item-Based Content Filtering
- Compares similarity of announcements using TF-IDF vectors.
- Suggests top 4 similar posts based on user's saved announcements.
- Filters out already seen or scrapped content.

> All recommendations are stored in `users/{UID}/recommendPosts/`, and updated every 20 minutes.  
> If new recommendations are generated, Firebase Cloud Messaging is triggered.

---

## 💬 UI
![image-3](https://github.com/user-attachments/assets/137084e0-daa5-464f-9262-252da8c32619)
![image-4](https://github.com/user-attachments/assets/04c8ca5c-fddb-48d7-ab8a-e7a834879f34)
![image-5](https://github.com/user-attachments/assets/60a1606a-8e65-4845-819d-b8ea34655595)
![image-6](https://github.com/user-attachments/assets/767eb45d-6cb6-41e1-a4cc-ebcb52df1404)
![image-7](https://github.com/user-attachments/assets/3b827b53-a3c0-4133-a5a2-410c978f31ef)
---

## 👥 Team Information

| Name         | Role                  | GitHub ID                    |
|--------------|-----------------------|------------------------------|
| Cheayeon Kim | Frontend Developer    | https://github.com/wfs0502   |
| Siyoung Kimm | Frontend Developer    | https://github.com/hyunvely8 |
| Suhyun Lee   | Modeling & Connecting | https://github.com/chantro   |

> Feel free to contact us or contribute through GitHub issues and pull requests!


