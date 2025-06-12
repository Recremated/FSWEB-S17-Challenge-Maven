package com.workintech.spring17challenge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workintech.spring17challenge.exceptions.ApiResponseError;
import com.workintech.spring17challenge.exceptions.ApiException;
import com.workintech.spring17challenge.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Spring17challengeApplication.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(ResultAnalyzer.class)
class MainTest {

    @Autowired
    private Environment env;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Course course;
    private static boolean isFirstTest = true;

    @BeforeEach
    void setUp() throws Exception {
        // Reset courses before each test
        mockMvc.perform(post("/courses/test/reset"))
                .andExpect(status().isOk());

        // Setup a sample Course object
        course = new Course();
        course.setId(1);
        course.setName("Introduction to Spring");
        course.setCredit(3);
        course.setGrade(new Grade(1, "A"));

        mockMvc.perform(post("/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(course)))
                .andExpect(status().isCreated());
    }

    /**
     * Helper method to check if a course exists
     */
    private boolean courseExists(String courseName) throws Exception {
        try {
            MvcResult result = mockMvc.perform(get("/courses/{name}", courseName))
                    .andReturn();
            return result.getResponse().getStatus() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Helper method to clear all courses
     * This assumes you have access to all courses via GET /courses
     */
    private void clearAllCourses() throws Exception {
        // Get all courses first
        MvcResult result = mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Course[] courses = objectMapper.readValue(response, Course[].class);

        // Delete each course
        for (Course existingCourse : courses) {
            mockMvc.perform(delete("/courses/{id}", existingCourse.getId()))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("application properties istenilenler eklendi mi?")
    void serverPortIsSetTo8585() {
        String serverPort = env.getProperty("server.port");
        assertThat(serverPort).isEqualTo("9000");

        String contextPath = env.getProperty("server.servlet.context-path");
        assertNotNull(contextPath);
        assertThat(contextPath).isEqualTo("/workintech");
    }

    @Test
    void testHandleApiException() throws Exception {
        String nonExistingName = "testCourseName";

        mockMvc.perform(get("/courses/{name}", nonExistingName))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void testCreateCourseValidationFailure() throws Exception {
        Course invalidCourse = new Course(null, null, null, null);

        mockMvc.perform(post("/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCourse)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    @Order(1)
    void testCreateCourse() throws Exception {
        // Create a different course to avoid duplicate name issue
        Course newCourse = new Course();
        newCourse.setName("Advanced Java Programming");
        newCourse.setCredit(4);
        newCourse.setGrade(new Grade(2, "B"));

        mockMvc.perform(post("/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCourse)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.course.name").value("Advanced Java Programming"))
                .andExpect(jsonPath("$.course.credit").value(4));
    }

    @Test
    @Order(2)
    void testGetAllCourses() throws Exception {
        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @Order(3)
    void testGetCourseByName() throws Exception {
        String courseName = course.getName();
        mockMvc.perform(get("/courses/{name}", courseName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(courseName)));
    }

    @Test
    @Order(4)
    void testUpdateCourse() throws Exception {
        // First, let's find a course to update
        MvcResult result = mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Course[] courses = objectMapper.readValue(response, Course[].class);

        if (courses.length > 0) {
            Course courseToUpdate = courses[0];
            courseToUpdate.setName("Updated Course Name");

            mockMvc.perform(put("/courses/{id}", courseToUpdate.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(courseToUpdate)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.course.name").value("Updated Course Name"));
        }
    }

    @Test
    @Order(5)
    void testDeleteCourse() throws Exception {
        // First, let's find a course to delete
        MvcResult result = mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Course[] courses = objectMapper.readValue(response, Course[].class);

        if (courses.length > 0) {
            Course courseToDelete = courses[0];
            mockMvc.perform(delete("/courses/{id}", courseToDelete.getId()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Course deleted successfully"));
        }
    }

    @Test
    @DisplayName("Test Course Setters and Getters")
    void testCourseSettersAndGetters() {
        Course course = new Course();
        Grade grade = new Grade(5, "Excellent");

        Integer expectedId = 101;
        String expectedName = "Advanced Mathematics";
        Integer expectedCredit = 4;

        course.setId(expectedId);
        course.setName(expectedName);
        course.setCredit(expectedCredit);
        course.setGrade(grade);

        assertEquals(expectedId, course.getId());
        assertEquals(expectedName, course.getName());
        assertEquals(expectedCredit, course.getCredit());
        assertEquals(grade, course.getGrade());
    }

    @Test
    @DisplayName("Test Grade Creation and Getters")
    void testGradeCreationAndGetters() {
        Integer expectedCoefficient = 5;
        String expectedNote = "Excellent";
        Grade grade = new Grade(expectedCoefficient, expectedNote);

        assertEquals(expectedCoefficient, grade.getCoefficient());
        assertEquals(expectedNote, grade.getNote());
    }

    @Test
    @DisplayName("Test GetHighCourseGpa Returns Correct Value")
    void testGetGpaReturnsCorrectValue() {
        CourseGpa highCourseGpa = new HighCourseGpa();
        int gpa = highCourseGpa.getGpa();

        assertEquals(10, gpa, "HighCourseGpa should return a GPA of 10");
    }

    @Test
    @DisplayName("Test GetLowCourseGpa Returns Correct Value")
    void getGpa() {
        CourseGpa lowCourseGpa = new LowCourseGpa();
        assertEquals(3, lowCourseGpa.getGpa(), "LowCourseGpa should return a GPA of 3");
    }

    @Test
    @DisplayName("Test MediumGpa Returns Correct Value")
    void testMediumGpaReturnsCorrectValue() {
        CourseGpa mediumCourseGpa = new MediumCourseGpa();
        int gpa = mediumCourseGpa.getGpa();

        assertEquals(5, gpa, "MediumCourseGpa should return a GPA of 5");
    }

    @Test
    @DisplayName("Test ApiErrorResponse Fields")
    void testApiErrorResponseFields() {
        Integer expectedStatus = 404;
        String expectedMessage = "Not Found";
        Long expectedTimestamp = System.currentTimeMillis();

        ApiResponseError errorResponse = new ApiResponseError(expectedMessage, expectedStatus, expectedTimestamp);

        assertEquals(expectedStatus, errorResponse.getStatus(), "The status should match the expected value.");
        assertEquals(expectedMessage, errorResponse.getMessage(), "The message should match the expected value.");
        assertEquals(expectedTimestamp, errorResponse.getTimestamp(), "The timestamp should match the expected value.");
    }

    @Test
    void testZooExceptionCreation() {
        String expectedMessage = "Test exception message";
        HttpStatus expectedStatus = HttpStatus.NOT_FOUND;

        ApiException exception = new ApiException(expectedMessage, expectedStatus);

        assertEquals(expectedMessage, exception.getMessage(), "The exception message should match the expected value.");
        assertEquals(expectedStatus, exception.getHttpStatus(), "The HttpStatus should match the expected value.");
        assertTrue(exception instanceof RuntimeException, "ApiException should be an instance of RuntimeException.");
    }

    @Test
    void testHttpStatusSetter() {
        ApiException exception = new ApiException("Initial message", HttpStatus.OK);
        exception.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus(), "The HttpStatus should be updatable and match the new value.");
    }
}