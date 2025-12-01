package com.backend.gamelibrarybackend.service;

import com.backend.gamelibrarybackend.dto.GameItemDTO;
import com.backend.gamelibrarybackend.models.GameItemEntity;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class GameItemService {

    private CollectionReference userGamesCollection(String userId) {
        Firestore firestore = FirestoreClient.getFirestore();
        return firestore.collection("users").document(userId).collection("games");
    }

    public List<GameItemEntity> addGame(String userId, GameItemDTO dto) {
        try {
            CollectionReference games = userGamesCollection(userId);
            DocumentReference docRef = games.document();
            GameItemEntity entity = mapFromDto(dto, userId);
            entity.setId(docRef.getId());
            docRef.set(entity).get();
            return findAll(userId);
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to add game", e);
        }
    }

    public long count(String userId) {
        try {
            ApiFuture<QuerySnapshot> future = userGamesCollection(userId).get();
            return future.get().size();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to count games", e);
        }
    }

    public List<GameItemEntity> findByYear(String userId, int year) {
        return query(userGamesCollection(userId).whereEqualTo("completedYear", year));
    }

    public List<GameItemEntity> findFavourites(String userId) {
        return query(userGamesCollection(userId).whereEqualTo("isFavourite", true));
    }

    public List<GameItemEntity> findHundredPercent(String userId) {
        return query(userGamesCollection(userId).whereEqualTo("isHundredPercent", true));
    }

    public List<GameItemEntity> findAll(String userId) {
        return query(userGamesCollection(userId));
    }

    private List<GameItemEntity> query(com.google.cloud.firestore.Query query) {
        try {
            List<DocumentSnapshot> documents = query.get().get().getDocuments();
            return documents.stream()
                    .map(doc -> {
                        GameItemEntity entity = doc.toObject(GameItemEntity.class);
                        if (entity != null) {
                            entity.setId(doc.getId());
                        }
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to fetch games", e);
        }
    }

    private GameItemEntity mapFromDto(GameItemDTO dto, String userId) {
        GameItemEntity entity = new GameItemEntity(
                dto.getName(),
                dto.getYear(),
                dto.getCompletedYear(),
                dto.isCompleted(),
                dto.isHundredPercent(),
                dto.isFavourite(),
                dto.getSpecialDescription(),
                dto.getImageUrl(),
                userId
        );
        return entity;
    }
}
