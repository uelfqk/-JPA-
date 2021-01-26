package com.studyolle.study.form;

import com.studyolle.domain.Study;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Getter @Setter
public class StudyForm {

    //TODO 스터디 url path
    @NotBlank
    @Length(max = 50)
    private String path;

    //TODO 스터디 제목
    @NotBlank
    @Length(max = 50)
    private String title;

    //TODO 스터디 짧은 소개
    @NotBlank
    @Length(max = 100)
    private String shortDescription;

    //TODO 스터디 긴 소개
    private String fullDescription;

    public Study getStudy() {
        Study study = new Study();
        study.setPath(path);
        study.setTitle(title);
        study.setShortDescription(shortDescription);
        study.setFullDescription(fullDescription);
        return study;
    }
}
