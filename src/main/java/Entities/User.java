package Entities;

import java.time.LocalDateTime;

public class User {
    private Long userId;
    private String username;
    private String password;
    private String salt;
    private String profile;
    private String role;
    private String studyPurpose;
    private Integer literacy;
    private LocalDateTime lastLiteracy;
    private Integer experience;
    private LocalDateTime lastCheckin;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    public User() {}

    public User(Long userId, String username, String password, String salt,
                String profile, String role, String studyPurpose,
                Integer literacy, LocalDateTime lastLiteracy,
                Integer experience, LocalDateTime lastCheckin,
                LocalDateTime createdAt, LocalDateTime lastLogin) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.salt = salt;
        this.profile = profile;
        this.role = role;
        this.studyPurpose = studyPurpose;
        this.literacy = literacy;
        this.lastLiteracy = lastLiteracy;
        this.experience = experience;
        this.lastCheckin = lastCheckin;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStudyPurpose() { return studyPurpose; }
    public void setStudyPurpose(String studyPurpose) { this.studyPurpose = studyPurpose; }

    public Integer getLiteracy() { return literacy; }
    public void setLiteracy(Integer literacy) { this.literacy = literacy; }

    public LocalDateTime getLastLiteracy() { return lastLiteracy; }
    public void setLastLiteracy(LocalDateTime lastLiteracy) { this.lastLiteracy = lastLiteracy; }

    public Integer getExperience() { return experience; }
    public void setExperience(Integer experience) { this.experience = experience; }

    private Integer cefrProgress;
    public Integer getCefrProgress() { return cefrProgress; }
    public void setCefrProgress(Integer cefrProgress) { this.cefrProgress = cefrProgress; }

    public LocalDateTime getLastCheckin() { return lastCheckin; }
    public void setLastCheckin(LocalDateTime lastCheckin) { this.lastCheckin = lastCheckin; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
}
