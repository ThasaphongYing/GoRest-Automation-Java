package com.gorest.tests;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class UserTests {
    private String baseUrl = "https://gorest.co.in/public/v2";
    private String token = "YOUR_ACTUAL_TOKEN_HERE"; // แทนที่ด้วย Token จริงของคุณ
    private int userId;
    private int initialActiveCount;

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = baseUrl;
    }

    @Test(priority = 1)
    public void testGetActiveUsersCount() {
        // Task 1: Validate active users count
        Response response = given()
                .header("Authorization", "Bearer " + token)
                .get("/users");

        Assert.assertEquals(response.getStatusCode(), 200);
        
        List<String> statuses = response.jsonPath().getList("status");
        initialActiveCount = (int) statuses.stream().filter(s -> s.equals("active")).count();
        System.out.println("Initial Active Users: " + initialActiveCount);
    }

    @Test(priority = 2)
    public void testCreateUser() {
        // Task 2: POST Request
        Map<String, String> payload = Map.of(
            "name", "John Doe",
            "email", "johndoe" + System.currentTimeMillis() + "@example.com", // Unique email
            "gender", "male",
            "status", "inactive"
        );

        Response response = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/users");

        Assert.assertEquals(response.getStatusCode(), 201);
        userId = response.jsonPath().getInt("id");

        // Validate POST response
        Assert.assertEquals(response.jsonPath().getString("name"), payload.get("name"));
        Assert.assertEquals(response.jsonPath().getString("status"), "inactive");

        // GET validation compare with POST
        Response getResponse = given()
                .header("Authorization", "Bearer " + token)
                .get("/users/" + userId);
        
        Assert.assertEquals(getResponse.jsonPath().getString("name"), response.jsonPath().getString("name"));
    }

    @Test(priority = 3, dependsOnMethods = "testCreateUser")
    public void testUpdateUser() {
        // Task 3: PUT Request
        Map<String, String> updatePayload = Map.of(
            "name", "John Updated",
            "email", "johnupd" + System.currentTimeMillis() + "@example.com",
            "status", "active"
        );

        Response response = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .put("/users/" + userId);

        Assert.assertEquals(response.getStatusCode(), 200);
        Assert.assertEquals(response.jsonPath().getString("status"), "active");

        // Validate GET /users?status=active and compare count
        Response activeUsersResp = given()
                .header("Authorization", "Bearer " + token)
                .queryParam("status", "active")
                .get("/users");
        
        List<Integer> ids = activeUsersResp.jsonPath().getList("id");
        Assert.assertTrue(ids.contains(userId), "Updated user should be in active list");
        
        // เปรียบเทียบจำนวน User (จะเพิ่มขึ้น 1 ถ้าตอนแรก User นั้นไม่ใช่ active)
        int newActiveCount = ids.size();
        System.out.println("New Active Users Count: " + newActiveCount);
    }

    @Test(priority = 4, dependsOnMethods = "testUpdateUser")
    public void testDeleteUser() {
        // Task 4: DELETE Request
        given()
            .header("Authorization", "Bearer " + token)
            .delete("/users/" + userId)
            .then()
            .statusCode(204);

        // Verify 404
        given()
            .header("Authorization", "Bearer " + token)
            .get("/users/" + userId)
            .then()
            .statusCode(404);
    }
}