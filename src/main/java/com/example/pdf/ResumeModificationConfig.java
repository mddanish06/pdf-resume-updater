package com.example.pdf;

import java.util.*;

public class ResumeModificationConfig {
    
    private List<ExperienceEntry> newExperiences;
    private Map<String, String> skillModifications;
    private List<String> newCertifications;
    private Map<String, Object> customEdits;
    
    public ResumeModificationConfig() {
        newExperiences = new ArrayList<>();
        skillModifications = new HashMap<>();
        newCertifications = new ArrayList<>();
        customEdits = new HashMap<>();
    }
    
    // Builder pattern for easy configuration
    public static class Builder {
        private ResumeModificationConfig config;
        
        public Builder() {
            config = new ResumeModificationConfig();
        }
        
        public Builder addExperience(String title, String company, 
                                    String duration, String... bullets) {
            config.newExperiences.add(
                new ExperienceEntry(title, company, duration, 
                                   Arrays.asList(bullets))
            );
            return this;
        }
        
        public Builder modifySkill(String oldSkill, String newSkill) {
            config.skillModifications.put(oldSkill, newSkill);
            return this;
        }
        
        public Builder addCertification(String cert) {
            config.newCertifications.add(cert);
            return this;
        }
        
        public ResumeModificationConfig build() {
            return config;
        }
    }
    
    // Getters
    public List<ExperienceEntry> getNewExperiences() {
        return newExperiences;
    }
    
    public Map<String, String> getSkillModifications() {
        return skillModifications;
    }
    
    public List<String> getNewCertifications() {
        return newCertifications;
    }
    
    // Experience entry class
    public static class ExperienceEntry {
        private String jobTitle;
        private String company;
        private String duration;
        private List<String> responsibilities;
        
        public ExperienceEntry(String title, String comp, String dur, 
                              List<String> resp) {
            this.jobTitle = title;
            this.company = comp;
            this.duration = dur;
            this.responsibilities = resp;
        }
        
        // Getters
        public String getJobTitle() { return jobTitle; }
        public String getCompany() { return company; }
        public String getDuration() { return duration; }
        public List<String> getResponsibilities() { return responsibilities; }
        
        @Override
        public String toString() {
            return String.format("%s at %s (%s) - %d responsibilities", 
                               jobTitle, company, duration, 
                               responsibilities.size());
        }
    }
    
    // Example usage method
    public static ResumeModificationConfig getDefaultConfig() {
        return new Builder()
            .addExperience(
                "Full Stack Developer",
                "Innovative Solutions Ltd.",
                "June 2024 - Present",
                "Developed responsive web applications using React and Spring Boot",
                "Implemented authentication system with JWT and OAuth2",
                "Optimized SQL queries resulting in 50% faster page load times",
                "Collaborated with cross-functional teams using Agile methodology",
                "Deployed applications on AWS EC2 with auto-scaling capabilities"
            )
            .modifySkill("Java", "Java (Spring Boot, Hibernate, JPA)")
            .addCertification("Oracle Certified Java Programmer (2024)")
            .build();
    }
}