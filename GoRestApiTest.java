package com.gorest.tests;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class GoRestApiTest {

    private final String BASE_URL = "https://gorest.co.in/public/v2";
    // IMPORTANT: Replace with your actual Bearer Token for testing
    private final String BEARER_TOKEN = "YOUR_TOKEN_HERE"; 
    private int userId;
    private int initialActiveCount;

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = BASE_URL;
    }

    /**
     * Task 1: GET Request - Fetch user details and validate status/active count
     */
    @Test(priority = 1)
    public void testFetchAndValidateUsers() {
        Response response = given()
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .get("/users");

        // Verify the status code is 200
        Assert.assertEquals(response.getStatusCode(), 200, "Status code mismatch!");

        // Validate that the response body contains the user ID, name, and email
        Assert.assertNotNull(response.jsonPath().get("[0].id"), "User ID missing");
        Assert.assertNotNull(response.jsonPath().get("[0].name"), "User Name missing");
        Assert.assertNotNull(response.jsonPath().get("[0].email"), "User Email missing");

        // Validate how many users have status = active
        List<String> statuses = response.jsonPath().getList("status");
        initialActiveCount = (int) statuses.stream().filter(s -> s.equals("active")).count();
        System.out.println("LOG: Initial active users on page 1 = " + initialActiveCount);
    }

    /**
     * Task 2: POST Request - Create a new user and verify with GET
     */
    @Test(priority = 2)
    public void testCreateNewUser() {
        String uniqueEmail = "tester_" + System.currentTimeMillis() + "@example.com";
        Map<String, String> payload = new HashMap<>();
        payload.put("name", "John Doe");
        payload.put("email", uniqueEmail);
        payload.put("gender", "male");
        payload.put("status", "inactive");

        // Send POST request
        Response postResponse = given()
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/users");

        // Verify the status code is 201
        Assert.assertEquals(postResponse.getStatusCode(), 201, "Failed to create user");
        userId = postResponse.jsonPath().getInt("id");

        // Validate response body matches the payload (status must be inactive)
        Assert.assertEquals(postResponse.jsonPath().getString("name"), payload.get("name"));
        Assert.assertEquals(postResponse.jsonPath().getString("status"), "inactive");

        // Validate the new user is valid by using GET request and comparing with POST response
        Response getResponse = given()
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .get("/users/" + userId);
        
        Assert.assertEquals(getResponse.jsonPath().getString("name"), postResponse.jsonPath().getString("name"), "GET name mismatch");
        Assert.assertEquals(getResponse.jsonPath().getString("email"), postResponse.jsonPath().getString("email"), "GET email mismatch");
    }

    /**
     * Task 3: PUT Request - Update user and compare active user counts
     */
    @Test(priority = 3, dependsOnMethods = "testCreateNewUser")
    public void testUpdateUserAndVerifyStatus() {
        Map<String, String> updatePayload = new HashMap<>();
        updatePayload.put("name", "John Updated");
        updatePayload.put("email", "updated_" + userId + "@example.com");
        updatePayload.put("status", "active");

        // Update the user's name and email
        Response putResponse = given()
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .put("/users/" + userId);

        // Verify the status code is 200
        Assert.assertEquals(putResponse.getStatusCode(), 200, "Update failed");

        // Validate the updated fields (status must be active)
        Assert.assertEquals(putResponse.jsonPath().getString("name"), "John Updated");
        Assert.assertEquals(putResponse.jsonPath().getString("status"), "active");

        // Validate GET /users?status=active to check if the new user is listed
        Response activeResponse = given()
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .queryParam("status", "active")
                .get("/users");

        List<Integer> activeUserIds = activeResponse.jsonPath().getList("id");
        Assert.assertTrue(activeUserIds.contains(userId), "User ID should be in the active list");

        // Compare number of users from Task 1
        int currentActiveCount = activeUserIds.size();
        System.out.println("LOG: Current active users count = " + currentActiveCount);
        System.out.println("LOG: Comparison with Task 1 - Previous: " + initialActiveCount + " | Current: " + currentActiveCount);
    }

    /**
     * Task 4: DELETE Request - Delete user and verify deletion
     */
    @Test(priority = 4, dependsOnMethods = "testUpdateUserAndVerifyStatus")
    public void testDeleteAndVerifyUser() {
        // Verify the status code is 204
        given()
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .delete("/users/" + userId)
                .then()
                .statusCode(204);

        // Attempt to fetch the user again and verify the status code is 404
        given()
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .get("/users/" + userId)
                .then()
                .statusCode(404);
    }
}