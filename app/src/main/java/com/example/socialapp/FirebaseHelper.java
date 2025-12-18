package com.example.socialapp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseHelper {
    private static FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static FirebaseAuth auth = FirebaseAuth.getInstance();

    public interface UserCallback {
        void onSuccess(User user);
        void onError(String error);
    }

    public static void getCurrentUser(UserCallback callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onError("No authenticated user");
            return;
        }

        db.collection("users").document(firebaseUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            User user = document.toObject(User.class);
                            callback.onSuccess(user);
                        } else {
                            // Create new user document
                            User newUser = new User();
                            newUser.setUserId(firebaseUser.getUid());
                            newUser.setPhoneNumber(firebaseUser.getPhoneNumber());
                            newUser.setUsername("User_" + firebaseUser.getUid().substring(0, 6));

                            db.collection("users").document(firebaseUser.getUid())
                                    .set(newUser)
                                    .addOnSuccessListener(aVoid -> callback.onSuccess(newUser))
                                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
                        }
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    public static String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }
}