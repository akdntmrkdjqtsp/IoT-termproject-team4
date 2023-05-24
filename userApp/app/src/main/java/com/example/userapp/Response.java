package com.example.userapp;

import java.util.List;

import retrofit2.http.GET;

public class Response {
    List<Next> list;
}


class Next {
    int cardinal_direction;
    int distance;
}
