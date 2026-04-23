## Virtual Teacher - Udemy-Like Learning Platform

Virtual Teacher is an - WIP - online learning platform where students can **enroll in courses**, and teachers can **create, manage, and teach** online lessons. The project is built using **Java Spring Boot** for the backend, with **RESTful APIs** for seamless communication.

---

## Swagger Link
http://localhost:8080/swagger-ui/index.html#/
##

##  Features

✅ **User Authentication & Roles**  
   - Students, Teachers, and Admins with different permissions.  
   - Secure login and registration using JWT authentication.  

✅ **Course Management**  
   - Teachers can create, edit, and delete courses.  
   - Students can browse and enroll in available courses.  
   - Admins can manage all courses.  

✅ **Lesson & Content Delivery**  
   - Video lectures, assignments, and quizzes.  
   - Support for various file formats (PDFs, videos, and text).  

✅ **RESTful API with Spring Boot**  
   - Well-structured API endpoints for interaction.  
   - Secure API authentication and authorization.  

✅ **Database Integration**  
   - Uses PostgreSQL for efficient data storage.  

✅ **Frontend Integration Ready**  
   - The backend is designed to work with **Bootstrap (for now)**.  

✅ **Deployment Ready**  
   - Can be deployed on cloud services like AWS, Azure, or Heroku.  

---

## 🏗️ Tech Stack

| **Technology** | **Usage** |
|---------------|----------|
| Java & Spring Boot | Backend Development |
| Spring Security & JWT | Authentication & Authorization |
| PostgreSQL | Database Management |
| Hibernate | ORM for Java |
| Maven | Project Management |
| RESTful API | Communication between frontend and backend |
| Postman | API Testing |
| Git & GitHub | Version Control |

---

## 🛠️ Installation & Setup
```bash

### 1️⃣ Clone the Repository

git clone https://github.com/yourusername/virtual-teacher.git
cd virtual-teacher

### **2️⃣ Set Up Database**

Ensure you have PostgreSQL installed.
Create a database called virtual_teacher_db.
Update application.properties with your database credentials:

spring.datasource.url=jdbc:postgresql://localhost:8080/virtual_teacher_db
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password

3️⃣ Run the Application
Using Maven Wrapper:
mvn spring-boot:run

4️⃣ API Testing with Postman
You can use Postman or cURL to test API endpoints.

Example: Register a New User

curl -X POST "http://localhost:8080/api/auth/register" \
-H "Content-Type: application/json" \
-d '{"username": "student123", "password": "password", "role": "STUDENT"}'


📌 API Endpoints
HTTP Method	Endpoint	Description	Access
POST	/api/auth/register	Register a new user	Public
POST	/api/auth/login	Login and get JWT token	Public
GET	/api/courses	Get list of available courses	Public
POST	/api/courses	Create a new course	Teacher/Admin
GET	/api/courses/{id}	Get details of a course	Student/Teacher
POST	/api/courses/{id}/enroll	Enroll in a course	Student

 Future Improvements
✅ Video streaming support
✅ Payment integration (Stripe, PayPal, etc.)
✅ Discussion forums for students & teachers
✅ Live Q&A sessions and webinars

📜 License
This project is open-source and available under the MIT License.

