package com.example.demo.Service.TouristSpot;

import com.example.demo.Model.TouristSpot;
import com.example.demo.Model.TouristSpotFeedback;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface TouristSpotService {
    List<TouristSpot> getAllSpots();
    ResponseEntity<List<TouristSpotFeedback.Feedback>> getAllFeedbackBySpotId(Integer spotId);
    ResponseEntity<String> submitFeedback(Integer spotId, TouristSpotFeedback.Feedback feedback);
    TouristSpot getSpotById(Integer spotId);
    ResponseEntity<?> addSpot(TouristSpot newSpot, MultipartFile spotPicture) throws IOException;
    ResponseEntity<TouristSpot> getSpotBySpotName(String spotName);
    String addAllSpots(List<TouristSpot> spots);
    ResponseEntity<String> removeSpotById(Integer spotId);
    List<TouristSpot> getAllPopularTouristSpot();

}
