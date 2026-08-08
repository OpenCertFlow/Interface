package io.opencertflow.auth.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "terms")
public class TermsEntity {

    @Id
    private Long id;

    @Column(name = "term_key", nullable = false)
    private String termKey;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private boolean required;

    @Column(nullable = false)
    private boolean active;

    protected TermsEntity() {
    }

    public String getTermKey() {
        return termKey;
    }

    public String getVersion() {
        return version;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isRequired() {
        return required;
    }
}
