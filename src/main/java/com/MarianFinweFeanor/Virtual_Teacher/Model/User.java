//package com.MarianFinweFeanor.Virtual_Teacher.Model;
//
//import jakarta.persistence.*;
//
//import java.util.Objects;
//
//@Entity
//@Table(name = "users") // Use "users" instead of "user" because The user table name is conflicting
//// with the SQL reserved keyword USER.
//public class User {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "user_id")
//    private Long userId;
//
//    @Column(name = "email", nullable = false, length = 30) //do so for the rest of the names
//    private String email;
//
//    @Column(name ="password",nullable = false, length = 20)
//    private String password;
//
//    @Column(name="first_name",nullable = false, length = 30)
//    private String firstName;
//
//    @Column(name="last_name",nullable = false, length = 30)
//    private String lastName;
//
//    @Column(name="profile_picture",nullable = true, length = 40)
//    private String profilePicture;
//
//    @Enumerated(EnumType.STRING)  //  Stores Enum as a String
//    @Column(name="role",nullable = false, length = 20)
//    private UserRole role;
//
//    @Column(name="status",nullable = false, length = 20)
//    private String status;
//
//    //Getters and Setters
//    public Long getUserId() {
//        return userId;
//    }
//
//    public void setUserId(Long userId) {
//        this.userId = userId;
//    }
//
//    public String getEmail() {
//        return email;
//    }
//
//    public void setEmail(String email) {
//        this.email = email;
//    }
//
//    public String getPassword() {
//        return password;
//    }
//
//    public void setPassword(String password) {
//        this.password = password;
//    }
//
//    public String  getFirstName() {
//        return firstName;
//    }
//
//    public void setFirstName(String firstName) {
//        this.firstName = firstName;
//    }
//
//    public String getLastName() {
//        return lastName;
//    }
//
//    public void setLastName(String lastName) {
//        this.lastName = lastName;
//    }
//
//    public String getProfilePicture() {
//        return profilePicture;
//    }
//
//    public void setProfilePicture(String profilePicture) {
//        this.profilePicture = profilePicture;
//    }
//
//    public UserRole getRole() {
//        return role;
//    }
//
//    public void setRole(UserRole role) {
//        this.role = role;
//    }
//
//
//
//    public String getStatus() {
//        return status;
//    }
//
//    public void setStatus(String status) {
//        this.status = status;
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(userId);
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj) return true;
//        if (obj == null || getClass() != obj.getClass()) return false;
//        User user = (User) obj;
//        return userId == user.userId;
//    }
//}
package com.MarianFinweFeanor.Virtual_Teacher.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", nullable = false, length = 20, unique = true)
    private String email;

    @Column(name = "password", nullable = false, length = 20)
    private String password;

    @Column(name = "first_name", nullable = false, length = 20)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 20)
    private String lastName;

    @Column(name = "profile_picture", nullable = true, length = 40)
    private String profilePicture;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

//    @ManyToMany
//    @JsonIgnore
//    @JoinTable(
//            name = "user_courses",
//            joinColumns = @JoinColumn(name = "user_id"),
//            inverseJoinColumns = @JoinColumn(name = "course_id")
//    )


    // --- Enrollments owned by this student ---
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Enrollment> enrollments = new HashSet<>();

    // ---------- Helpers ----------
    /** Convenience: the set of Courses the user is enrolled in. */
    public Set<Course> getCourses() {
        return enrollments.stream()
                .map(Enrollment::getCourse)   // requires Enrollment#getCourse()
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;   // cast to User
        return userId != null && userId.equals(other.getUserId());
    }

    @Override
    public int hashCode() {
        return (userId != null ? userId.hashCode() : 0);
    }

}
