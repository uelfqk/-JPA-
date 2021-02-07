package com.studyolle.study.form;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

@Getter @Setter
public class StudyPathForm {

    @NotBlank
    @Length(max = 50)
    private String newPath;
}
