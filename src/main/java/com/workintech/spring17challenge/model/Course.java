package com.workintech.spring17challenge.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Course {
    private Integer id; // id ekledim, CRUD için gerekli
    private String name;
    private Integer credit;
    private Grade grade;
}
