package com.workintech.spring17challenge.controller;

import com.workintech.spring17challenge.exceptions.ApiException;
import com.workintech.spring17challenge.model.Course;
import com.workintech.spring17challenge.model.HighCourseGpa;
import com.workintech.spring17challenge.model.LowCourseGpa;
import com.workintech.spring17challenge.model.MediumCourseGpa;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/courses")
@Slf4j
public class CourseController {

    @PostMapping("/test/reset")
    @Profile("test") // Only available in test profile
    public ResponseEntity<String> resetForTesting() {
        courses.clear();
        idCounter.set(1);
        return ResponseEntity.ok("Reset completed");
    }

    // Constants for credit validation
    private static final int MIN_CREDIT = 1;
    private static final int MAX_CREDIT = 4;
    private static final int LOW_CREDIT_THRESHOLD = 2;
    private static final int MEDIUM_CREDIT = 3;
    private static final int HIGH_CREDIT = 4;

    private final LowCourseGpa lowCourseGpa;
    private final MediumCourseGpa mediumCourseGpa;
    private final HighCourseGpa highCourseGpa;

    private List<Course> courses;
    private AtomicLong idCounter;

    public CourseController(LowCourseGpa lowCourseGpa, MediumCourseGpa mediumCourseGpa, HighCourseGpa highCourseGpa) {
        this.lowCourseGpa = lowCourseGpa;
        this.mediumCourseGpa = mediumCourseGpa;
        this.highCourseGpa = highCourseGpa;
        this.courses = new ArrayList<>();
        this.idCounter = new AtomicLong(1);
    }

    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        log.info("Getting all courses. Total count: {}", courses.size());
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/{name}")
    public ResponseEntity<Course> getCourseByName(@PathVariable String name) {
        log.info("Looking for course with name: {}", name);

        Course course = courses.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new ApiException("Course not found with name: " + name, HttpStatus.NOT_FOUND));

        return ResponseEntity.ok(course);
    }

    @PostMapping
    public ResponseEntity<CourseResponse> addCourse(@RequestBody Course course) {
        log.info("Adding new course: {}", course.getName());

        validateCourse(course, true);

        // ID assignment
        course.setId((int) idCounter.getAndIncrement());

        courses.add(course);
        int totalGpa = calculateTotalGpa(course);
        CourseResponse response = new CourseResponse(course, totalGpa);

        log.info("Course added successfully with ID: {}", course.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse(@PathVariable int id, @RequestBody Course updatedCourse) {
        log.info("Updating course with ID: {}", id);

        validateCourse(updatedCourse, false);

        Course course = courses.stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElseThrow(() -> new ApiException("Course not found with id: " + id, HttpStatus.NOT_FOUND));

        // Check for duplicate name with different ID
        boolean duplicateName = courses.stream()
                .anyMatch(c -> c.getId() != id && c.getName().equalsIgnoreCase(updatedCourse.getName()));
        if (duplicateName) {
            throw new ApiException("Another course with the same name exists.", HttpStatus.BAD_REQUEST);
        }

        // Update course properties
        course.setName(updatedCourse.getName());
        course.setCredit(updatedCourse.getCredit());
        course.setGrade(updatedCourse.getGrade());

        int totalGpa = calculateTotalGpa(course);
        CourseResponse response = new CourseResponse(course, totalGpa);

        log.info("Course updated successfully: {}", course.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCourse(@PathVariable int id) {
        log.info("Deleting course with ID: {}", id);

        boolean removed = courses.removeIf(c -> c.getId() == id);
        if (!removed) {
            throw new ApiException("Course not found with id: " + id, HttpStatus.NOT_FOUND);
        }

        log.info("Course deleted successfully with ID: {}", id);
        return ResponseEntity.ok("Course deleted successfully");
    }

    /**
     * Calculates total GPA based on course credit and grade coefficient
     * Formula: coefficient * credit * gpa_multiplier
     */
    private int calculateTotalGpa(Course course) {
        int credit = course.getCredit();
        int coefficient = course.getGrade().getCoefficient();

        if (credit <= LOW_CREDIT_THRESHOLD) {
            return coefficient * credit * lowCourseGpa.getGpa();
        } else if (credit == MEDIUM_CREDIT) {
            return coefficient * credit * mediumCourseGpa.getGpa();
        } else if (credit == HIGH_CREDIT) {
            return coefficient * credit * highCourseGpa.getGpa();
        }

        throw new ApiException("Invalid credit value: " + credit, HttpStatus.BAD_REQUEST);
    }

    /**
     * Validates course data
     * @param course The course to validate
     * @param isNew Whether this is a new course (for duplicate name checking)
     */
    private void validateCourse(Course course, boolean isNew) {
        // Validate credit range
        if (course.getCredit() == null || course.getCredit() < MIN_CREDIT || course.getCredit() > MAX_CREDIT) {
            throw new ApiException(String.format("Credit value must be between %d and %d.", MIN_CREDIT, MAX_CREDIT), HttpStatus.BAD_REQUEST);
        }

        // Validate course name
        if (course.getName() == null || course.getName().isBlank()) {
            throw new ApiException("Course name cannot be blank.", HttpStatus.BAD_REQUEST);
        }

        // Validate grade
        if (course.getGrade() == null) {
            throw new ApiException("Course grade cannot be null.", HttpStatus.BAD_REQUEST);
        }

        // Check for duplicate names only for new courses
        if (isNew) {
            boolean exists = courses.stream()
                    .anyMatch(c -> c.getName().equalsIgnoreCase(course.getName()));
            if (exists) {
                throw new ApiException("Course with the same name already exists.", HttpStatus.BAD_REQUEST);
            }
        }
    }

    /**
     * Response wrapper for course operations that include GPA calculation
     */
    public static class CourseResponse {
        private final Course course;
        private final int totalGpa;

        public CourseResponse(Course course, int totalGpa) {
            this.course = course;
            this.totalGpa = totalGpa;
        }

        public Course getCourse() {
            return course;
        }

        public int getTotalGpa() {
            return totalGpa;
        }

        @Override
        public String toString() {
            return String.format("CourseResponse{course=%s, totalGpa=%d}", course, totalGpa);
        }
    }
}