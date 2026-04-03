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
    private final String BEARER_TOKEN = "YOUR_TOKEN_HERE"; // Replace with actual token
    private int userId;
    private int initialActiveCount;
    private Map<String, String> postPayload;

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = BASE_URL;
    }

    /**
     * 1. GET Request
     * Fetch user details and validate status/active count
     */
    @Test(priority = 1)
    public void testGetRequest() {
        // Fetch list to count active users and get a valid ID for testing
        Response response = given()
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .get("/users");

        //  Verify the status code is 200
        Assert.assertEquals(response.getStatusCode(), 200);

        //  Validate that the response body contains user ID, name, and email
        // (Checking the first user in the list as a sample)
        Assert.assertNotNull(response.jsonPath().get("[0].id"));
        Assert.assertNotNull(response.jsonPath().get("[0].name"));
        Assert.assertNotNull(response.jsonPath().get("[0].email"));

        //  Validate how many user which status = active
        List<String> statuses = response.jsonPath().getList("status");
        initialActiveCount = (int) statuses.stream().filter(s -> s.equals("active")).count();
        System.out.println("Step 1: Initial Active User Count = " + initialActiveCount);
    }

    /**
     * 2. POST Request
     * Create a new user and verify integrity
     */
    @Test(priority = 2)
    public void testPostRequest() {
        // o Use the sample payload
        postPayload = new HashMap<>();
        postPayload.put("name", "John Doe");
        postPayload.put("email", "johndoe" + System.currentTimeMillis() + "@example.com");
        postPayload.put("gender", "male");
        postPayload.put("status", "inactive");

        Response postResponse = given()
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .contentType(ContentType.JSON)
                .body(postPayload)
                .post("/users");

        //  Verify the status code is 201
        Assert.assertEquals(postResponse.getStatusCode(), 201);
        userId = postResponse.jsonPath().getInt("id");

        // o Validate response body matches payload (status must be inactive)
        Assert.assertEquals(postResponse.jsonPath().getString("name"), postPayload.get("name"));
        Assert.assertEquals(postResponse.jsonPath().getString("email"), postPayload.get("email"));
        Assert.assertEquals(postResponse.jsonPath().getString("status"), "inactive");

        // o Validate new user is valid by using GET response compare with POST response body
        Response getResponse = given()
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .get("/users/" + userId);
        
        Assert.assertEquals(getResponse.jsonPath().getString("name"), postResponse.jsonPath().getString("name"));
        Assert.assertEquals(getResponse.jsonPath().getString("email"), postResponse.jsonPath().getString("email"));
    }

    /**
     * 3. PUT Request
     * Update existing user and validate active status
     */
    @Test(priority = 3, dependsOnMethods = "testPostRequest")
    public void testPutRequest() {
        //  Update the user's name and email
        Map<String, String> updatePayload = new HashMap<>();
        updatePayload.put("name", "John Updated");
        updatePayload.put("email", "updated" + userId + "@example.com");
        updatePayload.put("status", "active");

        Response putResponse = given()
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .put("/users/" + userId);

        //  Verify the status code is 200
        Assert.assertEquals(putResponse.getStatusCode(), 200);

        //  Validate the updated fields (status must be active)
        Assert.assertEquals(putResponse.jsonPath().getString("name"), "John Updated");
        Assert.assertEquals(putResponse.jsonPath().getString("status"), "active");

        //  Validate GET /users?status=active to check if new user matches payload
        Response activeResponse = given()
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .queryParam("status", "active")
                .get("/users");

        List<Integer> activeUserIds = activeResponse.jsonPath().getList("id");
        Assert.assertTrue(activeUserIds.contains(userId), "Updated user should be in active list");

        //  Compare number of user from Task 1
        int currentActiveCount = activeUserIds.size();
        System.out.println("Step 3: New Active User Count = " + currentActiveCount);
        // Note: Count should increase or stay same depending on pagination/total users
    }

    /**
     * 4. DELETE Request
     * Delete user and verify it is gone
     */
    @Test(priority = 4, dependsOnMethods = "testPutRequest")
    public void testDeleteRequest() {
        // o Verify the status code is 204
        given()
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .delete("/users/" + userId)
                .then()
                .statusCode(204);

        // o Attempt to fetch the user again and verify the status code is 404
        given()
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .get("/users/" + userId)
                .then()
                .statusCode(404);
    }
}
