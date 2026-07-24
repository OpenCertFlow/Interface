package com.certimakers.report.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "report_phrase")
public class ReportPhraseEntity {

    @Id
    @Column(name = "phrase_key")
    private String phraseKey;

    @Column(nullable = false)
    private String text;

    @Column
    private String description;

    protected ReportPhraseEntity() {
    }

    public ReportPhraseEntity(String phraseKey, String text, String description) {
        this.phraseKey = phraseKey;
        this.text = text;
        this.description = description;
    }

    public void update(String text, String description) {
        this.text = text;
        this.description = description;
    }

    public String getPhraseKey() {
        return phraseKey;
    }

    public String getText() {
        return text;
    }

    public String getDescription() {
        return description;
    }
}
