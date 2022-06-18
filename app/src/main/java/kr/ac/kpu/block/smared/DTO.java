package kr.ac.kpu.block.smared;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public abstract class DTO {
    private String uid = "";

    public String getUid() {
        return uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }

    /*
        Firebase에 데이터를 삽입하기 위해 Map 형태로 데이터 형식을 맞출 때
        필드명 문자열을 하드코딩 해야 하는 문제를 해결하기 위해 만든 함수이다.

        이 함수를 사용한다면 새로운 클래스 또는 필드를 추가하게 되더라도 필드명을 직접 입력할 필요가 없어진다.
        단, 필드명이 바뀌었을 경우 데이터베이스에는 이전 필드명이 그대로 남아있기 때문에 변환 작업을 거쳐줘야 한다.
    */
    public Map<String, Object> toMap() {
        Map<String, Object> resultMap = new HashMap<>();

        for (Field field : this.getClass().getDeclaredFields()) {
            field.setAccessible(true);

            // [Refactor] uid 필드명이 하드코딩됨
            if (field.getName().equals("uid")) {
                continue;
            }

            try {
                resultMap.put(field.getName(), field.get(this).toString());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return resultMap;
    }

}