package com.wcucampus.campuscompass.model.api;

import com.wcucampus.campuscompass.model.entity.Token;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * An interface to be used in the construction of API endpoints for use with {@link retrofit2.Retrofit}.
 */
public interface Service {
  @GET("{endpoint}")
  Call<List<Token>> get(@Path("endpoint") String endpoint);


}
