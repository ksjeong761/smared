package kr.ac.kpu.block.smared;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;
import java.util.function.Consumer;

public class DAO {
    private FormattedLogger logger = new FormattedLogger();

    private Consumer<DataSnapshot> successCallback;
    private Consumer<DatabaseError> failureCallback;

    public void setSuccessCallback(Consumer<DataSnapshot> successCallback) {
        this.successCallback = successCallback;
    }

    public void setFailureCallback(Consumer<DatabaseError> failureCallback) {
        this.failureCallback = failureCallback;
    }

    // 경로가 입력되지 않은 경우 기본 경로 DB에 데이터를 저장한다.
    public void create(DTO dto, Class clazz) {
        String databasePath = clazz.getSimpleName() + "/" + dto.getUid();
        create(dto, clazz, databasePath);
    }
    // DB에 데이터를 저장한다.
    public void create(DTO dto, Class clazz, String databasePath) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(databasePath);

        Map<String, Object> map = dto.toMap();
        databaseReference.setValue(map).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                logger.writeLog("Success");
                if (successCallback != null) {
                    successCallback.accept(null);
                }
            } else {
                logger.writeLog("Failure");
                if (failureCallback != null) {
                    failureCallback.accept(null);
                }
            }
        });
    }

    // 경로가 입력되지 않은 경우 기본 경로 DB에서 데이터를 읽어온다.
    public void readAll(DTO dto, Class clazz) {
        String databasePath = clazz.getSimpleName() + "/" + dto.getUid();
        readAll(dto, clazz, databasePath);
    }
    // DB에서 데이터를 읽어온다.
    public void readAll(DTO dto, Class clazz, String databasePath) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(databasePath);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot userSnapshot) {
                logger.writeLog("Success");
                if (successCallback != null) {
                    successCallback.accept(userSnapshot);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                logger.writeLog("Failed to read value : " + error.toException().getMessage());
                if (failureCallback != null) {
                    failureCallback.accept(error);
                }
            }
        });
    }

    public void update(DTO dto, Class clazz) {
        String databasePath = clazz.getSimpleName() + "/" + dto.getUid();
        update(dto, clazz, databasePath);
    }
    public void update(DTO dto, Class clazz, String databasePath) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(databasePath);

        Map<String, Object> map = dto.toMap();
        databaseReference.updateChildren(map).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                logger.writeLog("Success");
                if (successCallback != null) {
                    successCallback.accept(null);
                }
            } else {
                logger.writeLog("Failure");
                if (failureCallback != null) {
                    failureCallback.accept(null);
                }
            }
        });
    }

    public void delete(DTO dto, Class clazz) {
        String databasePath = clazz.getSimpleName() + "/" + dto.getUid();
        delete(dto, clazz, databasePath);
    }
    public void delete(DTO dto, Class clazz, String databasePath) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(databasePath);

        databaseReference.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                logger.writeLog("Success");
                if (successCallback != null) {
                    successCallback.accept(null);
                }
            } else {
                logger.writeLog("Failure");
                if (failureCallback != null) {
                    failureCallback.accept(null);
                }
            }
        });
    }
}
