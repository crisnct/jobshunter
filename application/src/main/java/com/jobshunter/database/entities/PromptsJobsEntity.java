package com.jobshunter.database.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "prompts_jobs")
@IdClass(PromptsJobsEntity.PromptsJobsId.class)
public class PromptsJobsEntity {

    @Id
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_id", nullable = false)
    private UserPromptEntity prompt;

    @Id
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_jobs_id", nullable = false)
    private UserJobEntity userJob;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromptsJobsId implements Serializable {
        private Long prompt;
        private Long userJob;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PromptsJobsId that = (PromptsJobsId) o;
            return Objects.equals(prompt, that.prompt) && Objects.equals(userJob, that.userJob);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prompt, userJob);
        }
    }
}
