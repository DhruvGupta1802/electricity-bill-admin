package com.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bill {
    private String month;
    private Double amount;
    private Integer units;
    private Date dueDate;
    private String notes;
    private Date createdAt;
    private String addedBy;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("month", month);
        map.put("amount", amount);
        map.put("units", units);
        map.put("dueDate", dueDate);
        map.put("notes", notes);
        map.put("createdAt", createdAt != null ? createdAt : new Date());
        map.put("addedBy", addedBy != null ? addedBy : "Admin");
        return map;
    }
}
